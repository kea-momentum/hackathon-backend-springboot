package com.momentum.releaser.domain.project.application;

import static com.momentum.releaser.domain.project.dto.ProjectDataDto.*;
import static com.momentum.releaser.global.common.Base64.getImageUrlFromBase64;
import static com.momentum.releaser.global.common.CommonEnum.DEFAULT_PROJECT_IMG;
import static com.momentum.releaser.global.config.BaseResponseStatus.*;
import static org.springframework.util.StringUtils.hasText;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.momentum.releaser.domain.issue.domain.Issue;
import com.momentum.releaser.domain.issue.domain.QIssue;
import com.momentum.releaser.domain.project.dto.ProjectRequestDto.FilterIssueRequestDTO;
import com.momentum.releaser.domain.project.dto.ProjectRequestDto.FilterReleaseRequestDTO;
import com.momentum.releaser.domain.project.dto.ProjectResponseDto.ProjectSearchResponseDTO;
import com.momentum.releaser.domain.release.dao.release.ReleaseRepository;
import com.momentum.releaser.domain.release.domain.QReleaseNote;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.redis.RedisUtil;
import com.momentum.releaser.redis.notification.NotificationRedisRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;

import com.momentum.releaser.domain.issue.dao.IssueRepository;
import com.momentum.releaser.domain.issue.mapper.IssueMapper;
import com.momentum.releaser.domain.project.dao.ProjectMemberRepository;
import com.momentum.releaser.domain.project.dao.ProjectRepository;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.project.dto.ProjectRequestDto.ProjectInfoRequestDTO;
import com.momentum.releaser.domain.project.dto.ProjectResponseDto.GetProjectResponseDTO;
import com.momentum.releaser.domain.project.dto.ProjectResponseDto.ProjectInfoResponseDTO;
import com.momentum.releaser.domain.project.mapper.ProjectMapper;
import com.momentum.releaser.domain.release.dao.approval.ReleaseApprovalRepository;
import com.momentum.releaser.domain.release.mapper.ReleaseMapper;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.global.config.aws.S3Upload;
import com.momentum.releaser.global.exception.CustomException;

/**
 * 프로젝트와 관련된 기능을 제공하는 서비스 구현 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;
    private final ReleaseRepository releaseRepository;
    private final ReleaseApprovalRepository releaseApprovalRepository;
    private final ModelMapper modelMapper;
    private final S3Upload s3Upload;

    private final RedisUtil redisUtil;
    private final NotificationRedisRepository notificationRedisRepository;

    private final AmqpAdmin rabbitAdmin;
    private final DirectExchange projectDirectExchange;
    private final ConnectionFactory connectionFactory;

    /**
     * 3.1 프로젝트 생성
     *
     * @param email 사용자 이메일
     * @author chaeanna, seonwoo
     * @date 2023-07-04
     */
    @Override
    @Transactional
    public ProjectInfoResponseDTO addProject(String email, ProjectInfoRequestDTO projectInfoReq) throws IOException {
        //Token UserInfo
        User user = getUserByEmail(email);
        // S3 URL 생성한다.
        String url = uploadProjectImg(projectInfoReq);
        // 프로젝트 생성
        Project newProject = createNewProject(projectInfoReq, url);
        // 프로젝트 멤버 추가
        addProjectMember(newProject, user);
        // 생성된 프로젝트에 해당하는 큐를 생성하고, 연결한다.
        createAndBindQueueAndRegisterListener(newProject.getProjectId());
        // 프로젝트 응답 객체 생성
        return ProjectMapper.INSTANCE.toProjectInfoRes(newProject);
    }

    /**
     * 3.2 프로젝트 수정
     *
     * @param email 사용자 이메일
     * @author chaeanna, seonwoo
     * @date 2023-07-04
     */
    @Override
    @Transactional
    public ProjectInfoResponseDTO modifyProject(Long projectId, String email, ProjectInfoRequestDTO projectInfoReq) throws IOException {
        Project project = getProjectById(projectId);
        User user = getUserByEmail(email);
        ProjectMember leader = findLeaderForProject(project);

        // 접근 유저가 프로젝트 생성자인지 확인
        if (leader == null) {
            throw new CustomException(NOT_PROJECT_PM);
        }

        String url = updateProjectImg(project, projectInfoReq);
        Project updatedProject = getAndUpdateProject(project, projectInfoReq, url);
        return ProjectMapper.INSTANCE.toProjectInfoRes(updatedProject);
    }

    /**
     * 3.3 프로젝트 삭제
     *
     * @author chaeanna
     * @date 2023-07-05
     */
    @Override
    @Transactional
    public String removeProject(Long projectId) {
        //project 정보
        Project project = getProjectById(projectId);

        projectRepository.deleteById(project.getProjectId());
        issueRepository.deleteByIssueNum();
        releaseApprovalRepository.deleteByReleaseApproval();

        return "프로젝트가 삭제되었습니다.";
    }

    /**
     * 3.4 프로젝트 조회
     *
     * @param email 사용자 이메일
     * @author chaeanna
     * @date 2023-07-04
     */
    @Override
    @Transactional
    public GetProjectResponseDTO findProjects(String email) {
        // 사용자 정보
        User user = getUserByEmail(email);

        // 프로젝트 멤버 정보
        List<ProjectMember> projectMemberList = projectMemberRepository.findByUser(user);
        List<GetProjectDataDTO> getCreateProjectList = new ArrayList<>();
        List<GetProjectDataDTO> getEnterProjectList = new ArrayList<>();

        for (ProjectMember projectMember : projectMemberList) {
            // 생성한 프로젝트 조회
            if (projectMember.getPosition() == 'L') {
                getCreateProjectList.add(mapToGetProject(projectMember.getProject()));
            } else { // 참가한 프로젝트 조회
                getEnterProjectList.add(mapToGetProject(projectMember.getProject()));
            }
        }

        return GetProjectResponseDTO.builder()
                .getCreateProjectList(getCreateProjectList)
                .getEnterProjectList(getEnterProjectList)
                .build();
    }

    /**
     * 10.2 프로젝트 내 통합검색
     *
     * @author chaeanna
     * @date 2023-08-06
     */
    @Override
    @Transactional
    public ProjectSearchResponseDTO findProjectSearch(Long projectId, String filterType, FilterIssueRequestDTO filterIssueGroup, FilterReleaseRequestDTO filterReleaseGroup) {
        // projectId로 해당 프로젝트의 팀원 정보 조회
        ProjectMember member = projectRepository.getProjectMemberPostionPM(projectId);

        List<GetIssueInfoDataDTO> issueResponses = null;
        List<GetReleaseInfoDataDTO> releaseResponses = null;

        // 검색 대상이 issue인 경우 이슈 정보 검색
        if ("issue".equals(filterType)) {
            issueResponses = findIssueResponses(filterIssueGroup, member.getProject());
        } else if ("release".equals(filterType)) {
            // 검색 대상이 release인 경우 릴리즈 정보 검색
            releaseResponses = findReleaseResponses(filterReleaseGroup, member);
        } else {
            throw new CustomException(INVALID_FILTER_TYPE);
        }

        // 검색 결과가 null인 경우 빈 리스트로 초기화
        issueResponses = Optional.ofNullable(issueResponses).orElse(Collections.emptyList());
        releaseResponses = Optional.ofNullable(releaseResponses).orElse(Collections.emptyList());

        // 이슈와 릴리즈 정보를 담고 있는 ProjectSearchResponseDTO 생성하여 반환
        return ProjectSearchResponseDTO.builder()
                .getIssueInfoList(issueResponses)
                .getReleaseInfoList(releaseResponses)
                .build();
    }

    // =================================================================================================================

    /**
     * 이메일로 User 가져오기
     *
     * @param email 사용자 이메일
     * @return User 조회된 사용자 엔티티
     * @throws CustomException 사용자가 존재하지 않을 경우 발생하는 예외
     * @author chaeanna
     * @date 2023-07-04
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new CustomException(NOT_EXISTS_USER));
    }

    /**
     * 프로젝트 식별 번호를 이용하여 프로젝트 엔티티 가져오기
     *
     * @param projectId 프로젝트 식별 번호
     * @return Project 조회된 프로젝트 엔티티
     * @throws CustomException 프로젝트가 존재하지 않을 경우 발생하는 예외
     * @author seonwoo
     * @date 2023-07-04
     */
    private Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> new CustomException(NOT_EXISTS_PROJECT));
    }

    /**
     * 클라이언트로부터 받은 프로젝트 이미지를 S3에 업로드한다.
     *
     * @param projectInfoReq 프로젝트 생성 또는 수정 요청 객체
     * @return String 업로드된 이미지의 S3 URL
     * @throws IOException 이미지 업로드 중 오류가 발생한 경우 발생하는 예외
     * @author seonwoo
     * @date 2023-07-04
     */
    String uploadProjectImg(ProjectInfoRequestDTO projectInfoReq) throws IOException {
        if (projectInfoReq == null) {
            throw new IllegalArgumentException("projectInfoReq cannot be null");
        }

        if (projectInfoReq.getImg().isEmpty()) {
            // 만약 사용자로부터 받은 이미지 데이터가 없는 경우 기본 프로필로 대체한다.
            return DEFAULT_PROJECT_IMG.url();
        }

        String img = projectInfoReq.getImg();

        log.info("Project images before base64 decoding: {}", img);

        // Base64로 인코딩된 이미지 파일을 파일 형태로 가져온다.
        File file = getImageUrlFromBase64(img);

        String url = s3Upload.upload(file, file.getName(), "projects");

        log.info("AWS S3 url: {}", url);

        if (file.delete()) {
            return url;
        } else {
            throw new CustomException(FAILED_TO_CREATE_PROJECT);
        }
    }

    /**
     * 클라이언트로부터 받은 프로젝트 이미지로 수정한다.
     *
     * @param project        프로젝트 엔티티
     * @param projectInfoReq 프로젝트 수정 요청 객체
     * @return String 수정된 프로젝트 이미지의 S3 URL
     * @throws IOException 이미지 업로드 중 오류가 발생한 경우 발생하는 예외
     * @author seonwoo
     * @date 2023-07-04
     */
    private String updateProjectImg(Project project, ProjectInfoRequestDTO projectInfoReq) throws IOException {
        deleteIfExistsProjectImg(project);
        return uploadProjectImg(projectInfoReq);
    }

    /**
     * 해당 프로젝트의 관리자 찾기
     *
     * @param project 프로젝트 엔티티
     * @return ProjectMember 관리자(L) 포지션을 가진 프로젝트 멤버 엔티티
     * @author chaeanna
     * @date 2023-07-04
     */
    private ProjectMember findLeaderForProject(Project project) {
        List<ProjectMember> members = projectMemberRepository.findByProject(project);
        for (ProjectMember member : members) {

            if (member.getPosition() == 'L') {
                return member;
            }

        }
        return null;
    }

    /**
     * 프로젝트 이미지 값이 null이 아닌 경우 한 번 지운다.
     *
     * @param project 프로젝트 엔티티
     * @author seonwoo
     * @date 2023-07-04
     */
    private void deleteIfExistsProjectImg(Project project) {
        Project updatedProject = project;

        // 만약 "" 값이 들어가 있는 경우 null로 바꾼다.
        if (project.getImg().isEmpty() || project.getImg().isBlank()) {
            project.updateImg(null);
            updatedProject = projectRepository.save(project);
        }

        // 만약 프로젝트 이미지가 기본 이미지가 아닌 다른 파일이 들어가 있는 경우 파일을 삭제한다.
        if (!Objects.equals(updatedProject.getImg(), DEFAULT_PROJECT_IMG.url()) && updatedProject.getImg() != null) {
            String img = updatedProject.getImg();
            if (img.length() > 55) {
                s3Upload.delete(updatedProject.getImg().substring(55));
            }
        }
    }

    /**
     * 프로필 이미지를 제외한 프로젝트 데이터를 업데이트한다.
     *
     * @param project        프로젝트 엔티티
     * @param projectInfoReq 프로젝트 수정 요청 객체
     * @param url            업로드된 프로젝트 이미지의 S3 URL
     * @return Project 업데이트된 프로젝트 엔티티
     * @author seonwoo
     * @date 2023-07-04
     */
    private Project getAndUpdateProject(Project project, ProjectInfoRequestDTO projectInfoReq, String url) {
        project.updateProject(projectInfoReq, url);
        return projectRepository.save(project);
    }

    /**
     * 프로젝트 생성
     *
     * @param registerReq 프로젝트 생성 요청 객체
     * @param url         업로드된 프로젝트 이미지의 S3 URL
     * @return Project 생성된 프로젝트 엔티티
     * @author seonwoo, chaeanna
     * @date 2023-07-04
     */
    Project createNewProject(ProjectInfoRequestDTO registerReq, String url) {
        //초대 링크 생성
        String inviteLink = generateInviteLink();

        return projectRepository.save(Project.builder()
                .title(registerReq.getTitle())
                .content(registerReq.getContent())
                .link(inviteLink)
                .team(registerReq.getTeam())
                .img(url)
                .status('Y')
                .build());
    }

    /**
     * 초대 링크 생성
     *
     * @return String 생성된 초대 링크
     * @author chaeanna
     * @date 2023-07-04
     */
    private String generateInviteLink() {
        // UUID를 이용하여 무작위의 초대 링크를 생성
        return UUID.randomUUID().toString();
    }

    /**
     * 프로젝트 멤버 추가
     *
     * @param project 프로젝트 엔티티
     * @param user    사용자 엔티티
     * @author seonwoo
     * @date 2023-07-04
     */
    void addProjectMember(Project project, User user) {
        ProjectMember projectMember = ProjectMember.builder()
                .position('L')
                .user(user)
                .project(project)
                .status('Y')
                .build();

        projectMemberRepository.save(projectMember);
    }

    /**
     * project mapper 사용
     *
     * @param project 프로젝트 엔티티
     * @return GetProjectDateDTO 변환된 프로젝트 DTO
     * @author chaeanna
     * @date 2023-07-04
     */
    private GetProjectDataDTO mapToGetProject(Project project) {
        return modelMapper.map(project, GetProjectDataDTO.class);
    }

    /**
     * <<<<<<< HEAD
     * 이슈 정보 조회
     *
     * @param filterIssueGroup 이슈 필터링 그룹
     * @param project          검색할 프로젝트
     * @return GetIssueInfoDataDTO 검색된 이슈 정보 리스트
     * @author chaeanna
     * @date 2023-08-06
     */
    private List<GetIssueInfoDataDTO> findIssueResponses(FilterIssueRequestDTO filterIssueGroup, Project project) {
        // issue에 대한 검색 조건 빌드
        Predicate predicateIssue = buildPredicateFromIssueFilters(filterIssueGroup, project);

        // 검색 조건을 이용하여 issue 조회
        Iterable<Issue> resultIssue = issueRepository.findAll(predicateIssue);

        // 조회된 issue를 GetIssueInfoDataDTO로 변환하여 리스트 반환
        return StreamSupport.stream(resultIssue.spliterator(), false)
                .map(this::toGetIssueInfoDataDTO)
                .collect(Collectors.toList());
    }

    /**
     * 릴리즈 정보 조회
     *
     * @param filterReleaseGroup 릴리즈 필터링 그룹
     * @param member             프로젝트 PM 정보
     * @return GetReleaseInfoDataDTO 검색된 릴리즈 정보 리스트
     * @author chaeanna
     * @date 2023-08-06
     */
    private List<GetReleaseInfoDataDTO> findReleaseResponses(FilterReleaseRequestDTO filterReleaseGroup, ProjectMember member) {
        // 릴리즈에 대한 검색 조건 빌드
        Predicate predicateRelease = buildPredicateFromReleaseFilters(filterReleaseGroup, member.getProject());

        // 검색 조건을 이용하여 릴리즈 정보를 조회합니다.
        Iterable<ReleaseNote> resultRelease = releaseRepository.findAll(predicateRelease);

        // 조회된 릴리즈 정보를 GetReleaseInfoDataDTO로 변환하여 리스트로 반환합니다.
        return StreamSupport.stream(resultRelease.spliterator(), false)
                .map(release -> toGetReleaseInfoDataDTO(release, member))
                .collect(Collectors.toList());
    }

    /**
     * 릴리즈 정보를 GetReleaseInfoDataDTO 형식 변환
     *
     * @param releaseNote 릴리즈 정보
     * @param member      프로젝트 PM 정보
     * @return GetReleaseInfoDataDTO 변환된 릴리즈 정보
     * @author chaeanna
     * @date 2023-08-06
     */
    private GetReleaseInfoDataDTO toGetReleaseInfoDataDTO(ReleaseNote releaseNote, ProjectMember member) {
        return ReleaseMapper.INSTANCE.toGetReleaseInfoDataDTO(releaseNote, member);
    }

    /**
     * 이슈 정보를 GetIssueInfoDataDTO 형식 변환
     *
     * @param issue 이슈 정보
     * @return GetIssueInfoDataDTO 변환된 이슈 정보
     * @author chaeanna
     * @date 2023-08-06
     */
    private GetIssueInfoDataDTO toGetIssueInfoDataDTO(Issue issue) {
        return IssueMapper.INSTANCE.toGetIssueInfoDataDTO(issue);
    }

    /**
     * 이슈 필터링 조건을 기반으로 Predicate 생성
     *
     * @param filterIssueGroup 이슈 필터링 조건 그룹
     * @param project          프로젝트 정보
     * @return Predicate 생성된 Predicate
     * @author chaeanna
     * @date 2023-08-06
     */
    private Predicate buildPredicateFromIssueFilters(FilterIssueRequestDTO filterIssueGroup, Project project) {
        BooleanBuilder builder = new BooleanBuilder();
        QIssue issue = QIssue.issue;

        // 프로젝트에 해당하는 이슈만 검색하도록 조건 추가
        builder.and(issue.project.eq(project));

        Date startDate = filterIssueGroup.getStartDate();
        Date endDate = filterIssueGroup.getEndDate();
        Long manager = filterIssueGroup.getManagerId();
        String startVersion = filterIssueGroup.getStartReleaseVersion();
        String endVersion = filterIssueGroup.getEndReleaseVersion();
        String tag = filterIssueGroup.getTag();
        String title = filterIssueGroup.getIssueTitle();

        // 이슈의 종료일 범위 검색 조건 추가
        if (startDate != null && endDate != null) {
            // endDate를 하루 뒤로 이동하여 포함되게 조회
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endDate);
            calendar.add(Calendar.DATE, 1);
            Date adjustedEndDate = calendar.getTime();

            builder.and(issue.endDate.between(startDate, adjustedEndDate));
        }
        // 이슈의 담당자 검색 조건 추가
        if (manager != null) {
            builder.and(issue.member.memberId.eq(manager));
        }
        // 이슈의 릴리즈 버전 범위 검색 조건 추가
        if (hasText(startVersion) && hasText(endVersion)) {
            builder.and(issue.release.version.goe(startVersion))
                    .and(issue.release.version.loe(endVersion));
        }
        // 이슈의 태그를 이용한 FULLTEXT 검색 조건 추가
        if (hasText(tag)) {
            NumberTemplate booleanTemplate = Expressions.numberTemplate(Double.class,
                    "function('match',{0},{1})", issue.tag, "+" + tag + "*");
            List<Issue> result = issueRepository.getSearch(booleanTemplate, project);

            if (result != null) {
                builder.and(issue.in(result));
            }

        }
        // 이슈의 제목을 이용한 FULLTEXT 검색 조건 추가
        if (hasText(title)) {
            NumberTemplate booleanTemplate = Expressions.numberTemplate(Double.class,
                    "function('match',{0},{1})", issue.title, "+" + title + "*");
            List<Issue> result = issueRepository.getSearch(booleanTemplate, project);

            if (result != null) {
                builder.and(issue.in(result));
            }

        }

        return builder.getValue();
    }

    /**
     * 릴리즈 필터링 조건을 기반으로 Predicate 생성
     *
     * @param filterReleaseGroup 릴리즈 필터링 조건 그룹
     * @param project            프로젝트 정보
     * @return Predicate 생성된 Predicate
     * @author chaeanna
     * @date 2023-08-06
     */
    private Predicate buildPredicateFromReleaseFilters(FilterReleaseRequestDTO filterReleaseGroup, Project project) {
        BooleanBuilder builder = new BooleanBuilder();
        QReleaseNote release = QReleaseNote.releaseNote;

        // 프로젝트에 해당하는 릴리즈만 검색하도록 조건 추가
        builder.and(release.project.eq(project));

        String startVersion = filterReleaseGroup.getStartVersion();
        String endVersion = filterReleaseGroup.getEndVersion();
        String title = filterReleaseGroup.getReleaseTitle();

        // 릴리즈의 버전 범위 검색 조건 추가
        if (hasText(startVersion) && hasText(endVersion)) {
            builder.and(release.version.goe(startVersion))
                    .and(release.version.loe(endVersion));
        }
        // 릴리즈의 제목을 이용한 FULLTEXT 검색 조건 추가
        if (hasText(title)) {
            NumberTemplate booleanTemplate = Expressions.numberTemplate(Double.class,
                    "function('match',{0},{1})", release.title, "+" + title + "*");
            List<ReleaseNote> result = releaseRepository.getSearch(booleanTemplate, project);

            if (result != null) {
                builder.and(release.in(result));
            }

        }

        return builder.getValue();
    }

    /**
     * 프로젝트 생성 시 큐를 생성하고 바인딩하는 메서드
     *
     * @param projectId 프로젝트 식별 번호
     * @author seonwoo
     * @date 2023-08-09 (수)
     */
    private void createAndBindQueueAndRegisterListener(Long projectId) {
        String queueName = "releaser.project." + projectId;
        String routingKey = "releaser.project." + projectId;

        createProjectQueue(queueName);
        bindProjectQueue(queueName, routingKey);
        registerListener(queueName);
    }

    /**
     * 프로젝트 큐를 생성한다.
     *
     * @param queueName 큐 이름
     * @author seonwoo
     * @date 2023-08-09 (수)
     */
    private void createProjectQueue(String queueName) {
        Queue queue = new Queue(queueName, true, false, false);
        rabbitAdmin.declareQueue(queue);
    }

    /**
     * 생성한 프로젝트 큐를 바인딩한다.
     *
     * @param queueName  큐 이름
     * @param routingKey 라우팅 키
     * @author seonwoo
     * @date 2023-08-09 (수)
     */
    private void bindProjectQueue(String queueName, String routingKey) {
        Binding binding = BindingBuilder.bind(new Queue(queueName)).to(projectDirectExchange).with(routingKey);
        rabbitAdmin.declareBinding(binding);
    }

    /**
     * 리스너를 등록한다.
     *
     * @param queueName 큐 이름
     * @author seonwoo
     * @date 2023-08-09 (수)
     */
    private void registerListener(String queueName) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setMessageListener(new MessageListenerAdapter(this, "receiveMessagePerProject"));
        container.start();
    }
}
