package com.momentum.releaser.domain.issue.application;

import static com.momentum.releaser.domain.issue.dto.IssueResponseDto.*;
import static com.momentum.releaser.global.config.BaseResponseStatus.*;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import com.momentum.releaser.domain.issue.dto.IssueDataDto.IssueDetailsDataDTO;
import com.momentum.releaser.domain.notification.event.IssueMessageEvent;
import com.momentum.releaser.domain.notification.event.NotificationEventPublisher;
import com.momentum.releaser.rabbitmq.MessageDto.IssueMessageDto;
import com.momentum.releaser.redis.issue.IssueStatus;
import com.momentum.releaser.redis.issue.OrderIssue;
import com.momentum.releaser.redis.issue.OrderIssueRedisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import com.momentum.releaser.domain.issue.dao.IssueNumRepository;
import com.momentum.releaser.domain.issue.dao.IssueOpinionRepository;
import com.momentum.releaser.domain.issue.dao.IssueRepository;
import com.momentum.releaser.domain.issue.domain.*;
import com.momentum.releaser.domain.issue.dto.IssueRequestDto.IssueInfoRequestDTO;
import com.momentum.releaser.domain.issue.dto.IssueRequestDto.RegisterOpinionRequestDTO;
import com.momentum.releaser.domain.issue.mapper.IssueMapper;
import com.momentum.releaser.domain.project.dao.ProjectMemberRepository;
import com.momentum.releaser.domain.project.dao.ProjectRepository;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.project.dto.ProjectDataDto.GetMembersDataDTO;
import com.momentum.releaser.domain.release.dao.release.ReleaseRepository;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.global.config.BaseResponseStatus;
import com.momentum.releaser.global.exception.CustomException;

import javax.swing.text.html.Option;

/**
 * 이슈 관련된 기능을 제공하는 서비스 구현 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueServiceImpl implements IssueService {

    private final IssueRepository issueRepository;
    private final IssueOpinionRepository issueOpinionRepository;
    private final IssueNumRepository issueNumRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ReleaseRepository releaseRepository;

    private final OrderIssueRedisRepository orderIssueRedisRepository;

    private final NotificationEventPublisher notificationEventPublisher;

    /**
     * 7.1 이슈 생성
     *
     * @author chaeanna
     * @date 2023-07-27
     */
    @Override
    @Transactional
    public IssueIdResponseDTO addIssue(String userEmail, Long projectId, IssueInfoRequestDTO createReq) {
        ProjectMember projectMember = null;

        // 담당자 memberId가 null이 아닌 경우 프로젝트 멤버 조회
        if (createReq.getMemberId() != null) {
            projectMember = getProjectMemberById(createReq.getMemberId());
        }

        Project project = getProjectById(projectId);

        // 이슈를 생성하고 이슈 번호를 할당하여 저장
        Issue newIssue = createIssueNumAndSaveIssue(createReq, project, projectMember);

        // 이슈 생성 시 알림
        notifyIssueAll(project, newIssue);

        // 이슈 담당자 할당 시 알림
        notifyIssueOne(userEmail, project, newIssue, null);

        return IssueIdResponseDTO.builder()
                .issueId(newIssue.getIssueId())
                .issueNum(newIssue.getIssueNum().getIssueNum())
                .build();
    }

    /**
     * 7.2 이슈 수정
     *
     * @param email 사용자 이메일
     * @author chaeanna
     * @date 2023-07-05
     */
    @Override
    @Transactional
    public IssueModifyResponseDTO modifyIssue(Long issueId, String email, IssueInfoRequestDTO updateReq) {
        // 이슈 정보 조회
        Issue issue = getIssueById(issueId);
        ProjectMember previousMember = issue.getMember();

        // Token UserInfo
        User user = getUserByEmail(email);
        ProjectMember projectMember = getProjectMemberByUserAndProject(user, issue.getProject());

        // 접근한 유저가 멤버일 경우 edit 상태 변경
        char edit = decideEditStatus(projectMember);

        ProjectMember manager = null;
        // 담당자 memberId가 null이 아닌 경우 프로젝트 멤버 조회
        if (updateReq.getMemberId() != null) {
            manager = getProjectMemberById(updateReq.getMemberId());
        }

        // 이슈 업데이트
        issue.updateIssue(updateReq, edit, manager);
        Issue updatedIssue = issueRepository.save(issue);

        // 이슈 담당자 할당 변경 시 알림
        notifyIssueOne(email, issue.getProject(), updatedIssue, previousMember);

        return IssueMapper.INSTANCE.toIssueModifyResponseDTO(projectMember);
    }

    /**
     * 7.3 이슈 제거
     *
     * @author chaeanna
     * @date 2023-07-09
     */
    @Override
    @Transactional
    public String removeIssue(Long issueId) {
        // 이슈 정보 조회
        Issue issue = getIssueById(issueId);

        // issue와 연결된 릴리즈가 있으면 삭제가 불가능, 예외 발생
        if (issue.getRelease() != null) {
            Long releaseId = issue.getRelease().getReleaseId();
            throw new CustomException(CONNECTED_RELEASE_EXISTS, releaseId);
        }

        // 이슈 번호와 이슈 삭제
        issueNumRepository.deleteById(issue.getIssueNum().getIssueNumId());
        issueRepository.deleteById(issue.getIssueId());

        return "이슈가 삭제되었습니다.";
    }

    /**
     * 7.4 프로젝트별 모든 이슈 조회
     *
     * @author chaeanna
     * @date 2023-07-08
     */
    @Override
    @Transactional
    public AllIssueListResponseDTO findAllIssues(Long projectId) {
        // 프로젝트 정보 조회
        Project findProject = getProjectById(projectId);


        // 해당 프로젝트에 속하는 모든 이슈 정보
        List<IssueInfoResponseDTO> getAllIssue = issueRepository.getIssues(findProject);

        // 각 상태별로 이슈를 분류
        List<IssueInfoResponseDTO> notStartedList = filterAndSetDeployStatus(projectId, getAllIssue, "NOT_STARTED");
        List<IssueInfoResponseDTO> inProgressList = filterAndSetDeployStatus(projectId, getAllIssue, "IN_PROGRESS");
        List<IssueInfoResponseDTO> doneList = filterAndSetDeployStatus(projectId, getAllIssue, "DONE");

        // 분류된 리스트들을 담아 반환
        return AllIssueListResponseDTO.builder()
                .getNotStartedList(notStartedList)
                .getInProgressList(inProgressList)
                .getDoneList(doneList)
                .build();
    }

    /**
     * 7.5 프로젝트별 해결 & 미연결 이슈 조회
     *
     * @author chaeanna
     * @date 2023-07-08
     */
    @Override
    @Transactional
    public List<DoneIssuesResponseDTO> findDoneIssues(Long projectId, String status) {
        // 프로젝트 정보 조회
        Project findProject = getProjectById(projectId);

        // 해당 프로젝트에서 지정된 상태(status)인 이슈 목록
        List<DoneIssuesResponseDTO> getDoneIssue = issueRepository.getDoneIssues(findProject, status.toUpperCase());
        // 이슈에 연결된 멤버가 없는 경우, memberId를 0으로 설정
        for (DoneIssuesResponseDTO doneIssue : getDoneIssue) {
            if (doneIssue.getMemberId() != null) {
                Optional<ProjectMember> projectMember = projectMemberRepository.findById(doneIssue.getMemberId());
                if (projectMember.isEmpty()) {
                    doneIssue.setMemberId(0L);
                }
            }
        }

        return getDoneIssue;
    }

    /**
     * 7.6 릴리즈 노트별 연결된 이슈 조회
     *
     * @author chaeanna
     * @date 2023-07-08
     */
    @Override
    @Transactional
    public List<ConnectionIssuesResponseDTO> findConnectIssues(Long projectId, Long releaseId) {
        // 프로젝트 정보 조회
        Project findProject = getProjectById(projectId);

        // 릴리즈 노트 정보 조회
        ReleaseNote findReleaseNote = getReleaseNoteById(releaseId);

        // 특정 릴리즈 노트에 연결된 이슈 목록
        List<ConnectionIssuesResponseDTO> getConnectionIssues = issueRepository.getConnectionIssues(findProject, findReleaseNote);

        return getConnectionIssues;
    }

    /**
     * 7.7 이슈별 조회
     *
     * @param email 사용자 이메일
     * @author chaeanna
     * @date 2023-07-09
     */
    @Override
    @Transactional
    public IssueDetailsDTO findIssue(Long issueId, String email) {
        // 이슈 정보 조회
        Issue issue = getIssueById(issueId);

        // Token UserInfo
        User user = getUserByEmail(email);

        // 사용자가 프로젝트 멤버인지 확인, 해당하는 프로젝트 멤버 정보 가져옴.
        Long memberId = getProjectMemberByUserAndProject(user, issue.getProject()).getMemberId();
        ProjectMember member = getProjectMemberById(memberId);

        // 프로젝트 멤버가 이슈를 조회하는 경우, edit 상태 변경
        updateIssueEdit(issue, member);

        // 이슈의 의견 리스트, 해당 프로젝트 멤버의 의견은 삭제 여부를 포함
        List<OpinionInfoResponseDTO> opinionRes = getIssueOpinionsWithDeleteYN(issue, memberId);

        // 프로젝트의 모든 멤버 리스트
        List<GetMembersDataDTO> memberRes = getProjectMembers(member.getProject());

        IssueDetailsDTO getIssue = createIssueDetails(member, issue, memberRes, opinionRes);
        log.info("endDate : {}", getIssue.getIssueDetails().getEndDate());

        return getIssue;
    }

    /**
     * 7.8 이슈 상태 변경
     *
     * @param issueId   상태 변경할 이슈 식별 번호
     * @param index     순서
     * @param lifeCycle 변경할 이슈의 상태 ("NOT_STARTED", "IN_PROGRESS", "DONE" 중 하나로 대소문자 구분 없이 입력)
     * @author chaeanna
     * @date 2023-07-08
     */
    @Override
    @Transactional
    public String modifyIssueLifeCycle(Long issueId, Integer index, String lifeCycle) {
        // 이슈 정보 조회
        Issue issue = getIssueById(issueId);

        // 연결된 이슈가 있고 상태의 src != dest일 경우 상태 변경이 불가능, 예외 발생
        if (issue.getRelease() != null && !Objects.equals(issue.getLifeCycle().toString(), lifeCycle.toUpperCase())) {
            throw new CustomException(CONNECTED_ISSUE_EXISTS);
        }

        // 이슈의 상태 변경
        String result = changeLifeCycle(issue, index, lifeCycle.toUpperCase());

        return result;
    }

    /**
     * 8.1 이슈 의견 추가
     *
     * @param email 사용자의 이메일
     * @author chaeanna
     * @date 2023-07-08
     */
    @Override
    @Transactional
    public List<OpinionInfoResponseDTO> addIssueOpinion(Long issueId, String email, RegisterOpinionRequestDTO issueOpinionReq) {
        // 이슈 정보 조회
        Issue issue = getIssueById(issueId);
        // 사용자 정보 조회
        User user = getUserByEmail(email);

        // 접근한 사용자의 프로젝트 멤버 정보 조회
        ProjectMember member = getProjectMemberByUserAndProject(user, issue.getProject());

        // 의견 등록
        saveOpinion(issue, member, issueOpinionReq.getOpinion());

        // 등록된 의견 리스트 조회 (삭제 여부 포함)
        List<OpinionInfoResponseDTO> opinionRes = getIssueOpinionsWithDeleteYN(issue, member.getMemberId());

        return opinionRes;
    }

    /**
     * 8.2 이슈 의견 삭제
     *
     * @param email 사용자의 이메일
     * @throws CustomException 삭제 권한이 없을 경우 예외 발생
     * @author chaeanna
     * @date 2023-07-08
     */
    @Override
    @Transactional
    public List<OpinionInfoResponseDTO> removeIssueOpinion(Long opinionId, String email) {
        // 사용자 정보 조회
        User user = getUserByEmail(email);

        // 의견 정보 조회
        IssueOpinion issueOpinion = getOpinionById(opinionId);

        // 접근 유저가 해당 의견 작성자인지 확인하여 삭제 권한이 있으면 삭제
        if (equalsMember(user, issueOpinion)) {
            // 의견 soft delete
            issueOpinionRepository.deleteById(opinionId);

            // 삭제 후의 이슈에 대한 모든 의견 정보 조회 (삭제 여부 포함)
            Long memberId = getProjectMemberByUserAndProject(user, issueOpinion.getIssue().getProject()).getMemberId();
            List<OpinionInfoResponseDTO> opinionRes = getIssueOpinionsWithDeleteYN(issueOpinion.getIssue(), memberId);
            return opinionRes;
        } else {
            throw new CustomException(NOT_ISSUE_COMMENTER);
        }
    }

    // =================================================================================================================

    /**
     * memberId로 프로젝트 멤버 가져오기
     *
     * @param memberId 조회할 프로젝트 멤버의 식별 번호
     * @return ProjectMember 프로젝트 멤버 정보
     * @throws CustomException 프로젝트 멤버가 존재하지 않을 경우 예외 발생
     * @author chaeanna
     * @date 2023-07-05
     */
    ProjectMember getProjectMemberById(Long memberId) {
        return projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_PROJECT_MEMBER));
    }

    /**
     * 사용자와 프로젝트로 프로젝트 멤버 가져오기
     *
     * @param user    사용자 엔티티
     * @param project 프로젝트 엔티티
     * @return ProjectMember 프로젝트 멤버 정보
     * @author chaeanna
     * @date 2023-07-05
     */
    private ProjectMember getProjectMemberByUserAndProject(User user, Project project) {
        return projectMemberRepository.findByUserAndProject(user, project).orElseThrow(() -> new CustomException(NOT_EXISTS_PROJECT_MEMBER));
    }

    /**
     * projectId로 프로젝트 가져오기
     *
     * @param projectId 조회할 프로젝트의 식별 번호
     * @return Project 프로젝트 정보
     * @throws CustomException 프로젝트가 존재하지 않을 경우 예외 발생
     * @author chaeanna
     * @date 2023-07-05
     */
    private Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(BaseResponseStatus.NOT_EXISTS_PROJECT));
    }

    /**
     * 이슈 저장
     *
     * @param issueInfoReq  이슈 정보를 담고 있는 요청 DTO
     * @param project       이슈가 속하는 프로젝트 정보
     * @param projectMember 이슈를 등록하는 프로젝트 멤버 정보
     * @return Issue 생성된 이슈 정보
     * @author chaeanna
     * @date 2023-07-05
     */
    private Issue createIssueNumAndSaveIssue(IssueInfoRequestDTO issueInfoReq, Project project, ProjectMember projectMember) {
        // 프로젝트에 저장된 마지막 이슈 번호를 조회 후 + 1
        Long number = issueRepository.getIssueNum(project) + 1;

        // 새로운 이슈 생성
        Issue issue = issueRepository.save(Issue.builder()
                .title(issueInfoReq.getTitle())
                .content(issueInfoReq.getContent())
                .tag(Tag.valueOf(issueInfoReq.getTag().toUpperCase()))
                .endDate(issueInfoReq.getEndDate())
                .project(project)
                .member(projectMember)
                .build());

        // 이슈 번호 정보 생성, 이슈 연결
        IssueNum issueNum = saveIssueNumberForIssue(project, issue, number);

        // 이슈 업데이트
        issue.updateIssueNum(issueNum);

        // 기존에 저장된 이슈 순서
        Optional<OrderIssue> optionalOrderIssue = orderIssueRedisRepository.findByProjectId(issue.getProject().getProjectId());

        // 새로 생성하려는 이슈 인덱스
        IssueStatus issueStatus = IssueStatus.builder()
                .issueId(issue.getIssueId())
                .lifeCycle(issue.getLifeCycle().toString())
                .index(0)
                .build();

        // 기존에 있는 이슈 인덱스들의 값 업데이트
        optionalOrderIssue.ifPresent(orderIssue -> {
            orderIssue.getIssueStatusList().forEach(issueStatusItem -> {
                if (issueStatusItem.getLifeCycle().equals(issue.getLifeCycle().toString())) {
                    issueStatusItem.updateIndex(issueStatusItem.getIndex() + 1);
                }
            });
            orderIssue.updateOrderIndex(issueStatus);
            orderIssueRedisRepository.save(orderIssue);
        });

        if (optionalOrderIssue.isEmpty()) {
            OrderIssue orderIssueResult = OrderIssue.builder()
                    .id(UUID.randomUUID().toString())
                    .projectId(project.getProjectId())
                    .issueStatusList(Collections.singletonList(issueStatus))
                    .build();

            orderIssueRedisRepository.save(orderIssueResult);
        }

        return issue;
    }

    /**
     * 이슈 번호 저장 및 연결
     *
     * @param project  이슈가 속하는 프로젝트 정보
     * @param newIssue 새로 생성된 이슈 정보
     * @param number   새로 생성된 이슈 번호
     * @return IssueNum 생성된 이슈 번호 정보
     * @author chaeanna
     * @date 2023-07
     */
    private IssueNum saveIssueNumberForIssue(Project project, Issue newIssue, Long number) {
        // 새로운 이슈 번호 정보 생성, 프로젝트와 이슈 연결
        return issueNumRepository.save(IssueNum.builder()
                .issue(newIssue)
                .project(project)
                .issueNum(number)
                .build());
    }

    /**
     * issueId로 issue 가져오기
     *
     * @param issueId 조회할 이슈의 식별 번호
     * @return Issue 조회된 이슈 정보
     * @throws CustomException 이슈가 존재하지 않을 경우 예외 발생
     * @author chaeanna
     * @date 2023-07-05
     */
    private Issue getIssueById(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_ISSUE));
    }

    /**
     * email로 user 가져오기
     *
     * @param email 조회할 사용자의 이메일 주소
     * @return User 조회된 사용자 정보
     * @throws CustomException 사용자가 존재하지 않을 경우 예외 발생
     * @author chaeanna
     * @date 2023-07-05
     */
    private User getUserByEmail(String email) {
        return userRepository.findOneByEmail(email)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_USER));
    }

    /**
     * 편집 여부를 멤버의 역할에 따라 결정
     *
     * @param member 조회할 멤버 엔티티
     * @return 편집 가능 여부 ('Y' 또는 'N')
     * @throws CustomException 멤버가 존재하지 않을 경우 예외 발생
     * @author chaeanna
     * @date 2023-07-05
     */
    private char decideEditStatus(ProjectMember member) {
        // 멤버의 포지션이 'M'인 경우 'Y'(편집 가능)을 반환하고, 그 외에는 'N'(편집 불가능)을 반환합니다.
        return (member.getPosition() == 'M') ? 'Y' : 'N';
    }

    /**
     * 이슈 필터링 및 배포 상태 설정
     *
     * @param issues    이슈 리스트
     * @param lifeCycle 필터링할 배포 상태 (NOT_STARTED, IN_PROGRESS, DONE 중 하나로 대소문자 구분 없이 입력)
     * @return IssueInfoResponseDTO 필터링된 이슈 리스트
     * @author chaeanna
     * @date 2023-07-08
     */
    private List<IssueInfoResponseDTO> filterAndSetDeployStatus(Long projectId, List<IssueInfoResponseDTO> issues, String lifeCycle) {
        Optional<OrderIssue> optionalOrderIssue = orderIssueRedisRepository.findByProjectId(projectId);

        if (optionalOrderIssue.isEmpty()) {
            // 만약 optionalOrderIssue가 비어있다면 정렬을 무시하고 입력 리스트를 그대로 반환
            return issues.stream()
                    .filter(issue -> lifeCycle.equalsIgnoreCase(issue.getLifeCycle()))
                    .peek(issueInfoRes -> {
                        // 이슈 정보 조회
                        Issue issue = getIssueById(issueInfoRes.getIssueId());

                        // 이슈에 연결된 멤버의 식별 번호 조회
                        Long memberId = issueInfoRes.getMemberId();

                        // 멤버 식별 번호가 null이 아니면서 프로젝트 멤버가 존재하지 않을 경우, 멤버 식별 번호를 0으로 설정
                        if (memberId != null && projectMemberRepository.findById(memberId).isEmpty()) {
                            issueInfoRes.setMemberId(0L);
                        }
                        // 연결된 릴리즈 존재, 릴리즈의 배포 상태가 "DEPLOYED"인 경우, deployYN 'Y' 설정, 그렇지 않은 경우 'N' 설정
                        if (issue.getRelease() != null && "DEPLOYED".equalsIgnoreCase(String.valueOf(issue.getRelease().getDeployStatus()))) {
                            issueInfoRes.setDeployYN('Y');
                        } else {
                            issueInfoRes.setDeployYN('N');
                        }
                    })
                    .collect(Collectors.toList());
        }

        // issue 순서
        OrderIssue orderIssue = optionalOrderIssue.get();
        List<IssueStatus> issueStatusList = orderIssue.getIssueStatusList();

        return issues.stream()
                .filter(issue -> lifeCycle.equalsIgnoreCase(issue.getLifeCycle()))
                .peek(issueInfoRes -> {
                    // 이슈 정보 조회
                    Issue issue = getIssueById(issueInfoRes.getIssueId());

                    // 이슈에 연결된 멤버의 식별 번호 조회
                    Long memberId = issueInfoRes.getMemberId();

                    // 멤버 식별 번호가 null이 아니면서 프로젝트 멤버가 존재하지 않을 경우, 멤버 식별 번호를 0으로 설정
                    if (memberId != null && projectMemberRepository.findById(memberId).isEmpty()) {
                        issueInfoRes.setMemberId(0L);
                    }
                    // 연결된 릴리즈 존재, 릴리즈의 배포 상태가 "DEPLOYED"인 경우, deployYN 'Y' 설정, 그렇지 않은 경우 'N' 설정
                    if (issue.getRelease() != null && "DEPLOYED".equalsIgnoreCase(String.valueOf(issue.getRelease().getDeployStatus()))) {
                        issueInfoRes.setDeployYN('Y');
                    } else {
                        issueInfoRes.setDeployYN('N');
                    }
                })
                .sorted(Comparator.comparingInt(issueInfoRes -> {
                    // 이슈 상태 리스트에서 해당 이슈의 index를 찾아서 반환
                    IssueStatus status = issueStatusList.stream()
                            .filter(result -> result.getIssueId().equals(issueInfoRes.getIssueId()))
                            .findFirst()
                            .orElse(null);
                    return status != null ? status.getIndex() : Integer.MAX_VALUE;
                }))
                .collect(Collectors.toList());
    }

    /**
     * releaseId로 releaseNote 찾기
     *
     * @param releaseId 조회할 릴리즈 노트의 식별 번호
     * @return ReleaseNote 조회된 릴리즈 노트 정보
     * @throws CustomException 릴리즈 노트가 존재하지 않을 경우 예외 발생
     * @author chaeanna
     * @date 2023-07-08
     */
    private ReleaseNote getReleaseNoteById(Long releaseId) {
        return releaseRepository.findById(releaseId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_RELEASE_NOTE));
    }

    /**
     * 이슈 상세 정보 생성
     *
     * @param issue      이슈 정보
     * @param memberRes  멤버 리스트
     * @param opinionRes 의견 리스트
     * @return IssueDetailsDTO 생성된 이슈 상세 정보
     * @author chaeanna
     * @date 2023-07-09
     */
    private IssueDetailsDTO createIssueDetails(ProjectMember member, Issue issue, List<GetMembersDataDTO> memberRes, List<OpinionInfoResponseDTO> opinionRes) {
        // 이슈 상세 정보 생성
        IssueDetailsDataDTO getIssue = IssueMapper.INSTANCE.mapToGetIssue(issue, memberRes, opinionRes);

        // 이슈에 연결된 담당자의 식별 번호 조회
        Long memberId = getIssue.getManager();

        // 담당자 식별 번호가 null이 아니면서 프로젝트 멤버가 존재하지 않는 경우, 담당자 식별 번호를 0으로 설정
        if (memberId != null && projectMemberRepository.findById(memberId).isEmpty()) {
            getIssue.setManager(0L);
        }

        // 연결된 릴리즈 존재, 릴리즈의 배포 상태가 "DEPLOYED"인 경우, deployYN 'Y' 설정, 그렇지 않은 경우 'N' 설정
        ReleaseNote release = issue.getRelease();
        getIssue.setDeployYN(release != null && "DEPLOYED".equals(String.valueOf(release.getDeployStatus())) ? 'Y' : 'N');

        return IssueDetailsDTO.builder().pmCheck(member.getPosition() == 'L' ? 'Y' : 'N').issueDetails(getIssue).build();
    }

    /**
     * 이슈 편집 상태 업데이트
     *
     * @param issue  이슈 정보
     * @param member 프로젝트 멤버 정보
     * @author chaeanna
     * @date 2023-07-09
     */
    private void updateIssueEdit(Issue issue, ProjectMember member) {
        Project project = issue.getProject();

        // 멤버의 포지션 'L'인 경우 이슈의 편집 상태를 'N'(편집 불가능)로 업데이트
        boolean hasEditor = project.getMembers().stream()
                .anyMatch(m -> m.getPosition() == 'L' && m.getMemberId() == member.getMemberId());

        if (hasEditor) {
            issue.updateIssueEdit('N');
        }
    }

    /**
     * 이슈의 의견 목록 조회 및 삭제 가능 여부 설정
     *
     * @param issue    이슈 정보
     * @param memberId 멤버 식별 번호
     * @return OpinionInfoResponseDTO 이슈의 의견 목록
     * @author chaeanna
     * @date 2023-07-09
     */
    private List<OpinionInfoResponseDTO> getIssueOpinionsWithDeleteYN(Issue issue, Long memberId) {
        // 이슈의 의견 목록 조회
        List<OpinionInfoResponseDTO> issueOpinion = issueRepository.getIssueOpinion(issue);

        // 각 의견에 대해 주어진 멤버 식별 번호와 비교하여 삭제 가능 여부 설정
        for (OpinionInfoResponseDTO opinion : issueOpinion) {
            // 의견의 작성자가 일치하는 경우 deleteYN 'Y'로 설정, 그렇지 않은 경우 'N' 설정
            opinion.setDeleteYN(memberId != null && opinion.getMemberId().equals(memberId) ? 'Y' : 'N');
        }

        return issueOpinion;
    }

    /**
     * 프로젝트 멤버 리스트 조회
     *
     * @param project 프로젝트 정보
     * @return GetMembers 프로젝트의 멤버 리스트
     * @author chaeanna
     * @date 2023-07-09
     */
    private List<GetMembersDataDTO> getProjectMembers(Project project) {
        // 프로젝트에 속한 멤버 리스트를 조회
        List<GetMembersDataDTO> issueMember = projectRepository.getMemberList(project);

        return issueMember;
    }

    /**
     * 이슈 상태 변경
     *
     * @param issue 이슈 정보
     * @param destLifeCycle 변경할 상태 ("NOT_STARTED", "IN_PROGRESS", "DONE" 중 하나로 대소문자 구분 없이 입력)
     * @return String "이슈 상태 변경이 완료되었습니다."
     * @author chaeanna
     * @date 2023-07-08
     */
    private String changeLifeCycle(Issue issue, Integer index, String destLifeCycle) {
        // 기존 lifeCycle
        String srcLifeCycle = String.valueOf(issue.getLifeCycle());

        // 이슈 순서 업데이트
        Optional<OrderIssue> optionalOrderIssue = orderIssueRedisRepository.findByProjectId(issue.getProject().getProjectId());

        if (optionalOrderIssue.isPresent()) {
            OrderIssue orderIssue = optionalOrderIssue.get();
            Long updateIssueId = issue.getIssueId();
            // updateIssueId가 issueStatusList에 있는지 확인하는 코드
            IssueStatus issueStatusResult = orderIssue.getIssueStatusList().stream().filter(issueStatus -> issueStatus.getIssueId().equals(updateIssueId)).findFirst().get();

            if (issueStatusResult != null && srcLifeCycle.equals(destLifeCycle)) {
                List<IssueStatus> issueList = orderIssue.getIssueStatusList().stream()
                        .filter(issueStatus -> issueStatus.getLifeCycle().equals(destLifeCycle))
                        .sorted(Comparator.comparingInt(IssueStatus::getIndex))
                        .collect(Collectors.toList());

                if (index >= 0 && index < issueList.size()) {
                    IssueStatus targetIssue = issueList.get(index);
                    int newIndex = issueList.indexOf(targetIssue);

                    // 두 이슈의 위치를 서로 바꿉니다
                    IssueStatus originalIssue = issueList.get(newIndex);
                    issueList.set(newIndex, targetIssue);
                    issueList.set(index, originalIssue);

                    // 인덱스를 업데이트합니다
                    for (int i = 0; i < issueList.size(); i++) {
                        issueList.get(i).updateIndex(i);
                    }
                }

                List<IssueStatus> issueStatusList = orderIssue.getIssueStatusList().stream()
                        .filter(issueStatus -> !(issueStatus.getLifeCycle().equals(destLifeCycle)))
                                .collect(Collectors.toList());

                List<IssueStatus> updatedIssueStatusList = new ArrayList<>(issueList);
                updatedIssueStatusList.addAll(issueStatusList);
                orderIssue.updateIssueStatusList(updatedIssueStatusList);

                orderIssueRedisRepository.save(orderIssue);
            } else {

                boolean exists = orderIssue.getIssueStatusList().stream()
                        .anyMatch(status -> status.getIssueId().equals(updateIssueId));
                // 해당 이슈가 존재하면 업데이트
                if (exists) {

                    // index가 0이면
                    if (index == 0) {
                        // destLifeCycle과 같은 lifeCycle이며 모든 인덱스 +1
                        orderIssue.getIssueStatusList().forEach(issueStatusItem -> {
                            if (issueStatusItem.getLifeCycle().equals(destLifeCycle)) {
                                issueStatusItem.updateIndex(issueStatusItem.getIndex() + 1);
                            }
                        });
                    } else {
                        // destLifeCycle과 같은 lifeCycle이며 index 이상 +1
                        orderIssue.getIssueStatusList().forEach(issueStatusItem -> {
                            if (issueStatusItem.getLifeCycle().equals(destLifeCycle) && issueStatusItem.getIndex() >= index) {
                                issueStatusItem.updateIndex(issueStatusItem.getIndex() + 1);
                            }
                        });
                    }

                    // srcLifeCycle과 같은 lifeCycle이며 기존 인덱스보다 큰 인덱스 -1
                    orderIssue.getIssueStatusList().replaceAll(issueStatus -> {
                        if (issueStatus.getLifeCycle().equals(srcLifeCycle) && issueStatus.getIndex() > index) {
                            // 기존 인덱스보다 크면 -1 처리
                            issueStatus.updateIndex(issueStatus.getIndex() - 1);
                        }
                        return issueStatus;
                    });

                    // 순서 변경 저장
                    IssueStatus issueStatus = new IssueStatus(updateIssueId, destLifeCycle, index);
                    orderIssue.updateIssueStatus(issueStatusResult, issueStatus);

                    orderIssueRedisRepository.save(orderIssue);
                }
            }
        }

        // 이슈의 상태를 주어진 상태로 변경
        issue.updateLifeCycle(destLifeCycle);
        issueRepository.save(issue);

        return "이슈 상태 변경이 완료되었습니다.";
    }


    /**
     * 특정 이슈에 대한 의견을 저장하는 메서드입니다.
     *
     * @param issue   이슈 정보
     * @param member  프로젝트 멤버 정보
     * @param opinion 의견 내용
     * @return IssueOpinion 저장된 의견 정보
     * @author chaeanna
     * @date 2023-07-08
     */
    private IssueOpinion saveOpinion(Issue issue, ProjectMember member, String opinion) {
        // 의견 저장
        return issueOpinionRepository.save(IssueOpinion.builder()
                .opinion(opinion)
                .issue(issue)
                .member(member)
                .build());
    }

    /**
     * 특정 사용자가 해당 이슈 의견 작성자인지 확인하는 메서드입니다.
     *
     * @param user    사용자 정보
     * @param opinion 이슈 의견 정보
     * @return boolean 해당 사용자가 이슈 의견 작성자인지 여부
     * @author chaeanna
     * @date 2023-07-08
     */
    boolean equalsMember(User user, IssueOpinion opinion) {
        // 이슈가 속한 프로젝트 정보 조회
        Project project = opinion.getIssue().getProject();

        // 사용자의 프로젝트 멤버 정보 조회
        Long accessMember = getProjectMemberByUserAndProject(user, project).getMemberId();

        // memberId와 이슈 의견 작성자의 memberId 비교하여 일치 여부 반환
        return Objects.equals(accessMember, opinion.getMember().getMemberId());
    }

    /**
     * opinionId로 issueOpinion 가져오기
     *
     * @param opinionId 이슈 의견 식별 번호
     * @return IssueOpinion 이슈 의견 엔티티
     */
    private IssueOpinion getOpinionById(Long opinionId) {
        return issueOpinionRepository.findById(opinionId).orElseThrow(() -> new CustomException(NOT_EXISTS_ISSUE_OPINION));
    }

    private void notifyIssueAll(Project project, Issue issue) {

        if (issue == null || issue.getCreatedDate() == null) {
            throw new CustomException(INVALID_ISSUE);
        }

        // 알림 메시지를 정의한다.
        IssueMessageDto message = IssueMessageDto.builder()
                .type("Issue")
                .projectId(project.getProjectId())
                .projectName(project.getTitle())
                .projectImg(project.getImg())
                .message("새로운 이슈가 생성되었습니다.")
                .date(Date.from(issue.getCreatedDate().atZone(ZoneId.systemDefault()).toInstant()))
                .issueId(issue.getIssueId())
                .build();

        // 알림 메시지를 보낼 대상 목록을 가져온다.
        List<String> consumers = projectMemberRepository.findByProject(project).stream()
                .map(m -> m.getUser().getEmail())
                .collect(Collectors.toList());

        // 이벤트 리스너를 호출하여 이슈 생성 트랜잭션이 완료된 후 호출하도록 한다.
        notificationEventPublisher.notifyIssue(IssueMessageEvent.toNotifyOneIssue(message, consumers));
    }

    private void notifyIssueOne(String userEmail, Project project, Issue issue, ProjectMember member) {

        if (issue.getMember() != member) {
            // 이전 멤버와 같지 않을 때 알림을 보내야 한다.

            // 알림 메시지를 정의한다.
            IssueMessageDto message = IssueMessageDto.builder()
                    .type("Issue")
                    .projectId(project.getProjectId())
                    .projectName(project.getTitle())
                    .projectImg(project.getImg())
                    .message("귀하에게 이슈가 할당되었습니다.")
                    .date(Date.from(issue.getCreatedDate().atZone(ZoneId.systemDefault()).toInstant()))
                    .issueId(issue.getIssueId())
                    .build();

            List<String> consumers = new ArrayList<>();
            consumers.add(userEmail);

            // 이벤트 리스너를 호출하여 이슈 생성 트랜잭션이 완료된 후 호출하도록 한다.
            notificationEventPublisher.notifyIssue(IssueMessageEvent.toNotifyOneIssue(message, consumers));
        }
    }
}
