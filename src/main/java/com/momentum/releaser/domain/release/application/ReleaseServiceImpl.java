package com.momentum.releaser.domain.release.application;

import static com.momentum.releaser.global.config.BaseResponseStatus.*;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import com.momentum.releaser.domain.notification.event.NotificationEventPublisher;
import com.momentum.releaser.domain.notification.event.ReleaseNoteMessageEvent;
import com.momentum.releaser.rabbitmq.MessageDto.ReleaseNoteMessageDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.momentum.releaser.domain.issue.dao.IssueRepository;
import com.momentum.releaser.domain.issue.domain.Issue;
import com.momentum.releaser.domain.issue.domain.LifeCycle;
import com.momentum.releaser.domain.project.dao.ProjectMemberRepository;
import com.momentum.releaser.domain.project.dao.ProjectRepository;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.project.mapper.ProjectMapper;
import com.momentum.releaser.domain.release.dao.approval.ReleaseApprovalRepository;
import com.momentum.releaser.domain.release.dao.opinion.ReleaseOpinionRepository;
import com.momentum.releaser.domain.release.dao.release.ReleaseRepository;
import com.momentum.releaser.domain.release.domain.ReleaseApproval;
import com.momentum.releaser.domain.release.domain.ReleaseEnum.ReleaseDeployStatus;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.domain.release.domain.ReleaseOpinion;
import com.momentum.releaser.domain.release.dto.ReleaseDataDto.CoordinateDataDTO;
import com.momentum.releaser.domain.release.dto.ReleaseDataDto.GetIssueTitleDataDTO;
import com.momentum.releaser.domain.release.dto.ReleaseDataDto.GetTagsDataDTO;
import com.momentum.releaser.domain.release.dto.ReleaseDataDto.ReleaseOpinionsDataDTO;
import com.momentum.releaser.domain.release.dto.ReleaseRequestDto.*;
import com.momentum.releaser.domain.release.dto.ReleaseResponseDto.*;
import com.momentum.releaser.domain.release.mapper.ReleaseMapper;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 릴리즈 노트와 관련된 기능을 제공하는 서비스 구현 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseServiceImpl implements ReleaseService {

    // Domain
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ReleaseRepository releaseRepository;
    private final ReleaseOpinionRepository releaseOpinionRepository;
    private final ReleaseApprovalRepository releaseApprovalRepository;
    private final IssueRepository issueRepository;

    // 알림
    private final NotificationEventPublisher notificationEventPublisher;

    /**
     * 5.1 프로젝트별 릴리즈 노트 목록 조회
     *
     * @param userEmail 사용자 이메일
     * @author seonwoo
     * @date 2023-07-05
     */
    @Transactional(readOnly = true)
    @Override
    public ReleasesResponseDTO findReleaseNotes(String userEmail, Long projectId) {
        // 프로젝트 식별 번호로 프로젝트 엔티티를 가져온다.
        Project project = getProjectById(projectId);

        // 요청을 한 사용자의 프로젝트 내 역할을 가져올 수 있도록 한다.
        ProjectMember member = getProjectMemberByEmail(project, userEmail);

        return ProjectMapper.INSTANCE.toReleasesResponseDto(project, member);
    }

    /**
     * 5.2 릴리즈 노트 생성
     *
     * @param userEmail 사용자 이메일
     * @author seonwoo
     * @date 2023-07-14
     */
    @Transactional
    @Override
    public ReleaseCreateAndUpdateResponseDTO addReleaseNote(String userEmail, Long projectId, ReleaseCreateRequestDTO releaseCreateRequestDto) {
        Project project = getProjectById(projectId);

        // 릴리즈 노트 생성 권한이 있는 프로젝트 멤버인지 확인한다.
        isProjectManager(userEmail, project);

        // 먼저, 클라이언트로부터 받아온 릴리즈 노트를 저장한다.
        ReleaseNote savedReleaseNote = saveReleaseNote(project, releaseCreateRequestDto, createReleaseVersion(project, releaseCreateRequestDto.getVersionType()));

        // 이슈들을 연결한다.
        connectIssues(releaseCreateRequestDto.getIssues(), savedReleaseNote);

        // 생성한 릴리즈 노트에 대한 동의 테이블을 생성한다.
        createReleaseApprovals(savedReleaseNote);

        // 릴리즈 노트 생성 알림
        notifyReleaseNote(project, savedReleaseNote, "새로운 릴리즈 노트가 생성되었습니다.");

        return ReleaseMapper.INSTANCE.toReleaseCreateAndUpdateResponseDto(savedReleaseNote);
    }

    /**
     * 5.3 릴리즈 노트 수정
     *
     * @param userEmail 사용자 이메일
     * @author seonwoo
     * @date 2023-07-14
     */
    @Transactional
    @Override
    public ReleaseCreateAndUpdateResponseDTO saveReleaseNote(String userEmail, Long releaseId, ReleaseUpdateRequestDTO releaseUpdateRequestDto) {
        ReleaseNote releaseNote = getReleaseNoteById(releaseId);

        // 릴리즈 노트 수정 권한이 있는 사용자인지 확인한다. 만약 아니라면, 예외를 발생시킨다.
        isProjectManager(userEmail, releaseNote.getProject());

        // 수정된 릴리즈 노트 내용을 반영 및 저장한다.
        ReleaseNote updatedReleaseNote = updateAndSaveReleaseNote(releaseNote, releaseUpdateRequestDto, updateReleaseVersion(releaseNote, releaseUpdateRequestDto.getVersion()));

        // 이슈를 연결한다.
        connectIssues(releaseUpdateRequestDto.getIssues(), updatedReleaseNote);

        return ReleaseMapper.INSTANCE.toReleaseCreateAndUpdateResponseDto(updatedReleaseNote);
    }

    /**
     * 5.4 릴리즈 노트 삭제
     *
     * @param userEmail 사용자 이메일
     * @author seonwoo
     * @date 2023-07-23
     */
    @Transactional
    @Override
    public String removeReleaseNote(String userEmail, Long releaseId) {
        ReleaseNote releaseNote = getReleaseNoteById(releaseId);

        // 프로젝트의 PM인지 확인한다.
        isProjectManager(userEmail, releaseNote.getProject());

        // 해당 릴리즈 노트 삭제가 가능한지 확인한다.
        validateReleaseNoteDelete(releaseNote);

        // 해당 릴리즈 노트에 대한 배포 동의 여부 데이터를 모두 삭제한다.
        releaseApprovalRepository.deleteByReleaseNote(releaseNote);

        // 해당 릴리즈 노트를 삭제하기 전 연결된 이슈를 모두 해제한다.
        disconnectIssues(releaseNote);

        // 해당 릴리즈 노트를 삭제한다.
        releaseRepository.deleteById(releaseNote.getReleaseId());

        return "릴리즈 노트 삭제에 성공하였습니다.";
    }

    /**
     * 5.5 릴리즈 노트 조회
     *
     * @param userEmail 사용자 이메일
     * @author seonwoo
     * @date 2023-07-12
     */
    @Transactional(readOnly = true)
    @Override
    public ReleaseInfoResponseDTO findReleaseNote(String userEmail, Long releaseId) {
        ReleaseNote releaseNote = getReleaseNoteById(releaseId);

        // 해당 프로젝트 멤버인지 식별한다.
        ProjectMember member = getProjectMember(userEmail, releaseNote.getProject());

        // 만약 릴리즈 노트 의견 목록 중 조회한 사용자가 작성한 댓글이 있다면, 삭제가 가능하도록 해준다.
        List<ReleaseOpinionsDataDTO> opinions = updateToAllowDeleteOpinion(releaseNote, member);

        return createReleaseInfoResponseDto(releaseNote, opinions);
    }

    /**
     * 5.6 릴리즈 노트 배포 동의 여부 선택
     *
     * @param userEmail 사용자 이메일
     * @author seonwoo
     * @date 2023-07-10
     */
    @Transactional
    @Override
    public List<ReleaseApprovalsResponseDTO> modifyReleaseApproval(String userEmail, Long releaseId, ReleaseApprovalRequestDTO releaseApprovalRequestDto) {
        ReleaseNote releaseNote = getReleaseNoteById(releaseId);

        // 프로젝트 멤버가 맞는지 확인하고, 맞다면 프로젝트 멤버를 반환한다.
        ProjectMember member = getProjectMember(userEmail, releaseNote.getProject());

        // 배포 동의 여부를 선택할 수 있는 릴리즈인지 확인한다.
        validateReleaseNoteApproval(member, releaseNote);

        // 릴리즈 노트에 대한 배포 동의 여부를 업데이트한다.
        updateReleaseNoteApproval(member, releaseNote, releaseApprovalRequestDto.getApproval().charAt(0));

        // 프로젝트 멤버들의 업데이트된 동의 여부 목록을 반환한다.
        return getReleaseApprovals(releaseNote);
    }

    /**
     * 5.7 릴리즈 노트 그래프 좌표 추가
     *
     * @author seonwoo
     * @date 2023-07-10
     */
    @Transactional
    @Override
    public String modifyReleaseCoordinate(ReleaseNoteCoordinateRequestDTO releaseNoteCoordinateRequestDto) {
        updateCoordinates(releaseNoteCoordinateRequestDto.getCoordinates());
        return "릴리즈 노트 좌표 업데이트에 성공하였습니다.";
    }

    /**
     * 6.1 릴리즈 노트 의견 추가
     *
     * @param userEmail 사용자 이메일
     * @author seonwoo
     * @date 2023-07-26
     */
    @Transactional
    @Override
    public List<ReleaseOpinionsResponseDTO> addReleaseOpinion(String userEmail, Long releaseId, ReleaseOpinionCreateRequestDTO releaseOpinionCreateRequestDto) {
        ReleaseNote releaseNote = getReleaseNoteById(releaseId);

        // JWT 토큰을 이용하여 요청을 한 사용자의 프로젝트 멤버 정보를 가져온다.
        ProjectMember projectMember = getProjectMemberByEmail(releaseNote.getProject(), userEmail);

        saveReleaseOpinion(releaseNote, projectMember, releaseOpinionCreateRequestDto);

        return createReleaseOpinionsResponseDto(releaseNote, projectMember.getMemberId());
    }

    /**
     * 6.2 릴리즈 노트 의견 삭제
     *
     * @param userEmail 사용자 이메일
     * @author seonwoo
     * @date 2023-07-26
     */
    @Transactional
    @Override
    public List<ReleaseOpinionsResponseDTO> removeReleaseOpinion(String userEmail, Long opinionId) {
        ReleaseOpinion releaseOpinion = getReleaseOpinionById(opinionId);

        // JWT 토큰을 이용하여 요청을 한 사용자의 프로젝트 멤버 정보를 가져온다.
        ProjectMember member = getProjectMemberByEmail(releaseOpinion.getRelease().getProject(), userEmail);

        // 해당 의견을 작성한 사용자가 맞는지 확인한다. 아니라면 예외를 발생시킨다.
        validateOpinionAndMember(releaseOpinion, member);

        releaseOpinionRepository.deleteById(opinionId);

        return createReleaseOpinionsResponseDto(releaseOpinion.getRelease(), member.getMemberId());
    }

    /**
     * 6.3 릴리즈 노트 의견 목록 조회
     *
     * @author seonwoo
     * @date 2023-07-10
     */
    @Transactional(readOnly = true)
    @Override
    public List<ReleaseOpinionsResponseDTO> findReleaseOpinions(Long releaseId) {
        ReleaseNote releaseNote = getReleaseNoteById(releaseId);
        return getReleaseOpinionsResponseDto(releaseNote.getReleaseOpinions());
    }

    /**
     * 9.1 프로젝트별 릴리즈 보고서 조회
     *
     * @author chaeanna
     * @date 2023-07-22
     */
    @Override
    @Transactional
    public List<ReleaseDocsResponseDTO> findReleaseDocs(Long projectId) {
        // 프로젝트 조회
        Project project = getProjectById(projectId);
        // 해당 프로젝트와 연결된 모든 릴리즈 조회
        List<ReleaseNote> releaseNotes = releaseRepository.findAllByProject(project);

        // 릴리즈별로 저장할 결과 리스트 초기화
        List<ReleaseDocsResponseDTO> releaseDocsResList = new ArrayList<>();

        for (ReleaseNote note : releaseNotes) {
            // 릴리즈에 연결된 이슈들 조회
            List<Issue> issues = issueRepository.findByRelease(note);

            // 이슈들을 태그별로 그룹화하여 저장할 맵
            Map<String, List<GetIssueTitleDataDTO>> tagToIssueMap = groupIssuesByTag(issues);

            // ReleaseDocsRes 객체 생성
            ReleaseDocsResponseDTO releaseDocsRes = buildReleaseDocsRes(note, tagToIssueMap);
            releaseDocsResList.add(releaseDocsRes);
        }

        // 버전을 기준으로 내림차순으로 정렬
        Collections.sort(releaseDocsResList, (note1, note2) -> note2.getReleaseVersion().compareTo(note1.getReleaseVersion()));

        return releaseDocsResList;
    }

    /**
     * 9.2 프로젝트별 릴리즈 보고서 수정
     *
     * @param email 사용자 이메일
     * @author chaeanna
     * @date 2023-07-22
     */
    @Transactional
    @Override
    public String modifyReleaseDocs(Long projectId, String email, List<UpdateReleaseDocsRequestDTO> updateReq) {
        // 프로젝트 구성원 정보 조회
        ProjectMember member = getProjectMemberByEmailAndProjectId(email, projectId);

        // 구성원이 'L'인 경우에만 업데이트 가능
        if (member.getPosition() != 'L') {
            throw new CustomException(NOT_ADMIN);
        }

        // 업데이트 요청에 따라 이슈의 요약 업데이트 수행
        for (UpdateReleaseDocsRequestDTO req : updateReq) {
            updateIssueSummary(req.getIssueId(), req);
        }

        return "릴리즈 보고서가 수정되었습니다.";
    }

    /**
     * 프로젝트 식별 번호를 통해 프로젝트 엔티티를 가져온다.
     *
     * @param projectId 프로젝트 식별 번호
     * @return Project 프로젝트 엔티티
     * @throws CustomException 프로젝트가 존재하지 않을 경우 예외 발생
     * @author seonwoo
     * @date 2023-07-05
     */
    private Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_PROJECT));
    }

    /**
     * 릴리즈 식별 번호를 통해 릴리즈 엔티티를 가져온다.
     *
     * @param releaseId 릴리즈 식별 번호
     * @return ReleaseNote 릴리즈 엔티티
     * @throws CustomException 릴리즈 노트가 존재하지 않을 경우 예외 발생
     * @author seonwoo
     * @date 2023-07-14
     */
    private ReleaseNote getReleaseNoteById(Long releaseId) {
        return releaseRepository.findById(releaseId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_RELEASE_NOTE));
    }

    /**
     * 릴리즈 노트 의견 식별 번호를 통해 릴리즈 의견 엔티티를 가져온다.
     *
     * @param opinionId 릴리즈 의견 식별 번호
     * @return ReleaseOpinion 릴리즈 의견 엔티티
     * @throws CustomException 릴리즈 의견이 존재하지 않을 경우 예외 발생
     * @author seonwoo
     * @date 2023-07-26
     */
    private ReleaseOpinion getReleaseOpinionById(Long opinionId) {
        return releaseOpinionRepository.findById(opinionId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_RELEASE_OPINION));
    }

    /**
     * 프로젝트 조회 시 해당 요청을 한 사용자의 프로젝트 멤버의 역할을 알려주기 위해 프로젝트 멤버 엔티티를 가져온다.
     *
     * @param project 프로젝트 엔티티
     * @param email   사용자 이메일
     * @return ProjectMember 프로젝트 멤버 엔티티
     * @throws CustomException NOT_EXISTS_USER 사용자가 존재하지 않을 경우 예외 발생
     * @throws CustomException NOT_EXISTS_PROJECT_MEMBER 프로젝트 멤버가 존재하지 않을 경우 예외 발생
     * @author seonwoo
     * @date 2023-07-05
     */
    private ProjectMember getProjectMemberByEmail(Project project, String email) {
        // 사용자 이메일을 통해 사용자 엔티티를 가져온다.
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(NOT_EXISTS_USER));

        // 사용자 엔티티를 통해 프로젝트 멤버 엔티티를 가져온다.
        return projectMemberRepository.findOneByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_PROJECT_MEMBER));
    }

    /**
     * 이슈 식별 번호를 통해 이슈 엔티티 목록을 가져온다.
     *
     * @param issues 이슈 식별 번호 목록
     * @return Issue 이슈 식별 번호에 해당하는 이슈 엔티티 목록
     * @throws CustomException 이슈 식별 번호에 해당하는 이슈가 존재하지 않을 경우 예외 발생
     * @author seonwoo
     * @date 2023-07-07
     */
    private List<Issue> getIssuesById(List<Long> issues) {
        return issues.stream()
                .map(i -> issueRepository.findById(i).orElseThrow(() -> new CustomException(NOT_EXISTS_ISSUE)))
                .collect(Collectors.toList());
    }

    /**
     * 릴리즈 버전 타입을 통해 올바른 릴리즈 버전을 생성한다.
     *
     * @param project     프로젝트 엔티티
     * @param versionType 릴리즈 버전 타입 ("MAJOR", "MINOR", "PATCH" 중 하나)
     * @return String 새로 생성된 릴리즈 버전
     * @throws CustomException INVALID_RELEASE_VERSION_TYPE 클라이언트로부터 받은 버전 타입이 올바르지 않은 경우 발생하는 예외
     * @throws CustomException FAILED_TO_GET_LATEST_RELEASE_VERSION 데이터베이스에서 최신 버전 정보를 가져오지 못한 경우 발생하는 예외
     * @author seonwoo
     * @date 2023-07-14
     */
    String createReleaseVersion(Project project, String versionType) {
        String newVersion = "";

        // 데이터베이스로부터 가장 최신의 버전을 가져온다.
        List<String> releaseVersions = releaseRepository.findAllVersionsByProject(project);

        if (releaseVersions.isEmpty()) {
            // 데이터베이스에서 가장 최신의 버전을 가져오지 못한 경우
            int size = releaseRepository.findAllByProject(project).size();

            if (size != 0) {
                throw new CustomException(FAILED_TO_GET_LATEST_RELEASE_VERSION);
            } else {
                // 처음 생성하는 릴리즈 노트인 경우
                newVersion = "1.0.0";
            }

        } else {
            // 데이터베이스에서 가장 최신의 버전을 가져온 경우
            releaseVersions = getLatestVersion(releaseVersions);
            String latestVersion = releaseVersions.get(0);

            String[] eachVersion = latestVersion.split("\\.");
            int latestMajorVersion = Integer.parseInt(eachVersion[0]);
            int latestMinorVersion = Integer.parseInt(eachVersion[1]);
            int latestPatchVersion = Integer.parseInt(eachVersion[2]);

            // 버전 종류에 따른 버전을 생성한다.
            switch (versionType.toUpperCase()) {
                case "MAJOR":
                    newVersion = (latestMajorVersion + 1) + ".0.0";
                    break;

                case "MINOR":
                    newVersion = latestMajorVersion + "." + (latestMinorVersion + 1) + ".0";
                    break;

                case "PATCH":
                    newVersion = latestMajorVersion + "." + latestMinorVersion + "." + (latestPatchVersion + 1);
                    break;

                default:
                    // 클라이언트로부터 받은 버전 타입이 올바르지 않은 경우 예외를 발생시킨다.
                    throw new CustomException(INVALID_RELEASE_VERSION_TYPE);
            }
        }
        return newVersion;
    }

    /**
     * 버전을 내림차순으로 정렬한다. 이렇게 되면 가장 첫 번째 요소가 제일 큰/최신 버전이 된다.
     *
     * @param versions 버전 목록
     * @return String 내림차순으로 정렬된 버전 목록
     * @author seonwoo
     * @date 2023-07-14
     */
    private List<String> getLatestVersion(List<String> versions) {
        versions.sort(new Comparator<String>() {
            @Override
            public int compare(String v1, String v2) {
                String[] v1s = v1.split("\\.");
                String[] v2s = v2.split("\\.");

                int majorV1 = Integer.parseInt(v1s[0]);
                int minorV1 = Integer.parseInt(v1s[1]);
                int patchV1 = Integer.parseInt(v1s[2]);

                int majorV2 = Integer.parseInt(v2s[0]);
                int minorV2 = Integer.parseInt(v2s[1]);
                int patchV2 = Integer.parseInt(v2s[2]);

                if (majorV1 != majorV2) {
                    return Integer.compare(majorV2, majorV1);
                } else if (minorV1 != minorV2) {
                    return Integer.compare(minorV2, minorV1);
                } else {
                    return Integer.compare(patchV2, patchV1);
                }
            }
        });

        return versions;
    }

    /**
     * 릴리즈 버전을 오름차순으로 정렬한다.
     *
     * @param versions 버전 목록
     * @return String 오름차순으로 정렬된 버전 목록
     * @author seonwoo
     * @date 2023-07-14
     */
    private List<String> sortVersionByAsc(List<String> versions) {

        versions.sort((v1, v2) -> {
            String[] v1s = v1.split("\\.");
            String[] v2s = v2.split("\\.");

            int majorV1 = Integer.parseInt(v1s[0]);
            int minorV1 = Integer.parseInt(v1s[1]);
            int patchV1 = Integer.parseInt(v1s[2]);

            int majorV2 = Integer.parseInt(v2s[0]);
            int minorV2 = Integer.parseInt(v2s[1]);
            int patchV2 = Integer.parseInt(v2s[2]);

            if (majorV1 != majorV2) {
                return Integer.compare(majorV1, majorV2);
            } else if (minorV1 != minorV2) {
                return Integer.compare(minorV1, minorV2);
            } else {
                return Integer.compare(patchV1, patchV2);
            }
        });

        return versions;
    }

    /**
     * 릴리즈 노트 엔티티 객체를 생성한 후, 데이터베이스에 저장한다.
     *
     * @param project                 프로젝트 엔티티
     * @param releaseCreateRequestDto 릴리즈 노트 생성 요청 DTO
     * @param newVersion              생성할 릴리즈 버전
     * @return ReleaseNote 저장된 릴리즈 노트 엔티티
     * @author seonwoo
     * @date 2023-07-14
     */
    private ReleaseNote saveReleaseNote(Project project, ReleaseCreateRequestDTO releaseCreateRequestDto, String newVersion) {

        // 새로운 릴리즈 노트 생성
        ReleaseNote newReleaseNote = ReleaseNote.builder()
                .title(releaseCreateRequestDto.getTitle())
                .content(releaseCreateRequestDto.getContent())
                .summary(releaseCreateRequestDto.getSummary())
                .version(newVersion)
                .project(project)
                .coordX(releaseCreateRequestDto.getCoordX())
                .coordY(releaseCreateRequestDto.getCoordY())
                .build();

        // 릴리즈 노트 엔티티 저장
        return releaseRepository.save(newReleaseNote);
    }

    /**
     * 릴리즈 노트 엔티티를 업데이트(수정)한다.
     *
     * @param releaseNote             수정할 릴리즈 노트 엔티티
     * @param releaseUpdateRequestDto 릴리즈 노트 업데이트 요청 DTO
     * @param updatedVersion          수정된 릴리즈 버전
     * @return ReleaseNote 업데이트된 릴리즈 노트 엔티티
     * @author seonwoo
     * @date 2023-07-14
     */
    private ReleaseNote updateAndSaveReleaseNote(ReleaseNote releaseNote, ReleaseUpdateRequestDTO releaseUpdateRequestDto, String updatedVersion) {

        // 수정이 가능한 릴리즈 노트인지 유효성 검사를 진행한다.
        validateReleaseNoteUpdate(releaseNote, releaseUpdateRequestDto);

        // 먼저 연결된 이슈를 모두 해제한다.
        disconnectIssues(releaseNote);

        Date date = new Date();

        // 배포 상태에 따라 배포 날짜 값을 결정한다.
        if (releaseUpdateRequestDto.getDeployStatus().equals("DEPLOYED")) {
            date = new Date();
        }

        // 수정된 내용을 반영한다.
        releaseNote.updateReleaseNote(
                releaseUpdateRequestDto.getTitle(),
                releaseUpdateRequestDto.getContent(),
                releaseUpdateRequestDto.getSummary(),
                updatedVersion,
                date,
                ReleaseDeployStatus.valueOf(releaseUpdateRequestDto.getDeployStatus())
        );

        return releaseRepository.save(releaseNote);
    }

    /**
     * 릴리즈 노트 수정 및 배포가 가능한지 검사한다.
     *
     * @param releaseNote             수정할 릴리즈 노트 엔티티
     * @param releaseUpdateRequestDto 릴리즈 노트 업데이트 요청 DTO
     * @throws CustomException 수정이 불가능한 경우 예외 발생
     * @author seonwoo
     * @date 2023-07-14
     */
    private void validateReleaseNoteUpdate(ReleaseNote releaseNote, ReleaseUpdateRequestDTO releaseUpdateRequestDto) {
        // 릴리즈 노트가 수정 가능한 상태(PLANNING, DENIED)인지 검사한다.
        if (releaseNote.getDeployStatus().equals(ReleaseDeployStatus.DEPLOYED)) {
            // 만약 이미 DEPLOYED 된 릴리즈 노트인 경우 예외를 발생시킨다.
            throw new CustomException(FAILED_TO_UPDATE_DEPLOYED_RELEASE_VERSION);
        }

        // 만약 요청된 릴리즈 노트의 배포 상태가 DEPLOYED인 경우
        if (releaseUpdateRequestDto.getDeployStatus().equals("DEPLOYED")) {
            Project project = releaseNote.getProject();

            // FIXME: 나중에 QA 확인 필요 (Querydsl로 가져오면 2.0.0이 10.0.0보다 더 앞에 있게 된다.)
            List<ReleaseNote> releaseNotes = releaseRepository.findPreviousReleaseNotes(project, releaseUpdateRequestDto.getVersion());

            // 이전 릴리즈 노트 중 배포되지 않은 것이 있는지 검증하고, 아닌 경우 예외를 발생시킨다.
            releaseNotes
                    .forEach(r -> {
                        if (!r.getDeployStatus().equals(ReleaseDeployStatus.DEPLOYED)) {
                            throw new CustomException(FAILED_TO_UPDATE_RELEASE_DEPLOY_STATUS);
                        }
                    });
        }
    }

    /**
     * 기존의 이슈들에 대해 연결을 해제한다.
     *
     * @param releaseNote 연결을 해제할 릴리즈 노트 엔티티
     * @author seonwoo
     * @date 2023-07-23
     */
    private void disconnectIssues(ReleaseNote releaseNote) {
        releaseNote.getIssues().forEach(Issue::disconnectReleaseNote);
    }

    /**
     * 릴리즈 노트에 이슈를 연결시킨다.
     *
     * @param issueIds         연결할 이슈들의 식별 번호 목록
     * @param savedReleaseNote 연결할 릴리즈 노트 엔티티
     * @author seonwoo
     * @date 2023-07-14
     */
    private void connectIssues(List<Long> issueIds, ReleaseNote savedReleaseNote) {
        List<Issue> issues = getIssuesById(issueIds);

        issues.forEach(i -> {

            // 각각의 이슈들에 이미 연결된 릴리즈 노트가 없는지, 각 이슈들은 완료된 상태인지를 한 번 더 확인한다.
            if (i.getRelease() != null) {
                throw new CustomException(INVALID_ISSUE_WITH_COMPLETED);
            }

            if (i.getLifeCycle() != LifeCycle.DONE) {
                throw new CustomException(INVALID_ISSUE_WITH_NOT_DONE);
            }

            i.updateReleaseNote(savedReleaseNote);
            issueRepository.save(i);
        });
    }

    /**
     * 생성된 릴리즈 노트의 동의 여부에 대한 멤버의 목록을 생성한다.
     *
     * @param releaseNote 생성된 릴리즈 노트 엔티티
     * @author seonwoo
     * @date 2023-07-14
     */
    private void createReleaseApprovals(ReleaseNote releaseNote) {
        // 해당 릴리즈 노트가 들어있는 프로젝트의 멤버 목록을 가져온다.
        List<ProjectMember> members = projectMemberRepository.findByProject(releaseNote.getProject());

        // 릴리즈 노트의 식별 번호와 프로젝트 멤버 식별 번호를 가지고 동의 여부 테이블에 데이터를 생성한다.
        for (ProjectMember member : members) {
            ReleaseApproval releaseApproval = ReleaseApproval.builder()
                    .member(member)
                    .release(releaseNote)
                    .build();

            releaseApprovalRepository.save(releaseApproval);
        }
    }

    /**
     * 클라이언트로부터 전달받은 버전이 올바른지 검사한다.
     *
     * @param releaseNote 수정하려는 릴리즈 노트 엔티티
     * @param version     클라이언트로부터 전달받은 변경하려는 버전
     * @return String 검사가 완료된 올바른 버전
     * @author seonwoo
     * @date 2023-07-14
     */
    String updateReleaseVersion(ReleaseNote releaseNote, String version) {

        // 1. 만약 수정하려고 하는 릴리즈 노트의 원래 버전이 1.0.0인 경우 수정하지 못하도록 한다. 이 경우 릴리즈 노트 내용만 수정해야 한다.
        if (!Objects.equals(version, "1.0.0") && Objects.equals(releaseNote.getVersion(), "1.0.0")) {
            throw new CustomException(FAILED_TO_UPDATE_INITIAL_RELEASE_VERSION);
        }

        // 2. 중복된 버전이 있는지 확인한다. 중복된 버전이 존재할 경우 예외를 발생시킨다.
        if (releaseRepository.existsByProjectAndVersion(releaseNote.getProject(), releaseNote.getReleaseId(), version)) {
            throw new CustomException(DUPLICATED_RELEASE_VERSION);
        }

        // 3. 해당 프로젝트의 모든 릴리즈 버전을 가져온 후, 변경하려는 버전을 이어 붙인다.
        List<String> versions = releaseRepository.findByProjectAndNotInVersion(releaseNote.getProject(), releaseNote.getVersion()).stream().map(ReleaseNote::getVersion).collect(Collectors.toList());
        versions.add(version);

        // 4. 변경하려는 버전이 포함된 릴리즈 버전 배열을 오름차순으로 정렬한다.
        List<String> sortedVersions = sortVersionByAsc(versions);
        log.info("updateReleaseVersion/sortedVersions: {}", sortedVersions);

        // 5. 바꾸려는 버전 값이 올바른 버전 값인지를 확인한다.
        validateCorrectVersion(sortedVersions);

        return version;
    }

    /**
     * 클라이언트가 수정하고자 하는 버전이 올바른 버전인지 검증한다.
     *
     * @param versions 변경하려는 버전이 포함된 릴리즈 버전 배열
     * @author seonwoo
     * @date 2023-07-14
     */
    private void validateCorrectVersion(List<String> versions) {
        int[] majors = versions.stream().mapToInt(v -> Integer.parseInt(v.split("\\.")[0])).toArray();
        int[] minors = versions.stream().mapToInt(v -> Integer.parseInt(v.split("\\.")[1])).toArray();
        int[] patches = versions.stream().mapToInt(v -> Integer.parseInt(v.split("\\.")[2])).toArray();

        int majorStartIdx = 0;
        int minorStartIdx = 0;

        validateMajorVersion(majors, minors, patches, versions.size() - 1, majorStartIdx, minorStartIdx);
    }

    /**
     * Major(메이저) 버전 숫자에 대한 유효성 검사를 진행한다.
     *
     * @param majors        메이저 버전 숫자 배열
     * @param minors        마이너 버전 숫자 배열
     * @param patches       패치 버전 숫자 배열
     * @param end           배열의 마지막 인덱스
     * @param majorStartIdx 현재 메이저 버전 검사의 시작 인덱스
     * @param minorStartIdx 현재 마이너 버전 검사의 시작 인덱스
     * @author seonwoo
     * @date 2023-07-14
     */
    void validateMajorVersion(int[] majors, int[] minors, int[] patches, int end, int majorStartIdx, int minorStartIdx) {

        for (int i = 0; i < end; i++) {
            int currentMajor = majors[i];
            int nextMajor = majors[i + 1];

            // 만약 연속되는 두 개의 메이저 버전 숫자가 +-1이 아닌 경우 예외를 발생시킨다.
            if ((nextMajor - currentMajor > 1) || (nextMajor - currentMajor < 0)) {
                throw new CustomException(INVALID_RELEASE_VERSION);
            }

            // 만약 가장 큰 메이저 버전 숫자인 경우 해당 메이저 버전에 대한 모든 하위 버전의 유효성 검사를 진행한다.
            if (currentMajor == nextMajor && i + 1 == end) {
                validateMinorVersion(minors, patches, majorStartIdx, end, minorStartIdx);
                return;
            }

            // 만약 그 다음 번째 메이저 버전 숫자가 바뀌는 경우 넘어가기 전에 마이너 버전 숫자를 확인한다.
            if (nextMajor - currentMajor == 1) {
                validateMinorVersion(minors, patches, majorStartIdx, i, minorStartIdx);
                majorStartIdx = i + 1;
                minorStartIdx = i + 1;

                // 메이저 버전 숫자가 바뀌었을 때 마이너와 패치 버전 숫자는 모두 0이어야 한다.
                if (minors[majorStartIdx] != 0 || patches[majorStartIdx] != 0) {
                    throw new CustomException(INVALID_RELEASE_VERSION);
                }

            }
        }
    }

    /**
     * Minor(마이너) 버전 숫자에 대한 유효성 검사를 진행한다.
     *
     * @param minors        마이너 버전 숫자 배열
     * @param patches       패치 버전 숫자 배열
     * @param start         배열의 시작 인덱스
     * @param end           배열의 마지막 인덱스
     * @param minorStartIdx 현재 마이너 버전 검사의 시작 인덱스
     * @author seonwoo
     * @date 2023-07-14
     */
    private void validateMinorVersion(int[] minors, int[] patches, int start, int end, int minorStartIdx) {

        if (end - start == 0) {
            return;
        }

        for (int i = start; i < end; i++) {
            int currentMinor = minors[i];
            int nextMinor = minors[i + 1];

            // 만약 연속되는 두 개의 마이너 버전 숫자가 +-1이 아닌 경우 예외를 발생시킨다.
            if ((nextMinor - currentMinor > 1) || (nextMinor - currentMinor < 0)) {
                throw new CustomException(INVALID_RELEASE_VERSION);
            }

            // 만약 가장 큰 마이너 버전 숫자인 경우 해당 마이너 버전에 대한 모든 하위 버전의 유효성 검사를 진행한다.
            if (currentMinor == nextMinor && i + 1 == end) {
                validatePatchVersion(patches, minorStartIdx, end);
                return;
            }

            // 만약 그 다음 번째 마이너 버전 숫자가 바뀌는 경우 넘어가기 전에 패치 버전 숫자를 확인한다.
            if (nextMinor - currentMinor == 1) {
                validatePatchVersion(patches, minorStartIdx, i);
                minorStartIdx = i + 1;

                // 마이너 버전 숫자가 바뀌었을 때 패치 버전 숫자는 0이어야 한다.
                if (patches[minorStartIdx] != 0) {
                    throw new CustomException(INVALID_RELEASE_VERSION);
                }
            }
        }
    }

    /**
     * Patch(패치) 버전 숫자에 대한 유효성 검사를 진행한다.
     *
     * @param patches 패치 버전 숫자 배열
     * @param start   배열의 시작 인덱스
     * @param end     배열의 마지막 인덱스
     * @author seonwoo
     * @date 2023-07-14
     */
    private void validatePatchVersion(int[] patches, int start, int end) {

        if (end - start == 0) {
            return;
        }

        for (int i = start; i < end; i++) {
            int currentPatch = patches[i];
            int nextPatch = patches[i + 1];

            // 만약 연속되는 두 개의 메이저 버전 숫자가 +-1이 아닌 경우 예외를 발생시킨다.
            if ((nextPatch - currentPatch > 1) || (nextPatch - currentPatch < 0)) {
                throw new CustomException(INVALID_RELEASE_VERSION);
            }
        }
    }

    /**
     * 릴리즈 노트 삭제가 가능한지 유효성 검사를 진행한다.
     *
     * @param releaseNote 삭제하려는 릴리즈 노트 객체
     * @author seonwoo
     * @date 2023-07-23
     */
    private void validateReleaseNoteDelete(ReleaseNote releaseNote) {

        // 해당 릴리즈 노트가 삭제 가능한 상태(PLANNING, DENIED)인지 검사한다.
        if (releaseNote.getDeployStatus().equals(ReleaseDeployStatus.DEPLOYED)) {
            // 만약 이미 DEPLOYED 된 릴리즈 노트인 경우 예외를 발생시킨다.
            throw new CustomException(FAILED_TO_DELETE_DEPLOYED_RELEASE_NOTE);
        }

        // 해당 릴리즈 노트의 이후 버전 중 배포된 것이 있다면 예외를 발생시킨다.
        // 1. 릴리즈 노트를 릴리즈 버전 기준 오름차순으로 정렬한다.
        List<ReleaseNote> releaseNotes = releaseRepository.findAllByProject(releaseNote.getProject());
        List<ReleaseNote> sortedReleaseNotes = sortReleaseNoteByAsc(releaseNotes);

        // 2. 해당 릴리즈 노트가 가장 최신의 버전이라면 유효성 검사를 통과한다.
        int currentIdx = sortedReleaseNotes.indexOf(releaseNote);
        if (currentIdx == sortedReleaseNotes.size() - 1) {
            return;
        }

        // 3. 현재 릴리즈 노트의 이후 버전 중 배포된 릴리즈 노트가 있는지 확인하고, 있다면 예외를 발생시킨다.
        for (int i = currentIdx + 1; i < sortedReleaseNotes.size(); i++) {

            if (sortedReleaseNotes.get(i).getDeployStatus() == ReleaseDeployStatus.DEPLOYED) {
                throw new CustomException(EXISTS_DEPLOYED_RELEASE_NOTE_AFTER_THIS);
            }

        }

        // 이후 릴리즈가 배포되지 않은 상황에서 릴리즈 노트의 각 자릿수 버전(Major, Minor, Patch) 끝 숫자만 삭제할 수 있다.
        // 1. 현재 릴리즈 노트의 버전의 각 숫자(Major, Minor, Patch)와 다음 버전의 각 숫자를 가져온다.
        String currentVersion = releaseNote.getVersion();
        int currentMinor = Integer.parseInt(currentVersion.split("\\.")[1]);
        int currentPatch = Integer.parseInt(currentVersion.split("\\.")[2]);

        String nextVersion = releaseNotes.get(currentIdx + 1).getVersion();
        int nextMinor = Integer.parseInt(nextVersion.split("\\.")[1]);
        int nextPatch = Integer.parseInt(nextVersion.split("\\.")[2]);

        // 3. 현재 버전과 다음 버전의 바뀌는 숫자가 같은 자리인 경우 예외를 발생시킨다.
        if (currentMinor == 0 && currentPatch == 0) {
            throw new CustomException(FAILED_TO_DELETE_RELEASE_NOTE);
        }

        if (currentPatch == 0 && nextPatch == 0) {

            if (currentMinor < nextMinor) {
                throw new CustomException(FAILED_TO_DELETE_RELEASE_NOTE);
            }

        } else {

            if (currentPatch < nextPatch) {
                throw new CustomException(FAILED_TO_DELETE_RELEASE_NOTE);
            }

        }
    }

    /**
     * 릴리즈 노트를 버전을 기준으로 오름차순으로 배열한다.
     *
     * @param releaseNotes 정렬할 릴리즈 노트 목록
     * @return ReleaseNote 버전을 기준으로 오름차순으로 정렬된 릴리즈 노트 목록
     * @author seonwoo
     * @date 2023-07-23
     */
    private List<ReleaseNote> sortReleaseNoteByAsc(List<ReleaseNote> releaseNotes) {

        releaseNotes.sort((r1, r2) -> {
            String[] v1s = r1.getVersion().split("\\.");
            String[] v2s = r2.getVersion().split("\\.");

            int majorV1 = Integer.parseInt(v1s[0]);
            int minorV1 = Integer.parseInt(v1s[1]);
            int patchV1 = Integer.parseInt(v1s[2]);

            int majorV2 = Integer.parseInt(v2s[0]);
            int minorV2 = Integer.parseInt(v2s[1]);
            int patchV2 = Integer.parseInt(v2s[2]);

            if (majorV1 != majorV2) {
                return Integer.compare(majorV1, majorV2);
            } else if (minorV1 != minorV2) {
                return Integer.compare(minorV1, minorV2);
            } else {
                return Integer.compare(patchV1, patchV2);
            }
        });

        return releaseNotes;
    }

    /**
     * 릴리즈 노트 배포 동의 여부를 선택할 수 있는 건지 확인한다.
     *
     * @param member      배포 동의를 선택하려는 프로젝트 멤버
     * @param releaseNote 배포 동의를 선택하려는 릴리즈 노트
     * @throws CustomException 배포 동의를 선택할 수 없는 경우 예외 발생
     * @author seonwoo
     * @date 2023-07-10
     */
    private void validateReleaseNoteApproval(ProjectMember member, ReleaseNote releaseNote) {

        // 만약 릴리즈 노트가 배포된 상태(DEPLOYED)라면 배포 동의를 체크할 수 없다.
        if (releaseNote.getDeployStatus().equals(ReleaseDeployStatus.DEPLOYED)) {
            throw new CustomException(FAILED_TO_APPROVE_RELEASE_NOTE);
        }

        // 만약 릴리즈 노트가 멤버가 속한 프로젝트의 릴리즈 노트가 아닌 경우 예외를 발생시킨다.
        if (!releaseNote.getProject().equals(member.getProject())) {
            throw new CustomException(UNAUTHORIZED_RELEASE_NOTE);
        }
    }

    /**
     * 릴리즈 노트의 배포 동의 여부를 업데이트한다.
     *
     * @param member      배포 동의를 업데이트하는 프로젝트 멤버
     * @param releaseNote 배포 동의를 업데이트하는 릴리즈 노트
     * @param approval    배포 동의 여부 ('Y': 동의, 'N': 거부)
     * @throws CustomException 해당 릴리즈 노트의 배포 동의가 존재하지 않는 경우 예외 발생
     * @author seonwoo
     * @date 2023-07-10
     */
    private void updateReleaseNoteApproval(ProjectMember member, ReleaseNote releaseNote, char approval) {
        ReleaseApproval releaseApproval = releaseApprovalRepository.findByMemberAndRelease(member, releaseNote).orElseThrow(() -> new CustomException(NOT_EXISTS_RELEASE_APPROVAL));
        releaseApproval.updateApproval(approval);
        releaseApprovalRepository.save(releaseApproval);

        if (member.getPosition() == 'L' && approval == 'Y') {
            // 저장한 후 배포 동의 상태 값을 전달한 사용자가 관리자이고, 관리자가 동의를 선택한 경우 최종적으로 릴리즈 노트를 배포한다.
            // 1. 모든 멤버의 동의 여부가 완료되었는지 확인한다.
            checkIfApproveAllMembers(releaseNote);

            // 2. 만약 이전에 배포되지 않은 버전이 있다면 예외를 발생시킨다.
            checkIfNotDeployedReleaseNotes(releaseNote);

            // 3. 모든 조건을 만족했다면 릴리즈 노트의 배포 상태 값을 배포 완료로 변경한다.
            releaseNote.updateDeployStatus(ReleaseDeployStatus.DEPLOYED);
            releaseRepository.save(releaseNote);

            // 4. 릴리즈 배포 상태 알림을 보낸다.
            notifyReleaseNote(releaseNote.getProject(), releaseNote, "릴리즈 노트가 배포되었습니다.");
        }

        if (member.getPosition() == 'L' && approval == 'N') {
            // 배포 동의 상태 값을 전달한 사용자가 관리자이고, 관리자가 배포 거부를 선택한 경우
            // 릴리즈 노트 배포 거부 알림을 프로젝트 멤버 모두에게 준다.
            notifyReleaseNote(releaseNote.getProject(), releaseNote, "릴리즈 노트의 배포가 거부되었습니다.");
        }

        if (member.getPosition() == 'M' && approval == 'Y') {
            // 배포 동의 상태 값을 전달한 사용자가 멤버이고, 멤버가 동의를 선택한 경우
            // 릴리즈 노트 배포 결정 알림을 프로젝트 PM에게 준다.
            notifyReleaseNoteApprovalToManager(releaseNote.getProject(), releaseNote);
        }
    }

    /**
     * 이전에 배포되지 않은 버전이 있는지 확인한다.
     *
     * @param releaseNote 확인할 릴리즈 노트
     * @throws CustomException 이전에 배포되지 않은 버전이 있을 경우 예외 발생
     * @author seonwoo
     * @date 2023-07-10
     */
    private void checkIfNotDeployedReleaseNotes(ReleaseNote releaseNote) {
        // 먼저 릴리즈 버전을 기준으로 오름차순 정렬한다.
        List<ReleaseNote> releaseNotes = releaseRepository.findAllByProject(releaseNote.getProject());
        List<ReleaseNote> sortedReleaseNotes = sortReleaseNoteByAsc(releaseNotes);

        // 현재 릴리즈 노트의 인덱스를 찾는다.
        int currentIdx = sortedReleaseNotes.indexOf(releaseNote);
        if (currentIdx == 0) {
            // 만약 해당 릴리즈 노트 하나밖에 없다면 유효성 검사를 통과할 수 있다.
            return;
        }

        // 현재 릴리즈 노트의 이전 버전 중 배포되지 않은 릴리즈 노트가 있는지 확인하고, 있다면 예외를 발생시킨다.
        for (int i = 0; i < currentIdx; i++) {

            if (sortedReleaseNotes.get(i).getDeployStatus() != ReleaseDeployStatus.DEPLOYED) {
                throw new CustomException(EXISTS_NOT_DEPLOYED_RELEASE_NOTE_BEFORE_THIS);
            }
        }
    }

    /**
     * 모든 프로젝트 멤버가 배포를 동의했는지 확인한다.
     *
     * @param releaseNote 확인할 릴리즈 노트
     * @throws CustomException 한 사람이라도 배포를 동의하지 않은 경우 예외 발생
     * @author seonwoo
     * @date 2023-07-10
     */
    private void checkIfApproveAllMembers(ReleaseNote releaseNote) {
        // ReleaseApproval 테이블을 가져와서 모든 멤버의 배포 동의 값이 'Y'인지 확인한다.
        List<ReleaseApproval> approvals = releaseApprovalRepository.findAllByRelease(releaseNote);
        for (ReleaseApproval approval : approvals) {

            if (approval.getApproval() != 'Y') {
                // 만약 한 사람이라도 배포를 동의하지 않았다면 예외를 발생시킨다.
                throw new CustomException(EXISTS_DISAPPROVED_MEMBER);
            }

        }
    }

    /**
     * 해당 릴리즈 노트에 대한 프로젝트 멤버들의 업데이트된 배포 동의 여부 목록을 반환한다.
     *
     * @param releaseNote 조회할 릴리즈 노트
     * @return ReleaseApprovalsResponseDTO 프로젝트 멤버들의 배포 동의 여부 목록
     * @throws CustomException 릴리즈 노트에 대한 배포 동의 정보를 가져오지 못한 경우 예외 발생
     * @author seonwoo
     * @date 2023-07-10
     */
    private List<ReleaseApprovalsResponseDTO> getReleaseApprovals(ReleaseNote releaseNote) {
        List<ReleaseApproval> releaseApprovals = releaseApprovalRepository.findAllByRelease(releaseNote);

        if (releaseApprovals == null || releaseApprovals.size() == 0) {
            throw new CustomException(FAILED_TO_GET_RELEASE_APPROVALS);
        }

        return releaseApprovals.stream()
                .map(ReleaseMapper.INSTANCE::toReleaseApprovalsResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 릴리즈 노트 좌표를 클라이언트에서 새로 받은 값으로 업데이트한다.
     *
     * @param datas 클라이언트에서 받은 릴리즈 노트 좌표 데이터 목록
     * @author seonwoo
     * @date 2023-07-10
     */
    private void updateCoordinates(List<CoordinateDataDTO> datas) {
        for (CoordinateDataDTO data : datas) {

            // 해당 릴리즈 식별 번호에 대항하는 릴리즈 노트 엔티티를 가져온다.
            ReleaseNote releaseNote = getReleaseNoteById(data.getReleaseId());

            // 해당 릴리즈 노트의 이전 좌표 값과 새로 전달받은 좌표 값이 같은 경우 업데이트를 생략한다.
            Double prevX = releaseNote.getCoordX();
            Double prevY = releaseNote.getCoordY();
            Double newX = data.getCoordX();
            Double newY = data.getCoordY();

            if (!Objects.equals(prevX, newX) && !Objects.equals(prevY, newY)) {
                // x, y 좌표 모두 다른 경우 업데이트 한다.
                releaseNote.updateCoordinates(newX, newY);

            } else if (!Objects.equals(prevX, newX)) {
                // x 좌표가 다른 경우 업데이트한다.
                releaseNote.updateCoordX(newX);

            } else if (!Objects.equals(prevY, newY)) {
                // y 좌표가 다른 경우 업데이트한다.
                releaseNote.updateCoordY(newY);

            } else {
                // 만약 변경된 값이 없는 경우 업데이트를 하지 않고 넘어간다.
                continue;
            }

            releaseRepository.save(releaseNote);
        }
    }

    /**
     * 릴리즈 노트 의견을 저장한다.
     *
     * @param releaseNote                    릴리즈 노트 엔티티
     * @param member                         의견 작성 멤버
     * @param releaseOpinionCreateRequestDto 릴리즈 노트 의견 생성 요청 DTO
     * @author seonwoo
     * @date 2023-07-10
     */
    private void saveReleaseOpinion(ReleaseNote releaseNote, ProjectMember member, ReleaseOpinionCreateRequestDTO releaseOpinionCreateRequestDto) {
        ReleaseOpinion releaseOpinion = ReleaseOpinion.builder()
                .opinion(releaseOpinionCreateRequestDto.getOpinion())
                .release(releaseNote)
                .member(member)
                .build();

        releaseOpinionRepository.save(releaseOpinion);
    }

    /**
     * 릴리즈 노트 의견 조회 결과를 DTO 리스트로 변환한다.
     *
     * @param releaseOpinions 릴리즈 노트 의견 엔티티 리스트
     * @return ReleaseOpinionsResponseDTO 릴리즈 노트 의견 조회 결과 DTO 리스트
     * @author seonwoo
     * @date 2023-07-10
     */
    private List<ReleaseOpinionsResponseDTO> getReleaseOpinionsResponseDto(List<ReleaseOpinion> releaseOpinions) {
        return releaseOpinions.stream()
                .map(ReleaseMapper.INSTANCE::toReleaseOpinionsResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 이슈들을 태그별로 그룹화하는 메서드
     *
     * @param issues 이슈 엔티티 리스트
     * @return Map<String, List < GetIssueTitleDataDTO>> 태그별로 그룹화된 이슈 리스트 맵
     * @author chaeanna
     * @date 2023-07-22
     */
    private Map<String, List<GetIssueTitleDataDTO>> groupIssuesByTag(List<Issue> issues) {
        Map<String, List<GetIssueTitleDataDTO>> tagToIssueMap = new HashMap<>();
        for (Issue issue : issues) {
            // 이슈의 태그 가져오기
            String tag = String.valueOf(issue.getTag());
            // GetIssueTitle 객체 생성
            GetIssueTitleDataDTO issueTitle = GetIssueTitleDataDTO.builder()
                    .issueId(issue.getIssueId())
                    .title(issue.getTitle())
                    .summary(issue.getSummary())
                    .build();

            // 태그별로 이슈들을 그룹화
            tagToIssueMap.computeIfAbsent(tag, k -> new ArrayList<>()).add(issueTitle);
        }
        return tagToIssueMap;
    }

    /**
     * ReleaseDocsRes 객체를 생성하는 메서드
     *
     * @param note          릴리즈 노트 엔티티
     * @param tagToIssueMap 태그별로 그룹화된 이슈 리스트 맵
     * @return ReleaseDocsResponseDTO 릴리즈 문서 정보 DTO
     * @author chaeanna
     * @date 2023-07-22
     */
    private ReleaseDocsResponseDTO buildReleaseDocsRes(ReleaseNote note, Map<String, List<GetIssueTitleDataDTO>> tagToIssueMap) {
        // 태그별로 그룹화된 이슈들을 GetTags 리스트로 변환하여 저장
        List<GetTagsDataDTO> tagsList = tagToIssueMap.entrySet().stream()
                .map(entry -> GetTagsDataDTO.builder()
                        .tag(entry.getKey())
                        .titleList(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        // ReleaseDocsRes 객체 생성 및 반환
        return ReleaseDocsResponseDTO.builder()
                .releaseId(note.getReleaseId())
                .releaseVersion(note.getVersion())
                .releaseTitle(note.getTitle())
                .releaseContent(note.getContent())
                .tagsList(tagsList)
                .build();
    }

    /**
     * 프로젝트 구성원 정보
     *
     * @param email     사용자 이메일
     * @param projectId 프로젝트 식별 번호
     * @return ProjectMember 프로젝트 멤버 엔티티
     * @throws CustomException 주어진 이메일에 해당하는 사용자가 존재하지 않거나, 주어진 projectId에 해당하는 프로젝트 멤버가 존재하지 않을 경우 예외 발생
     * @author chaeanna
     * @date 2023-07-22
     */
    private ProjectMember getProjectMemberByEmailAndProjectId(String email, Long projectId) {
        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(NOT_EXISTS_USER));
        // 프로젝트 조회
        Project project = getProjectById(projectId);
        // 사용자와 프로젝트로 구성원 정보 조회
        return projectMemberRepository.findByUserAndProject(user, project).orElseThrow(() -> new CustomException(NOT_EXISTS_PROJECT_MEMBER));
    }

    /**
     * 이슈의 summary 업데이트
     *
     * @param issueId 이슈 식별 번호
     * @param req     업데이트 요청 정보
     * @throws CustomException 주어진 issueId에 해당하는 이슈가 존재하지 않을 경우 예외 발생
     * @author chaeanna
     * @date 2023-07-22
     */
    private void updateIssueSummary(Long issueId, UpdateReleaseDocsRequestDTO req) {
        // id로 이슈 조회
        Issue issue = issueRepository.findById(issueId).orElseThrow(() -> new CustomException(NOT_EXISTS_ISSUE));
        // 요청에 따라 이슈의 요약 업데이트 후 저장
        issue.updateSummary(req);
        issueRepository.save(issue);
    }

    /**
     * 사용자가 프로젝트의 PM인지 확인한다.
     *
     * @param email   사용자 이메일
     * @param project 프로젝트 정보
     * @throws CustomException 주어진 이메일에 해당하는 사용자가 존재하지 않거나, 프로젝트 멤버가 아닌 경우, 또는 프로젝트의 관리자가 아닌 경우 예외 발생
     * @author seonwoo
     * @date 2023-07-14
     */
    private void isProjectManager(String email, Project project) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(NOT_EXISTS_USER));
        ProjectMember member = projectMemberRepository.findByUserAndProject(user, project).orElseThrow(() -> new CustomException(NOT_EXISTS_PROJECT_MEMBER));
        ;

        if (member.getPosition() != 'L') {
            // 프로젝트의 관리자가 아닌 경우 예외를 발생시킨다.
            throw new CustomException(NOT_PROJECT_MANAGER);
        }
    }

    /**
     * 사용자가 프로젝트의 멤버인지 확인하고, 맞다면 가져온다.
     *
     * @param email   사용자 이메일
     * @param project 프로젝트 정보
     * @return ProjectMember 프로젝트 멤버 엔티티
     * @throws CustomException 주어진 이메일에 해당하는 사용자가 존재하지 않거나, 프로젝트 멤버가 아닌 경우 예외 발생
     * @author seonwoo
     * @date 2023-07-12
     */
    private ProjectMember getProjectMember(String email, Project project) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(NOT_EXISTS_USER));
        ProjectMember member = projectMemberRepository.findByUserAndProject(user, project).orElseThrow(() -> new CustomException(NOT_EXISTS_PROJECT_MEMBER));
        ;

        return member;
    }

    /**
     * 릴리즈 노트 의견을 삭제할 수 있도록 상태 값을 변경한다.
     *
     * @param releaseNote 릴리즈 노트 정보
     * @param member      사용자의 프로젝트 멤버 정보
     * @return ReleaseOpinionsDataDTO 변경된 릴리즈 노트 의견 DTO 리스트
     * @author seonwoo
     * @date 2023-07-12
     */
    private List<ReleaseOpinionsDataDTO> updateToAllowDeleteOpinion(ReleaseNote releaseNote, ProjectMember member) {
        // DTO를 받아올 때 릴리즈 노트 의견을 작성한 사용자의 탈퇴 여부를 확인하고, 만약 탈퇴한 경우 memberId를 0으로 바꾼다.
        List<ReleaseOpinionsDataDTO> opinionDtos = releaseOpinionRepository.getDtosByReleaseNote(releaseNote);

        for (ReleaseOpinionsDataDTO opinionDto : opinionDtos) {
            // 만약 요청한 사용자가 작성한 의견인 경우 삭제가 가능하도록 상태 값을 변경한다.
            opinionDto.updateDeleteYN(opinionDto.getMemberId().equals(member.getMemberId()) ? 'Y' : 'N');
        }

        return opinionDtos;
    }

    /**
     * ReleaseInfoResponseDto로 변환
     *
     * @param releaseNote 릴리즈 노트 정보
     * @param opinions    릴리즈 노트에 작성된 의견들의 정보 리스트
     * @return ReleaseInfoResponseDTO 릴리즈 정보 DTO
     * @author seonwoo
     * @date 2023-07-12
     */
    private ReleaseInfoResponseDTO createReleaseInfoResponseDto(ReleaseNote releaseNote, List<ReleaseOpinionsDataDTO> opinions) {
        return ReleaseMapper.INSTANCE.toReleaseInfoResponseDto(releaseNote, opinions);
    }

    /**
     * ReleaseOpinionsResponseDto 배열 생성 후 변환
     *
     * @param releaseNote 릴리즈 노트 정보
     * @return ReleaseOpinionsResponseDTO 릴리즈 의견 정보 DTO
     * @author seonwoo
     * @date 2023-07-26
     */
    private List<ReleaseOpinionsResponseDTO> createReleaseOpinionsResponseDto(ReleaseNote releaseNote, Long memberId) {
        List<ReleaseOpinion> opinions = releaseOpinionRepository.findAllByRelease(releaseNote);

        return opinions.stream()
                .map(opinion -> {
                    ReleaseOpinionsResponseDTO resDTO = ReleaseMapper.INSTANCE.toReleaseOpinionsResponseDto(opinion);
                    resDTO.updateDeleteYN(memberId != null && opinion.getMember().getMemberId().equals(memberId) ? 'Y' : 'N');
                    return resDTO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 릴리즈 노트 의견 작성자와 의견 삭제 요청을 보낸 멤버가 같은 사람인지 확인한다.
     *
     * @param opinion 릴리즈 노트 의견 정보
     * @param member  의견 삭제 요청을 보낸 프로젝트 멤버 정보
     * @author seonwoo
     * @date 2023-07-26
     */
    private void validateOpinionAndMember(ReleaseOpinion opinion, ProjectMember member) {
        if (opinion.getMember() != member) {
            // 만약 작성자가 같은 사람이 아닌 경우 예외를 발생시킨다.
            throw new CustomException(UNAUTHORIZED_TO_DELETE_RELEASE_OPINION);
        }
    }

    /**
     * 이벤트 리스너를 이용하여 트랜잭션 처리 후 릴리즈 노트 생성 알림을 전달한다.
     *
     * @param project     프로젝트
     * @param releaseNote 릴리즈 노트
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    private void notifyReleaseNote(Project project, ReleaseNote releaseNote, String alarmMessage) {
        // 알림 메시지를 정의한다.
        ReleaseNoteMessageDto message = ReleaseNoteMessageDto.builder()
                .type("Release Note")
                .projectId(project.getProjectId())
                .projectName(project.getTitle())
                .projectImg(project.getImg())
                .message(alarmMessage)
                .date(Date.from(releaseNote.getCreatedDate().atZone(ZoneId.systemDefault()).toInstant()))
                .releaseNoteId(releaseNote.getReleaseId())
                .build();

        // 알림 메시지를 보낼 대상 목록을 가져온다.
        List<String> consumers = projectMemberRepository.findByProject(project).stream()
                .map(m -> m.getUser().getEmail())
                .collect(Collectors.toList());

        // 이벤트 리스너를 호출하여 릴리즈 노트 생성 트랜잭션이 완료된 후 호출하도록 한다.
        notificationEventPublisher.notifyReleaseNote(ReleaseNoteMessageEvent.toNotifyOneReleaseNote(message, consumers));
    }

    /**
     * 릴리즈 노트 배포 결정 알림 이벤트를 호출한다.
     *
     * @param project     프로젝트
     * @param releaseNote 릴리즈 노트
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    private void notifyReleaseNoteApprovalToManager(Project project, ReleaseNote releaseNote) {
        // 알림 메시지를 정의한다.
        ReleaseNoteMessageDto message = ReleaseNoteMessageDto.builder()
                .type("Release Note")
                .projectId(project.getProjectId())
                .projectName(project.getTitle())
                .projectImg(project.getImg())
                .message("릴리즈 노트 배포를 결정해 주세요.")
                .date(Date.from(releaseNote.getCreatedDate().atZone(ZoneId.systemDefault()).toInstant()))
                .releaseNoteId(releaseNote.getReleaseId())
                .build();

        // 프로젝트 PM의 이메일을 가져온다.
        ProjectMember member = projectRepository.getProjectMemberPostionPM(project.getProjectId());

        List<String> consumers = new ArrayList<>();
        consumers.add(member.getUser().getEmail());

        // 이벤트 리스너를 호출하여 릴리즈 노트 생성 트랜잭션이 완료된 후 호출하도록 한다.
        notificationEventPublisher.notifyReleaseNote(ReleaseNoteMessageEvent.toNotifyOneReleaseNote(message, consumers));
    }
}
