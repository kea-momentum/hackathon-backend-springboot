package com.momentum.releaser.domain.release.application;

import com.momentum.releaser.domain.issue.dao.IssueRepository;
import com.momentum.releaser.domain.issue.domain.Issue;
import com.momentum.releaser.domain.issue.domain.IssueNum;
import com.momentum.releaser.domain.issue.domain.LifeCycle;
import com.momentum.releaser.domain.issue.domain.Tag;
import com.momentum.releaser.domain.notification.event.NotificationEventPublisher;
import com.momentum.releaser.domain.project.dao.ProjectMemberRepository;
import com.momentum.releaser.domain.project.dao.ProjectRepository;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.release.dao.approval.ReleaseApprovalRepository;
import com.momentum.releaser.domain.release.dao.opinion.ReleaseOpinionRepository;
import com.momentum.releaser.domain.release.dao.release.ReleaseRepository;
import com.momentum.releaser.domain.release.domain.ReleaseApproval;
import com.momentum.releaser.domain.release.domain.ReleaseEnum;
import com.momentum.releaser.domain.release.domain.ReleaseEnum.ReleaseDeployStatus;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.domain.release.domain.ReleaseOpinion;
import com.momentum.releaser.domain.release.dto.ReleaseRequestDto;
import com.momentum.releaser.domain.release.dto.ReleaseRequestDto.ReleaseApprovalRequestDTO;
import com.momentum.releaser.domain.release.dto.ReleaseRequestDto.ReleaseCreateRequestDTO;
import com.momentum.releaser.domain.release.dto.ReleaseRequestDto.ReleaseUpdateRequestDTO;
import com.momentum.releaser.domain.release.dto.ReleaseRequestDto.UpdateReleaseDocsRequestDTO;
import com.momentum.releaser.domain.release.dto.ReleaseResponseDto;
import com.momentum.releaser.domain.release.dto.ReleaseResponseDto.ReleaseCreateAndUpdateResponseDTO;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.parameters.P;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.momentum.releaser.global.config.BaseResponseStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReleaseServiceImplTest {

    private ReleaseServiceImpl releaseService;
    private UserRepository userRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository projectMemberRepository;
    private ReleaseRepository releaseRepository;
    private ReleaseOpinionRepository releaseOpinionRepository;
    private ReleaseApprovalRepository releaseApprovalRepository;
    private IssueRepository issueRepository;
    private NotificationEventPublisher notificationEventPublisher;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        projectRepository = mock(ProjectRepository.class);
        projectMemberRepository = mock(ProjectMemberRepository.class);
        releaseRepository = mock(ReleaseRepository.class);
        releaseOpinionRepository = mock(ReleaseOpinionRepository.class);
        releaseApprovalRepository = mock(ReleaseApprovalRepository.class);
        issueRepository = mock(IssueRepository.class);
        notificationEventPublisher = mock(NotificationEventPublisher.class);
        releaseService = new ReleaseServiceImpl(
                userRepository, projectRepository, projectMemberRepository, releaseRepository, releaseOpinionRepository, releaseApprovalRepository, issueRepository, notificationEventPublisher
        );
    }

//    @Test
//    @DisplayName("5.2 릴리즈 노트 생성 - pm만 릴리즈 노트 생성 가능")
//    void testAddReleaseNote_ByPM() {
//        // 테스트를 위한 mock 릴리즈 노트 생성 정보
//        Long mockProjectId = 1L;
//        String mockUserEmail = "testLeader@releaser.com";
//
//        Project mockProject = new Project(
//                mockProjectId, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
//        );
//        User mockLeaderUser = new User(
//                "testLeaderUserName", mockUserEmail, null, 'Y'
//        );
//        User mockMemberUser = new User(
//                "testMemberUserName", "testMember@releaser.com", null, 'Y'
//        );
//        ProjectMember mockLeaderMember = new ProjectMember(
//                1L, 'L', 'Y', mockLeaderUser, mockProject
//        );
//        ProjectMember mockMember = new ProjectMember(
//                2L, 'M', 'Y', mockMemberUser, mockProject
//        );
//        Issue mockIssue1 = new Issue(
//                1L, "Test Issue Title", "Test Issue Content", null,
//                Tag.NEW, Date.valueOf("2023-08-02"), LifeCycle.DONE, 'N', 'Y',
//                mockProject, mockLeaderMember, null, null
//        );
//        Issue mockIssue2 = new Issue(
//                2L, "Test Issue Title", "Test Issue Content", null,
//                Tag.NEW, Date.valueOf("2023-08-02"), LifeCycle.DONE, 'N', 'Y',
//                mockProject, mockLeaderMember, null, null
//        );
//        ReleaseCreateRequestDTO mockReleaseCreateRequestDto = new ReleaseCreateRequestDTO(
//                "Test Release", "MAJOR", "Test Release Content", "Test Release Summary",
//                50.0, 50.0, List.of(1L, 2L)
//        );
//        ReleaseNote mockSavedReleaseNote = new ReleaseNote(
//                1L, "save Release Title", "save Release Content", "save Release Summary", "1.0.0",
//                Date.valueOf("2023-08-02"), ReleaseEnum.ReleaseDeployStatus.PLANNING, mockProject, 50.0, 50.0
//        );
//        // 해당 프로젝트에 속해있는 멤버 리스트
//        List<ProjectMember> mockMemberList = List.of(mockLeaderMember, mockMember);
//
//        // projectRepository.findById() 메서드가 mockProject를 반환하도록 설정
//        when(projectRepository.findById(mockProjectId)).thenReturn(Optional.of(mockProject));
//
//        // userRepository.findByEmail() 메서드가 mockLeaderUser를 반환하도록 설정 (접근한 유저의 정보 조회)
//        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockLeaderUser));
//
//        // projectMemberRepository.findByUserAndProject() 메서드가 mockLeaderMember를 반환하도록 설정 (접근 유저의 해당 프로젝트의 멤버 정보 조회)
//        when(projectMemberRepository.findByUserAndProject(mockLeaderUser, mockProject)).thenReturn(Optional.of(mockLeaderMember));
//
//        // issueRepository.findById() 메서드가 mockIssue1, mockIssue2를 반환하도록 설정
//        when(issueRepository.findById(mockIssue1.getIssueId())).thenReturn(Optional.of(mockIssue1));
//        when(issueRepository.findById(mockIssue2.getIssueId())).thenReturn(Optional.of(mockIssue2));
//
//        // releaseRepository.save() 메서드가 mockSavedReleaseNote를 반환하도록 설정 (릴리즈 노트 생성)
//        when(releaseRepository.save(any(ReleaseNote.class))).thenReturn(mockSavedReleaseNote);
//
//        // projectMemberRepository.findByProject() 메서드가 mockMemberList를 반환하도록 설정 (해당 프로젝트의 멤버 리스트)
//        when(projectMemberRepository.findByProject(mockProject)).thenReturn(mockMemberList);
//
//        // 릴리즈 노트 생성 서비스 호출
//        ReleaseCreateAndUpdateResponseDTO result = releaseService.addReleaseNote(mockUserEmail, mockProjectId, mockReleaseCreateRequestDto);
//
//        // 결과 검증
//        assertNotNull(result);
//        assertEquals(mockSavedReleaseNote.getReleaseId(), result.getReleaseId());
//        assertEquals(mockSavedReleaseNote.getVersion(), result.getVersion());
//        assertEquals(mockSavedReleaseNote.getSummary(), result.getSummary());
//        assertEquals(mockSavedReleaseNote.getCoordX(), result.getCoordX());
//        assertEquals(mockSavedReleaseNote.getCoordY(), result.getCoordY());
//
//        // 각 메서드가 호출 됐는지 확인
//        verify(projectRepository, times(1)).findById(mockProjectId);
//        verify(userRepository, times(1)).findByEmail(mockUserEmail);
//        verify(projectMemberRepository, times(1)).findByUserAndProject(mockLeaderUser, mockProject);
//        verify(issueRepository, times(2)).findById(anyLong()); // 리스트에 두 개의 이슈가 있으므로 2번 호출
//        verify(releaseRepository, times(1)).save(any(ReleaseNote.class));
//        verify(releaseApprovalRepository, times(mockMemberList.size())).save(any(ReleaseApproval.class));
//    }

    @Test
    @DisplayName("5.2 릴리즈 노트 생성 - 다음 버전을 알맞게 추가한 경우")
    void testAddReleaseNote_ValidVersion() {
        // 테스트를 위한 mock 릴리즈 노트 생성 정보
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );

        // 릴리즈 버전들이 담길 리스트를 초기화
        List<String> mockReleaseVersions = new ArrayList<>();
        mockReleaseVersions.add("1.0.0");

        // releaseRepository.findAllVersionsByProject() 메서드가 mockReleaseVersions를 반환하도록 설정 (해당 프로젝트의 릴리즈 버전 리스트)
        when(releaseRepository.findAllVersionsByProject(mockProject)).thenReturn(mockReleaseVersions);

        // 새로운 버전 생성 서비스 호출
        String newVersion = releaseService.createReleaseVersion(mockProject, "MAJOR");

        // 테스트 결과 검증
        assertEquals("2.0.0", newVersion);

        // 각 메서드가 호출 됐는지 확인
        verify(releaseRepository, times(1)).findAllVersionsByProject(mockProject);
    }

    @Test
    @DisplayName("5.3 릴리즈 노트 수정 - PM이 아닌 멤버가 수정 시 불가능")
    void testSaveReleaseWithoutPM() {
        // 테스트를 위한 mock 릴리즈 노트 수정 정보
        String mockUserEmail = "testMember@releaser.com";
        Long mockReleaseId = 1L;

        User mockMemberUser = new User(
                "testUserName", mockUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                1L, 'M', 'Y', mockMemberUser, mockProject
        );
        ReleaseNote mockRelease = new ReleaseNote(
                1L, "release Title", "release Content", null,
                "1.0.0", null, ReleaseDeployStatus.PLANNING, mockProject, 50.0, 50.0
        );
        ReleaseUpdateRequestDTO mockReqDTO = new ReleaseUpdateRequestDTO(
                "release Update Title", "2.0.0", "release Update Content", null, "PLANNING", null
        );

        // releaseRepository.findById() 메서드가 mockRelease를 반환하도록 설정
        when(releaseRepository.findById(mockReleaseId)).thenReturn(Optional.of(mockRelease));

        // userRepository.findByEmail() 메서드가 mockMemberUser를 반환하도록 설정 (접근한 유저의 정보 조회)
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockMemberUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockMemberUser, mockProject)).thenReturn(Optional.of(mockMember));

        // 예외 메시지 검증용
        String expectedExceptionMessage = String.valueOf(NOT_PROJECT_MANAGER);

        // 테스트 실행 및 예외 검증 (프로젝트 PM이 아닌 경우 예외 발생)
        assertThrows(CustomException.class, () -> releaseService.saveReleaseNote(mockUserEmail, mockReleaseId, mockReqDTO), expectedExceptionMessage);

        // 각 메서드가 호출 됐는지 확인
        verify(releaseRepository, times(1)).findById(mockReleaseId);
        verify(userRepository, times(1)).findByEmail(mockUserEmail);
        verify(projectMemberRepository, times(1)).findByUserAndProject(mockMemberUser, mockProject);
    }

    @Test
    @DisplayName("5.3 릴리즈 노트 수정 - 수정하려는 버전이 1.0.0인 경우 예외 발생")
    void testSaveReleaseWithVersion1_0_0() {
        // 테스트를 위한 mock 릴리즈 노트 수정 정보
        String mockLeaderUserEmail = "testLeader@releaser.com";
        Long mockReleaseId = 1L;

        User mockLeaderUser = new User(
                "testUserName", mockLeaderUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                1L, 'M', 'Y', mockLeaderUser, mockProject
        );
        ReleaseNote mockRelease = new ReleaseNote(
                mockReleaseId, "release Title", "release Content", null,
                "1.0.0", null, ReleaseDeployStatus.PLANNING, mockProject, 50.0, 50.0
        );
        ReleaseUpdateRequestDTO mockReqDTO = new ReleaseUpdateRequestDTO(
                "release Update Title", "2.0.0", "release Update Content", null, "PLANNING", null
        );

        // releaseRepository.findById() 메서드가 mockRelease를 반환하도록 설정
        when(releaseRepository.findById(mockReleaseId)).thenReturn(Optional.of(mockRelease));

        // userRepository.findByEmail() 메서드가 mockLeaderUser를 반환하도록 설정
        when(userRepository.findByEmail(mockLeaderUserEmail)).thenReturn(Optional.of(mockLeaderUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockLeaderUser, mockProject)).thenReturn(Optional.of(mockMember));

        // 예외 메시지 검증용
        String expectedExceptionMessage = String.valueOf(FAILED_TO_UPDATE_INITIAL_RELEASE_VERSION);

        // 테스트 실행 및 예외 검증 (1.0.0 버전 수정할 경우 예외 발생)
        assertThrows(CustomException.class, () -> releaseService.updateReleaseVersion(mockRelease, mockReqDTO.getVersion()), expectedExceptionMessage);
    }

    @Test
    @DisplayName("5.3 릴리즈 노트 수정 - 중복 버전인 경우 예외 발생")
    void testSaveReleaseWithDuplicatedVersion() {
        // 테스트를 위한 mock 릴리즈 노트 수정 정보
        String mockLeaderUserEmail = "testLeader@releaser.com";
        Long mockReleaseId = 1L;

        User mockLeaderUser = new User(
                "testUserName", mockLeaderUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                1L, 'M', 'Y', mockLeaderUser, mockProject
        );
        ReleaseNote mockRelease = new ReleaseNote(
                mockReleaseId, "release Title", "release Content", null,
                "1.0.1", null, ReleaseDeployStatus.PLANNING, mockProject, 50.0, 50.0
        );
        ReleaseUpdateRequestDTO mockReqDTO = new ReleaseUpdateRequestDTO(
                "release Update Title", "1.0.3", "release Update Content", null, "PLANNING", null
        );

        // releaseRepository.findById() 메서드가 mockRelease를 반환하도록 설정
        when(releaseRepository.findById(mockReleaseId)).thenReturn(Optional.of(mockRelease));

        // userRepository.findByEmail() 메서드가 mockLeaderUser를 반환하도록 설정
        when(userRepository.findByEmail(mockLeaderUserEmail)).thenReturn(Optional.of(mockLeaderUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockLeaderUser, mockProject)).thenReturn(Optional.of(mockMember));

        // releaseRepository.existsByProjectAndVersion() 메서드가 true를 반환하도록 설정 (이미 있는 버전이면 true 반 )
        when(releaseRepository.existsByProjectAndVersion(eq(mockProject), eq(mockReleaseId), eq(mockReqDTO.getVersion()))).thenReturn(true);


        // 예외 메시지 검증용
        String expectedExceptionMessage = String.valueOf(DUPLICATED_RELEASE_VERSION);

        // 테스트 실행 및 예외 검증 (이미 있는 버전일 경우 예외 발생)
        assertThrows(CustomException.class, () -> releaseService.updateReleaseVersion(mockRelease, mockReqDTO.getVersion()), expectedExceptionMessage);
    }

    @Test
    @DisplayName("5.3 릴리즈 노트 수정 - 잘못된 버전을 입력한 경우 예외 발생")
    void testSaveReleaseWithWrongVersion() {
        // 테스트를 위한 mock 릴리즈 노트 수정 정보
        String mockLeaderUserEmail = "testLeader@releaser.com";
        Long mockReleaseId = 1L;

        User mockLeaderUser = new User(
                "testUserName", mockLeaderUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                1L, 'M', 'Y', mockLeaderUser, mockProject
        );
        ReleaseNote mockRelease = new ReleaseNote(
                mockReleaseId, "release Title", "release Content", null,
                "1.0.0", null, ReleaseDeployStatus.PLANNING, mockProject, 50.0, 50.0
        );

        // releaseRepository.findById() 메서드가 mockRelease를 반환하도록 설정
        when(releaseRepository.findById(mockReleaseId)).thenReturn(Optional.of(mockRelease));

        // userRepository.findByEmail() 메서드가 mockLeaderUser를 반환하도록 설정
        when(userRepository.findByEmail(mockLeaderUserEmail)).thenReturn(Optional.of(mockLeaderUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockLeaderUser, mockProject)).thenReturn(Optional.of(mockMember));

        // 버전을 나누어 체크
        int[] majors = {1, 4};
        int[] minors = {0, 0};
        int[] patches = {0, 0};

        int end = majors.length - 1;
        int majorStartIdx = 0;
        int minorStartIdx = 0;

        // 예외 메시지 검증용
        String expectedExceptionMessage = String.valueOf(INVALID_RELEASE_VERSION);

        // 버전 검증 서비스 호출 (올바르지 않은 버전일 경우 예외 발생)
        assertThrows(CustomException.class, () ->
            releaseService.validateMajorVersion(majors, minors, patches, end, majorStartIdx, minorStartIdx), expectedExceptionMessage);
    }

    @Test
    @DisplayName("5.4 릴리즈 노트 삭제 - PM이 아닌 멤버가 삭제할 경우")
    void testRemoveReleaseNoteWithoutPM() {
        // 테스트를 위한 mock 릴리즈 노트 삭제 정보
        String mockUserEmail = "testMember@releaser.com";
        Long mockReleaseId = 1L;

        User mockMemberUser = new User(
                "memberUserName", mockUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                1L, 'M', 'Y', mockMemberUser, mockProject
        );
        ReleaseNote mockRelease = new ReleaseNote(
                mockReleaseId, "release Title", "release Content", null,
                "1.0.0", null, ReleaseDeployStatus.PLANNING, mockProject, 50.0, 50.0
        );

        // releaseRepository.findById() 메서드가 mockRelease를 반환하도록 설정
        when(releaseRepository.findById(mockReleaseId)).thenReturn(Optional.of(mockRelease));

        // userRepository.findByEmail() 메서드가 mockMemberUser를 반환하도록 설정
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockMemberUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockMemberUser, mockProject)).thenReturn(Optional.of(mockMember));

        // 예외 메시지 검증용
        String expectedExceptionMessage = String.valueOf(NOT_PROJECT_MANAGER);

        // 테스트 실행 및 예외 검증 (멤버가 삭제할 경우 예외 발생)
        assertThrows(CustomException.class, () ->
            releaseService.removeReleaseNote(mockUserEmail, mockReleaseId), expectedExceptionMessage);
    }

    @Test
    @DisplayName("5.4 릴리즈 노트 삭제 - 배포된 릴리즈 노트일 경우 삭제 불가능")
    void testRemoveReleaseNoteWithDeployedRelease() {
        // 테스트를 위한 Mock 릴리즈 노트 삭제 정보
        String mockUserEmail = "testLeader@releaser.com";
        Long mockReleaseId = 1L;

        User mockLeaderUser = new User(
                "leaderUserName", mockUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                1L, 'M', 'Y', mockLeaderUser, mockProject
        );
        ReleaseNote mockRelease = new ReleaseNote(
                mockReleaseId, "release Title", "release Content", null,
                "1.0.0", null, ReleaseDeployStatus.DEPLOYED, mockProject, 50.0, 50.0
        );

        // releaseRepository.findById() 메서드가 mockRelease를 반환하도록 설정
        when(releaseRepository.findById(mockReleaseId)).thenReturn(Optional.of(mockRelease));

        // userRepository.findByEmail() 메서드가 mockLeaderUser를 반환하도록 설정
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockLeaderUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockLeaderUser, mockProject)).thenReturn(Optional.of(mockMember));

        // 예외 메시지 검증용
        String expectedExceptionMessage = String.valueOf(FAILED_TO_DELETE_DEPLOYED_RELEASE_NOTE);

        // 테스트 실행 및 예외 검증 (배포된 릴리즈일 경우 예외 발생)
        assertThrows(CustomException.class, () ->
            releaseService.removeReleaseNote(mockUserEmail, mockReleaseId), expectedExceptionMessage);

    }

    @Test
    @DisplayName("5.6 릴리즈 노트 배포 동의 여부 선택 - 프로젝트 멤버가 아닌 경우")
    void testModifyReleaseApprovalWithoutMember() {
        // 테스트를 위한 Mock 릴리즈 노트 배포 동의 여부 선택 정보
        String mockUserEmail = "test@releaser.com";
        Long mockReleaseId = 1L;

        User mockUser = new User(
                "UserName", mockUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );

        ReleaseNote mockRelease = new ReleaseNote(
                mockReleaseId, "release Title", "release Content", null,
                "1.0.0", null, ReleaseDeployStatus.PLANNING, mockProject, 50.0, 50.0
        );
        ReleaseApprovalRequestDTO mockReqDTO = new ReleaseApprovalRequestDTO(
                "Y"
        );

        // releaseRepository.findById() 메서드가 mockRelease를 반환하도록 설정
        when(releaseRepository.findById(mockReleaseId)).thenReturn(Optional.of(mockRelease));

        // userRepository.findByEmail() 메서드가 mockUser를 반환하도록 설정
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockUser));

        // 예외 메시지 검증용
        String expectedExceptionMessage = String.valueOf(NOT_EXISTS_PROJECT_MEMBER);

        // 테스트 실행 및 예외 검증 (해당 프로젝트 멤버가 아닐 경우)
        assertThrows(CustomException.class, () ->
            releaseService.modifyReleaseApproval(mockUserEmail, mockReleaseId, mockReqDTO), expectedExceptionMessage);
    }

    @Test
    @DisplayName("5.6 릴리즈 노트 배포 동의 여부 선택 - 배포된 릴리즈 노트인 경우 예외 발생")
    void testModifyReleaseApproval() {
        // 테스트를 위한 mock 릴리즈 노트 배포 동의 여부 선택 정보
        String mockMemberUserEmail = "testMember@releaser.com";
        Long mockReleaseId = 1L;

        User mockMemberUser = new User(
                "UserName", mockMemberUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                1L, 'M', 'Y', mockMemberUser, mockProject
        );
        ReleaseNote mockRelease = new ReleaseNote(
                mockReleaseId, "release Title", "release Content", null,
                "1.0.0", null, ReleaseDeployStatus.DEPLOYED, mockProject, 50.0, 50.0
        );
        ReleaseApprovalRequestDTO mockReqDTO = new ReleaseApprovalRequestDTO(
                "Y"
        );

        // releaseRepository.findById() 메서드가 mockRelease를 반환하도록 설정
        when(releaseRepository.findById(mockReleaseId)).thenReturn(Optional.of(mockRelease));

        // userRepository.findByEmail() 메서드가 mockMemberUser를 반환하도록 설정
        when(userRepository.findByEmail(mockMemberUserEmail)).thenReturn(Optional.of(mockMemberUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockMemberUser, mockProject)).thenReturn(Optional.of(mockMember));

        // 예외 메시지 검증용
        String expectedExceptionMessage = String.valueOf(FAILED_TO_APPROVE_RELEASE_NOTE);

        // 테스트 실행 및 예외 검증 (배포된 릴리즈 노트에 동의를 구할 경우 예외 발생)
        assertThrows(CustomException.class, () ->
            releaseService.modifyReleaseApproval(mockMemberUserEmail, mockReleaseId, mockReqDTO), expectedExceptionMessage);
    }

    @Test
    @DisplayName("6.2 릴리즈 노트 의견 삭제 - 해당 의견 작성자가 아닌 경우 예외 발생")
    void testRemoveReleaseOpinionWithoutCommenter() {
        // 테스트를 위한 mock 릴리즈 노트 의견 삭제 정보
        String mockUserEmail = "test@releaser.com";
        Long mockOpinionId = 1L;

        User mockMemberUser = new User(
                "UserName", mockUserEmail, null, 'Y'
        );
        User mockCommenterUser = new User(
                "CommenterUserName", "testCommenter@releaser.com", null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                1L, 'M', 'Y', mockMemberUser, mockProject
        );
        ProjectMember mockCommenterMember = new ProjectMember(
                1L, 'M', 'Y', mockCommenterUser, mockProject
        );
        ReleaseNote mockRelease = new ReleaseNote(
                1L, "release Title", "release Content", null,
                "1.0.0", null, ReleaseDeployStatus.PLANNING, mockProject, 50.0, 50.0
        );
        ReleaseOpinion mockReleaseOpinion = new ReleaseOpinion(
                "opinion", mockRelease, mockCommenterMember
        );

        // releaseOpinionRepository.findById() 메서드가 mockReleaseOpinion를 반환하도록 설정
        when(releaseOpinionRepository.findById(mockOpinionId)).thenReturn(Optional.of(mockReleaseOpinion));

        // userRepository.findByEmail() 메서드가 mockMemberUser를 반환하도록 설정
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockMemberUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockMemberUser, mockProject)).thenReturn(Optional.of(mockMember));

        // 예외 메시지 검증용
        String expectedExceptionMessage = String.valueOf(UNAUTHORIZED_TO_DELETE_RELEASE_OPINION);

        // 테스트 실행 및 예외 검증 (해당 의견 작성자 아닐 경우 예외 발생)
        assertThrows(CustomException.class, () ->
            releaseService.removeReleaseOpinion(mockUserEmail, mockOpinionId), expectedExceptionMessage);
    }

    @Test
    @DisplayName("9.1 프로젝트별 릴리즈 보고서 조회")
    void testFindReleaseDocs() {
        // 테스트를 위한 mock 프로젝트별 릴리즈 보고서 조회 정보
        Long mockProjectId = 1L;

        Project mockProject = new Project(
                mockProjectId, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        // 릴리즈 노트 리스트 초기화
        List<ReleaseNote> releaseNoteList = new ArrayList<>();
        ReleaseNote mockRelease1 = new ReleaseNote(
                1L, "release Title", "release Content", null,
                "1.0.0", null, ReleaseDeployStatus.DEPLOYED, mockProject, 50.0, 50.0
        );
        ReleaseNote mockRelease2 = new ReleaseNote(
                2L, "release Title", "release Content", null,
                "2.0.0", null, ReleaseDeployStatus.DEPLOYED, mockProject, 50.0, 50.0
        );
        // 리스트에 릴리즈 추가
        releaseNoteList.add(mockRelease1);
        releaseNoteList.add(mockRelease2);
        // 이슈 리스트 초기화
        List<Issue> issueList = new ArrayList<>();
        Issue mockIssueNew = new Issue(
                1L, "Test Issue Title", "Test Issue Content", null,
                Tag.NEW, Date.valueOf("2023-08-02"), LifeCycle.DONE, 'N', 'Y',
                mockProject, null, mockRelease1, null
        );
        Issue mockIssueFeature = new Issue(
                2L, "Test Issue Title", "Test Issue Content", null,
                Tag.FEATURE, Date.valueOf("2023-08-02"), LifeCycle.DONE, 'N', 'Y',
                mockProject, null, mockRelease2, null
        );
        // 리스트에 이슈 추가
        issueList.add(mockIssueNew);
        issueList.add(mockIssueFeature);

        // projectRepository.findById() 메서드가 mockProject를 반환하도록 설정
        when(projectRepository.findById(mockProjectId)).thenReturn(Optional.of(mockProject));

        // releaseRepository.findAllByProject() 메서드가 releaseNoteList를 반환하도록 설정 (해당 프로젝트에 관련한 릴리즈 노트 정보 리스트 조회)
        when(releaseRepository.findAllByProject(mockProject)).thenReturn(releaseNoteList);

        // issueRepository.findByRelease() 메서드가 issueList를 반환하도록 설정 (해당 릴리즈 노트와 연결된 이슈 리스트 조회)
        when(issueRepository.findByRelease(any(ReleaseNote.class))).thenReturn(issueList);

        // 릴리즈 노트 보고서 조회 서비스 호출
        List<ReleaseResponseDto.ReleaseDocsResponseDTO> result = releaseService.findReleaseDocs(mockProjectId);

        // 결과 검증
        assertNotNull(result);
        assertEquals("NEW", result.get(0).getTagsList().get(0).getTag());
        assertEquals("FEATURE", result.get(0).getTagsList().get(1).getTag());

        // 각 메서드 호출됐는지 확인
        verify(projectRepository, times(1)).findById(mockProjectId);
        verify(releaseRepository, times(1)).findAllByProject(mockProject);
        verify(issueRepository, times(2)).findByRelease(any(ReleaseNote.class));
    }

    @Test
    @DisplayName("9.2 프로젝트별 릴리즈 보고서 수정 - PM이 수정할 경우")
    void testModifyReleaseDocsWithPM() {
        // 테스트를 위한 mock 프로젝트별 릴리즈 보고서 수정 정보
        Long mockProjectId = 1L;
        String mockUserEmail = "testLeader@releaser.com";

        Project mockProject = new Project(
                mockProjectId, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        User mockLeaderUser = new User(
                "leaderUserName", mockUserEmail, null, 'Y'

        );
        ProjectMember mockLeaderMember = new ProjectMember(
                1L, 'L', 'Y', mockLeaderUser, mockProject

        );
        // 요청 DTO 리스트 초기화
        List<UpdateReleaseDocsRequestDTO> mockReqDTOList = new ArrayList<>();
        UpdateReleaseDocsRequestDTO mockReqDTO1 = new UpdateReleaseDocsRequestDTO(
                1L, "summary1"
        );
        UpdateReleaseDocsRequestDTO mockReqDTO2 = new UpdateReleaseDocsRequestDTO(
                2L, "summary2"
        );
        // 리스트에 요청 DTO 추가
        mockReqDTOList.add(mockReqDTO1);
        mockReqDTOList.add(mockReqDTO2);
        Issue mockIssueNew = new Issue(
                1L, "Test Issue Title", "Test Issue Content", null,
                Tag.NEW, Date.valueOf("2023-08-02"), LifeCycle.DONE, 'N', 'Y',
                mockProject, null, null, null
        );
        Issue mockIssueFeature = new Issue(
                2L, "Test Issue Title", "Test Issue Content", null,
                Tag.FEATURE, Date.valueOf("2023-08-02"), LifeCycle.DONE, 'N', 'Y',
                mockProject, null, null, null
        );

        // userRepository.findByEmail() 메서드가 mockLeaderUser를 반환하도록 설정
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockLeaderUser));

        // projectRepository.findById() 메서드가 mockProject를 반환하도록 설정
        when(projectRepository.findById(mockProjectId)).thenReturn(Optional.of(mockProject));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockLeaderMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockLeaderUser, mockProject)).thenReturn(Optional.of(mockLeaderMember));

        // issueRepository.findById() 메서드가 이슈를 반환하도록 설정
        when(issueRepository.findById(1L)).thenReturn(Optional.of(mockIssueNew));
        when(issueRepository.findById(2L)).thenReturn(Optional.of(mockIssueFeature));

        // 릴리즈 노트 보고서 수정 서비스 호출
        String result = releaseService.modifyReleaseDocs(mockProjectId, mockUserEmail, mockReqDTOList);

        // 결과 검증
        assertEquals("릴리즈 보고서가 수정되었습니다.", result);

        // 각 메서드 호출됐는지 확인
        verify(userRepository, times(1)).findByEmail(mockUserEmail);
        verify(projectRepository, times(1)).findById(mockProjectId);
        verify(projectMemberRepository, times(1)).findByUserAndProject(mockLeaderUser, mockProject);
        verify(issueRepository, times(2)).findById(any(Long.class));
    }

    @Test
    @DisplayName("9.2 프로젝트별 릴리즈 보고서 수정 - member 수정할 경우 예외 발생")
    void testModifyReleaseDocsWithoutPM() {
        // 테스트를 위한 mock 프로젝트별 릴리즈 보고서 수정 정보
        Long mockProjectId = 1L;
        String mockUserEmail = "testMember@releaser.com";

        Project mockProject = new Project(
                mockProjectId, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        User mockMemberUser = new User(
                "lmemberUserName", mockUserEmail, null, 'Y'

        );
        ProjectMember mockMember = new ProjectMember(
                1L, 'M', 'Y', mockMemberUser, mockProject

        );
        // 요청 DTO 리스트 초기화
        List<UpdateReleaseDocsRequestDTO> mockReqDTOList = new ArrayList<>();
        UpdateReleaseDocsRequestDTO mockReqDTO1 = new UpdateReleaseDocsRequestDTO(
                1L, "summary1"
        );
        UpdateReleaseDocsRequestDTO mockReqDTO2 = new UpdateReleaseDocsRequestDTO(
                2L, "summary2"
        );
        // 리스트에 요청 DTO 추가
        mockReqDTOList.add(mockReqDTO1);
        mockReqDTOList.add(mockReqDTO2);

        // userRepository.findByEmail() 메서드가 mockMemberUser를 반환하도록 설정
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockMemberUser));

        // projectRepository.findById() 메서드가 mockProject를 반환하도록 설정
        when(projectRepository.findById(mockProjectId)).thenReturn(Optional.of(mockProject));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockMember를 반환하도록 설정 (해당 유저는 프로젝트에서 멤버 역할)
        when(projectMemberRepository.findByUserAndProject(mockMemberUser, mockProject)).thenReturn(Optional.of(mockMember));

        // 예외 메시지 검증용 (프로젝트의 PM이 아닌 경우)
        String expectedExceptionMessage = String.valueOf(NOT_ADMIN);

        // 테스트 실행 및 예외 검증
        assertThrows(CustomException.class, () -> releaseService.modifyReleaseDocs(mockProjectId, mockUserEmail, mockReqDTOList), expectedExceptionMessage);

        // 각 메서드가 호출됐는지 확인
        verify(userRepository, times(1)).findByEmail(mockUserEmail);
        verify(projectRepository, times(1)).findById(mockProjectId);
        verify(projectMemberRepository, times(1)).findByUserAndProject(mockMemberUser, mockProject);
    }

}