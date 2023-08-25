package com.momentum.releaser.domain.issue.application;

import com.momentum.releaser.domain.issue.dao.IssueNumRepository;
import com.momentum.releaser.domain.issue.dao.IssueOpinionRepository;
import com.momentum.releaser.domain.issue.dao.IssueRepository;
import com.momentum.releaser.domain.issue.domain.*;
import com.momentum.releaser.domain.issue.dto.IssueRequestDto.IssueInfoRequestDTO;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.IssueIdResponseDTO;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.IssueModifyResponseDTO;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.OpinionInfoResponseDTO;
import com.momentum.releaser.domain.notification.event.NotificationEventPublisher;
import com.momentum.releaser.domain.project.dao.ProjectMemberRepository;
import com.momentum.releaser.domain.project.dao.ProjectRepository;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.release.dao.release.ReleaseRepository;
import com.momentum.releaser.domain.release.domain.ReleaseEnum.ReleaseDeployStatus;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.global.exception.CustomException;
import com.momentum.releaser.redis.issue.OrderIssueRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.momentum.releaser.domain.issue.dto.IssueRequestDto.*;
import static com.momentum.releaser.global.config.BaseResponseStatus.*;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;


class IssueServiceImplTest {

    private IssueServiceImpl issueService;
    private IssueRepository issueRepository;
    private IssueOpinionRepository issueOpinionRepository;
    private IssueNumRepository issueNumRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository projectMemberRepository;
    private UserRepository userRepository;
    private ReleaseRepository releaseRepository;
    private OrderIssueRedisRepository orderIssueRedisRepository;
    private NotificationEventPublisher notificationEventPublisher;


    @BeforeEach
    void setUp() {
        issueRepository = mock(IssueRepository.class);
        issueOpinionRepository = mock(IssueOpinionRepository.class);
        issueNumRepository = mock(IssueNumRepository.class);
        projectRepository = mock(ProjectRepository.class);
        projectMemberRepository = mock(ProjectMemberRepository.class);
        userRepository = mock(UserRepository.class);
        releaseRepository = mock(ReleaseRepository.class);
        notificationEventPublisher = mock(NotificationEventPublisher.class);
        orderIssueRedisRepository = mock(OrderIssueRedisRepository.class);
        issueService = new IssueServiceImpl(issueRepository, issueOpinionRepository, issueNumRepository, projectRepository,
                projectMemberRepository, userRepository, releaseRepository, orderIssueRedisRepository, notificationEventPublisher);
    }

//    @Test
//    @DisplayName("7.1 이슈 생성")
//    void testAddIssue() {
//        // 테스트를 위한 Mock 이슈 생성 정보
//        Long mockProjectId = 1L;
//        Long mockMemberId = 1L;
//        String mockUserEmail = "testLeader@releaser.com";
//
//        Project mockProject = new Project(
//                mockProjectId, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
//        );
//        User mockUser = new User(
//                "testUser1Name", mockUserEmail, null, 'Y'
//        );
//        List<String> mockConsumers = new ArrayList<>();
//        mockConsumers.add(mockUserEmail);
//        ProjectMember mockProjectMember = new ProjectMember(
//                mockMemberId, 'L', 'Y', mockUser, mockProject
//        );
//        IssueInfoRequestDTO mockIssueInfoRequestDTO = new IssueInfoRequestDTO(
//                "Test Issue Title", "Test Issue Content", Tag.NEW.toString(), Date.valueOf("2023-08-02"), mockMemberId
//        );
//        Issue mockSavedIssue = new Issue(
//                1L, "Test Issue Title", "Test Issue Content", null,
//                Tag.NEW, Date.valueOf("2023-08-02"), LifeCycle.DONE, 'N', 'Y', mockProject, mockProjectMember, null, null
//        );
//        IssueNum mockIssueNum = new IssueNum(
//                1L, mockSavedIssue, mockProject, 1L
//        );
//
//        mockSavedIssue.updateIssueNum(mockIssueNum);
//
//        // projectRepository.findById() 메서드가 mockProject를 반환하도록 설정
//        when(projectRepository.findById(mockProjectId)).thenReturn(Optional.of(mockProject));
//
//        // projectMemberRepository.findById() 메서드가 mockProjectMember를 반환하도록 설정 (이슈 담당자 지정하기 위한 멤버 조회)
//        when(projectMemberRepository.findById(mockMemberId)).thenReturn(Optional.of(mockProjectMember));
//
//        // issueRepository.getIssueNum() 메서드가 0L을 반환하도록 설정 (해당 프로젝트에 대한 최근 이슈 번호)
//        when(issueRepository.getIssueNum(mockProject)).thenReturn(0L);
//
//        // issueRepository.save() 메서드가 mockSavedIssue를 반환하도록 설정 (생성하고 싶은 이슈 정보 저장)
//        when(issueRepository.save(any(Issue.class))).thenReturn(mockSavedIssue);
//
//        // issueNumRepository.save() 메서드가 mockIssueNum를 반환하도록 설정
//        when(issueNumRepository.save(any(IssueNum.class))).thenReturn(mockIssueNum);
//
//        when(projectMemberRepository.findByProject(mockProject).stream().map(m -> m.getUser().getEmail())
//                .collect(Collectors.toList())).thenReturn(mockConsumers);
//
//        // 이슈 생성 서비스 호출
//        IssueIdResponseDTO result = issueService.addIssue(mockUserEmail, mockProjectId, mockIssueInfoRequestDTO);
//
//        // 예상된 결과와 실제 결과 비교
//        assertNotNull(result);
//        assertEquals(mockSavedIssue.getIssueId(), result.getIssueId());
//
//        // 각 메서드가 호출됐는지 확인
//        verify(projectRepository, times(1)).findById(mockProjectId);
//        verify(projectMemberRepository, times(1)).findById(mockMemberId);
//        verify(issueRepository, times(1)).getIssueNum(mockProject);
//        verify(issueRepository, times(1)).save(any(Issue.class));
//        verify(issueNumRepository, times(1)).save(any(IssueNum.class));
//    }

    @Test
    @DisplayName("7.2 이슈 수정 - PM이 수정할 경우")
    void testModifyIssueWithPM() {
        // 테스트를 위한 mock 이슈 수정 정보
        Long mockIssueId = 1L;
        String mockAccessUserEmail = "test@releaser.com";

        User mockAccessUser = new User(
                "accessUser", mockAccessUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam",
                null, "testLink", 'Y'
        );
        ProjectMember mockAccessMember = new ProjectMember(
                1L, 'L', 'Y', mockAccessUser, mockProject
        );
        ProjectMember mockManagerMember = new ProjectMember(
                2L, 'M', 'Y', null, mockProject
        );
        Issue mockIssue = new Issue(
                mockIssueId, "Issue Title", "Issue Content", null,
                Tag.CHANGED, null, LifeCycle.NOT_STARTED, 'N', 'Y',
                mockProject, mockManagerMember, null, null
        );
        IssueInfoRequestDTO mockReqDTO = new IssueInfoRequestDTO(
                "Update Issue Title", "Update Issue Content",
                String.valueOf(Tag.FEATURE), null, 2L
        );
        Issue mockUpdateIssue = new Issue(
                mockIssueId, "Update Issue Title", "Update Issue Content", null,
                Tag.FEATURE, null, LifeCycle.NOT_STARTED, 'N', 'Y',
                mockProject, mockManagerMember, null, null
        );

        // issueRepository.findById() 메서드가 mockIssue를 반환하도록 설정 (수정할 이슈가 존재하는 지 확인)
        when(issueRepository.findById(mockIssueId)).thenReturn(Optional.of(mockIssue));

        // userRepository.findOneByEmail() 메서드가 mockAccessUser를 반환하도록 설정 (접근한 유저의 정보)
        when(userRepository.findOneByEmail(mockAccessUserEmail)).thenReturn(Optional.of(mockAccessUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockAccessMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockAccessUser, mockProject)).thenReturn(Optional.of(mockAccessMember));

        // projectMemberRepository.findById() 메서드가 mockManagerMember를 반환하도록 설정 (이슈 담당자 정보 조회)
        when(projectMemberRepository.findById(2L)).thenReturn(Optional.of(mockManagerMember));

        // issueRepository.save() 메서드가 mockUpdateIssue를 반환하도록 설정 (이슈 수정 정보를 업데이트)
        when(issueRepository.save(any(Issue.class))).thenReturn(mockUpdateIssue);

        // 이슈 수정 서비스 호출
        IssueModifyResponseDTO result = issueService.modifyIssue(mockIssueId, mockAccessUserEmail, mockReqDTO);

        // 결과가 null인지 확인
        assertNotNull(result);

        // PM이 수정할 경우 수정 상태 N
        assertEquals('N', mockUpdateIssue.getEdit());

        // 각 메서드가 호출 됐는지 확인
        verify(issueRepository, times(1)).findById(mockIssueId);
        verify(userRepository, times(1)).findOneByEmail(mockAccessUserEmail);
        verify(projectMemberRepository, times(1)).findByUserAndProject(mockAccessUser, mockProject);
        verify(projectMemberRepository, times(1)).findById(2L);
        verify(issueRepository, times(1)).save(any(Issue.class));
    }

    @Test
    @DisplayName("7.2 이슈 수정 - member가 수정할 경우")
    void testModifyIssueWithMember() {
        // 테스트를 위한 mock 이슈 수정 정보
        Long mockIssueId = 1L;
        String mockAccessUserEmail = "test@releaser.com";

        User mockAccessUser = new User(
                "accessUser", mockAccessUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam",
                null, "testLink", 'Y'
        );
        ProjectMember mockAccessMember = new ProjectMember(
                1L, 'M', 'Y', mockAccessUser, mockProject
        );
        ProjectMember mockManagerMember = new ProjectMember(
                2L, 'M', 'Y', null, mockProject
        );
        Issue mockIssue = new Issue(
                mockIssueId, "Issue Title", "Issue Content", null,
                Tag.CHANGED, null, LifeCycle.NOT_STARTED, 'N', 'Y',
                mockProject, mockManagerMember, null, null
        );
        IssueInfoRequestDTO mockReqDTO = new IssueInfoRequestDTO(
                "Update Issue Title", "Update Issue Content",
                String.valueOf(Tag.FEATURE), null, 2L
        );
        Issue mockUpdateIssue = new Issue(
                mockIssueId, "Update Issue Title", "Update Issue Content", null,
                Tag.FEATURE, null, LifeCycle.NOT_STARTED, 'Y', 'Y',
                mockProject, mockManagerMember, null, null
        );

        // issueRepository.findById() 메서드가 mockIssue를 반환하도록 설정
        when(issueRepository.findById(mockIssueId)).thenReturn(Optional.of(mockIssue));

        // userRepository.findOneByEmail() 메서드가 mockAccessUser를 반환하도록 설정
        when(userRepository.findOneByEmail(mockAccessUserEmail)).thenReturn(Optional.of(mockAccessUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockAccessMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockAccessUser, mockProject)).thenReturn(Optional.of(mockAccessMember));

        // projectMemberRepository.findById() 메서드가 mockManagerMember를 반환하도록 설정 (이슈 담당자 정보 조회)
        when(projectMemberRepository.findById(2L)).thenReturn(Optional.of(mockManagerMember));

        // issueRepository.save() 메서드가 mockUpdateIssue를 반환하도록 설정
        when(issueRepository.save(any(Issue.class))).thenReturn(mockUpdateIssue);

        // 이슈 수정 서비스 호출
        IssueModifyResponseDTO result = issueService.modifyIssue(mockIssueId, mockAccessUserEmail, mockReqDTO);

        // 결과가 null인지 확인
        assertNotNull(result);

        // member가 수정할 경우 이슈 상태 Y
        assertEquals('Y', mockUpdateIssue.getEdit());

        // 각 메서드가 호출 됐는지 확인
        verify(issueRepository, times(1)).findById(mockIssueId);
        verify(userRepository, times(1)).findOneByEmail(mockAccessUserEmail);
        verify(projectMemberRepository, times(1)).findByUserAndProject(mockAccessUser, mockProject);
        verify(projectMemberRepository, times(1)).findById(2L);
        verify(issueRepository, times(1)).save(any(Issue.class));
    }

    @Test
    @DisplayName("7.3 이슈 제거 - 연결된 릴리즈가 없는 경우")
    void testRemoveIssueWithoutConnectedRelease() {
        // 테스트를 위한 mock 이슈 제거 정보
        Long mockIssueId = 1L;

        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam",
                null, "testLink", 'Y'
        );
        IssueNum mockIssueNum = new IssueNum(
                2L, null, mockProject, 2L
        );
        Issue mockIssue = new Issue(
                mockIssueId, "issueTitle", "issueContent", null, Tag.FIXED, null,
                LifeCycle.NOT_STARTED, 'N', 'Y', mockProject, null, null, null
        );
        mockIssue.updateIssueNum(mockIssueNum);

        // issueRepository.findById() 메서드가 mockIssue를 반환하도록 설정 (삭제할 이슈 존재 여부 확인)
        when(issueRepository.findById(mockIssueId)).thenReturn(Optional.of(mockIssue));

        // 이슈 제거 서비스 호출
        String result = issueService.removeIssue(mockIssueId);

        // 결과 검증
        assertEquals("이슈가 삭제되었습니다.", result);

        // 각 메서드가 호출 됐는지 확인
        verify(issueRepository, times(1)).findById(mockIssueId);
        verify(issueNumRepository, times(1)).deleteById(mockIssueNum.getIssueNumId());
        verify(issueRepository, times(1)).deleteById(mockIssueId);
    }

    @Test
    @DisplayName("7.3 이슈 제거 - 연결된 릴리즈가 있는 경우 예외 발생")
    void testRemoveIssueWithConnectedRelease() {
        // 테스트를 위한 mock 이슈 제거 정보
        Long mockIssueId = 1L;
        Long mockReleaseId = 2L;

        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        ReleaseNote mockRelease = new ReleaseNote(
                mockReleaseId, "releaseTitle", "releaseDescription", null,"1.0.0", null,
                ReleaseDeployStatus.PLANNING, mockProject,50.0, 50.0
        );
        Issue mockIssue = new Issue(
                mockIssueId, "issueTitle", "issueContent", null, Tag.FIXED, null,
                LifeCycle.NOT_STARTED, 'N', 'Y', mockProject, null, mockRelease, null);

        // issueRepository.findById() 메서드가 mockIssue를 반환하도록 설정
        when(issueRepository.findById(mockIssueId)).thenReturn(Optional.of(mockIssue));

        // 예외 메시지 검증용
        String expectedExceptionMessage = String.valueOf(CONNECTED_RELEASE_EXISTS);

        // 테스트 실행 및 예외 검증 (연결된 릴리즈가 있는 경우 예외 발생)
        assertThrows(CustomException.class, () -> issueService.removeIssue(mockIssueId), expectedExceptionMessage);

        // 각 메서드가 호출 됐는지 확인
        verify(issueRepository, times(1)).findById(mockIssueId);
        verify(issueNumRepository, never()).deleteById(anyLong());
        verify(issueRepository, never()).deleteById(anyLong());
    }

//    @Test
//    @DisplayName("7.8 이슈 상태 변경 - 연결된 릴리즈가 없는 경우")
//    void testModifyIssueLifeCycleWithoutConnectedIssue() {
//        // 테스트를 위한 mock 이슈 상태 변경 정보
//        Long mockIssueId = 1L;
//        String mockLifeCycle = "IN_PROGRESS";
//        int mockIndex = 1;
//
//        Issue mockIssue = new Issue(
//                mockIssueId,
//                "issueTitle", "issueContent", null,
//                Tag.FIXED, null, LifeCycle.NOT_STARTED,
//                'N', 'Y', null, null, null, null);
//
//        // issueRepository.findById() 메서드가 mockIssue를 반환하도록 설정
//        when(issueRepository.findById(mockIssueId)).thenReturn(Optional.of(mockIssue));
//
//        // 이슈 상태 변경 서비스 호출
//        String result = issueService.modifyIssueLifeCycle(mockIssueId, mockIndex, mockLifeCycle);
//
//        // 결과 검증
//        assertEquals("이슈 상태 변경이 완료되었습니다.", result);
//
//        // 각 메서드가 호출됐는지 확인
//        verify(issueRepository, times(1)).findById(mockIssueId);
//        verify(issueRepository, times(1)).save(any(Issue.class));
//    }

    @Test
    @DisplayName("7.8 이슈 상태 변경 - 연결된 릴리즈가 있는 경우 예외 발생")
    void testModifyIssueLifeCycleWithConnectedIssue() {
        // 테스트를 위한 mock 이슈 상태 변경 정보
        Long mockIssueId = 1L;
        String mockLifeCycle = "IN_PROGRESS";
        Long mockReleaseId = 2L;
        int mockIndex = 1;

        ReleaseNote mockRelease = new ReleaseNote(
                mockReleaseId, "releaseTitle", "releaseDescription", null, "1.0.0", null,
                ReleaseDeployStatus.PLANNING, null, 50.0, 50.0
        );
        Issue mockIssue = new Issue(
                mockIssueId, "issueTitle", "issueContent", null, Tag.FIXED, null,
                LifeCycle.NOT_STARTED, 'N', 'Y', null, null, mockRelease, null
        );

        // issueRepository.findById() 메서드가 mockIssue를 반환하도록 설정
        when(issueRepository.findById(mockIssueId)).thenReturn(Optional.of(mockIssue));

        // 예외 메시지 검증용 (연결된 릴리즈가 있는 경우 예외 발생)
        String expectedExceptionMessage = String.valueOf(CONNECTED_ISSUE_EXISTS);

        // 이슈 상태 변경 서비스 호출
        assertThrows(CustomException.class, () -> issueService.modifyIssueLifeCycle(mockIssueId, mockIndex, mockLifeCycle), expectedExceptionMessage);

        // 각 메서드가 호출됐는지 확인
        verify(issueRepository, times(1)).findById(mockIssueId);
        verify(issueRepository, never()).save(any(Issue.class));
    }

    @Test
    @DisplayName("8.1 이슈 의견 추가")
    void testAddIssueOpinion() {
        // 테스트를 위한 mock 이슈 의견 추가 정보
        Long mockIssueId = 1L;
        Long mockMemberId = 2L;
        String mockUserEmail = "testUser@releaser.com";
        String mockOpinion = "test opinion.";

        User mockUser = new User(
                "testUserName", mockUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        Issue mockIssue = new Issue(
                mockIssueId, "issueTitle", "issueContent", null, Tag.FIXED, null,
                LifeCycle.NOT_STARTED, 'N', 'Y', mockProject, null, null, null
        );
        RegisterOpinionRequestDTO mockReqDTO = new RegisterOpinionRequestDTO(
                mockOpinion
        );
        ProjectMember mockProjectMember = new ProjectMember(
                mockMemberId, 'M', 'Y', mockUser, mockProject
        );
        // Mock 의견 등록 결과 리스트
        List<OpinionInfoResponseDTO> mockOpinionResponseList = new ArrayList<>();
        OpinionInfoResponseDTO mockOpinionResponse = new OpinionInfoResponseDTO(
                mockMemberId, mockUser.getName(), mockUser.getImg(), 1L, mockOpinion
        );
        mockOpinionResponse.setDeleteYN('Y');
        mockOpinionResponseList.add(mockOpinionResponse);
        IssueOpinion mockIssueOpinion = new IssueOpinion(
                1L, mockOpinion, 'Y', mockProjectMember, mockIssue
        );

        // issueRepository.findById() 메서드가 mockIssue를 반환하도록 설정
        when(issueRepository.findById(mockIssueId)).thenReturn(Optional.of(mockIssue));

        // userRepository.findOneByEmail() 메서드가 mockUser를 반환하도록 설정 (접근한 유저 정보)
        when(userRepository.findOneByEmail(mockUserEmail)).thenReturn(Optional.of(mockUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockProjectMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.of(mockProjectMember));

        // issueOpinionRepository.save() 메서드가 mockIssueOpinion를 반환하도록 설정
        when(issueOpinionRepository.save(any(IssueOpinion.class))).thenReturn(mockIssueOpinion);

        // issueRepository.getIssueOpinion() 메서드가 mockOpinionResponseList를 반환하도록 설정 (해당 이슈에 달린 의견 리스트)
        when(issueRepository.getIssueOpinion(mockIssue)).thenReturn(mockOpinionResponseList);

        // 이슈 의견 추가 서비스 호출
        List<OpinionInfoResponseDTO> result = issueService.addIssueOpinion(mockIssueId, mockUserEmail, mockReqDTO);

        // 결과 검증
        assertNotNull(result);
        assertEquals(1, result.size());

        // 해당 이슈 의견 응답에 대한 정보 검증
        OpinionInfoResponseDTO opinionResponse = result.get(0);
        assertEquals(mockMemberId, opinionResponse.getMemberId());
        assertEquals('Y', opinionResponse.getDeleteYN());

        // 각 메서드 호출됐는지 확인
        verify(issueRepository, times(1)).findById(mockIssueId);
        verify(userRepository, times(1)).findOneByEmail(mockUserEmail);
        verify(projectMemberRepository, times(1)).findByUserAndProject(mockUser, mockProject);
        verify(issueOpinionRepository, times(1)).save(any(IssueOpinion.class));
        verify(issueRepository, times(1)).getIssueOpinion(mockIssue);
    }

    @Test
    @DisplayName("8.2 이슈 의견 삭제 - 접근한 유저가 작성한 의견 삭제할 경우")
    void testRemoveIssueOpinionWithCommentUser() {
        // 테스트를 위한 mock 이슈 의견 삭제 정보
        Long mockOpinionId = 1L;
        String mockAccessUserEmail = "testUser@releaser.com";

        User mockAccessUser = new User(
                "testUserName", mockAccessUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        ProjectMember mockProjectMember = new ProjectMember(
                1L, 'M', 'Y', mockAccessUser, mockProject
        );
        Issue mockIssue = new Issue(
                2L, "issueTitle", "issueContent", null, Tag.FIXED, null,
                LifeCycle.NOT_STARTED, 'N', 'Y', mockProject, mockProjectMember, null, null
        );
        IssueOpinion mockIssueOpinion = new IssueOpinion(
                mockOpinionId, "opinion", 'Y', mockProjectMember, mockIssue
        );
        // Mock 의견 등록 결과 리스트
        List<OpinionInfoResponseDTO> mockOpinionResponseList = new ArrayList<>();

        // userRepository.findOneByEmail() 메서드가 mockAccessUser를 반환하도록 설정
        when(userRepository.findOneByEmail(mockAccessUserEmail)).thenReturn(Optional.of(mockAccessUser));

        // issueOpinionRepository.findById() 메서드가 mockIssueOpinion를 반환하도록 설정 (삭제할 이슈 의견 존재 여부 확인)
        when(issueOpinionRepository.findById(mockOpinionId)).thenReturn(Optional.of(mockIssueOpinion));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockProjectMember를 반환하도록 설정 (해당 의견 작성자인지 확인을 위한 멤버 정보 조회)
        when(projectMemberRepository.findByUserAndProject(mockAccessUser, mockProject)).thenReturn(Optional.of(mockProjectMember));

        // 이슈 의견 제거 서비스 호출
        List<OpinionInfoResponseDTO> result = issueService.removeIssueOpinion(mockOpinionId, mockAccessUserEmail);

        // 결과 검증
        assertIterableEquals(mockOpinionResponseList, result);

        // 각 메서드 호출됐는지 확인
        verify(userRepository, times(1)).findOneByEmail(mockAccessUserEmail);
        verify(issueOpinionRepository, times(1)).findById(mockOpinionId);
    }

    @Test
    @DisplayName("8.2 이슈 의견 삭제 - 삭제 권한이 없을 경우")
    void testRemoveIssueOpinionWithoutCommentUser() {
        // 테스트를 위한 Mock 이슈 의견 삭제 정보
        Long mockOpinionId = 1L;
        String mockAccessUserEmail = "testUser@releaser.com";

        User mockAccessUser = new User(
                "testUserName", mockAccessUserEmail, null, 'Y'
        );
        User mockCommentUser = new User(
                "testUser2Name", "testComment@releaser.com", null, 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        ProjectMember mockProjectMember = new ProjectMember(
                1L, 'M', 'Y', mockAccessUser, mockProject
        );
        ProjectMember mockCommentMember = new ProjectMember(
                2L, 'M', 'Y', mockCommentUser, mockProject

        );
        Issue mockIssue = new Issue(
                2L, "issueTitle", "issueContent", null, Tag.FIXED, null,
                LifeCycle.NOT_STARTED, 'N', 'Y', mockProject, mockProjectMember, null, null
        );
        IssueOpinion mockIssueOpinion = new IssueOpinion(
                mockOpinionId, "opinion", 'Y', mockCommentMember, mockIssue
        );

        // userRepository.findOneByEmail() 메서드가 mockAccessUser를 반환하도록 설정
        when(userRepository.findOneByEmail(mockAccessUserEmail)).thenReturn(Optional.of(mockAccessUser));

        // issueOpinionRepository.findById() 메서드가 mockIssueOpinion를 반환하도록 설정 (삭제할 이슈 의견 존재 여부 확인)
        when(issueOpinionRepository.findById(mockOpinionId)).thenReturn(Optional.of(mockIssueOpinion));

        // 예외 메시지 검증용 (해당 의견 작성자가 아닌 경우)
        String expectedExceptionMessage = String.valueOf(NOT_ISSUE_COMMENTER);

        // 이슈 의견 삭제 서비스 호출
        assertThrows(CustomException.class, () -> issueService.removeIssueOpinion(mockOpinionId, mockAccessUserEmail), expectedExceptionMessage);

        // 각 메서드 호출됐는지 확인
        verify(userRepository, times(1)).findOneByEmail(mockAccessUserEmail);
        verify(issueOpinionRepository, times(1)).findById(mockOpinionId);
    }

}