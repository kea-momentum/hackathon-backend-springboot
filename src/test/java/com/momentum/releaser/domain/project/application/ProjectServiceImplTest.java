package com.momentum.releaser.domain.project.application;

import static com.momentum.releaser.global.config.BaseResponseStatus.NOT_PROJECT_PM;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.momentum.releaser.redis.RedisUtil;
import com.momentum.releaser.redis.notification.NotificationRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import com.momentum.releaser.domain.issue.dao.IssueRepository;
import com.momentum.releaser.domain.project.dao.ProjectMemberRepository;
import com.momentum.releaser.domain.project.dao.ProjectRepository;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.project.dto.ProjectDataDto.GetProjectDataDTO;
import com.momentum.releaser.domain.project.dto.ProjectRequestDto.FilterIssueRequestDTO;
import com.momentum.releaser.domain.project.dto.ProjectRequestDto.FilterReleaseRequestDTO;
import com.momentum.releaser.domain.project.dto.ProjectRequestDto.ProjectInfoRequestDTO;
import com.momentum.releaser.domain.project.dto.ProjectResponseDto.GetProjectResponseDTO;
import com.momentum.releaser.domain.project.dto.ProjectResponseDto.ProjectInfoResponseDTO;
import com.momentum.releaser.domain.project.dto.ProjectResponseDto.ProjectSearchResponseDTO;
import com.momentum.releaser.domain.release.dao.approval.ReleaseApprovalRepository;
import com.momentum.releaser.domain.release.dao.release.ReleaseRepository;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.global.config.aws.S3Upload;
import com.momentum.releaser.global.exception.CustomException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

class ProjectServiceImplTest {

    private ProjectServiceImpl projectService;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository projectMemberRepository;
    private UserRepository userRepository;
    private IssueRepository issueRepository;
    private ReleaseRepository releaseRepository;
    private ReleaseApprovalRepository releaseApprovalRepository;
    private ModelMapper modelMapper;
    private S3Upload s3Upload;

    private RedisUtil redisUtil;
    private NotificationRedisRepository notificationRedisRepository;
    private AmqpAdmin rabbitAdmin;
    private DirectExchange projectDirectExchange;
    private ConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        issueRepository = mock(IssueRepository.class);
        releaseRepository = mock(ReleaseRepository.class);
        releaseApprovalRepository = mock(ReleaseApprovalRepository.class);
        projectMemberRepository = mock(ProjectMemberRepository.class);
        userRepository = mock(UserRepository.class);
        modelMapper = new ModelMapper(); // modelMapper 초기화
        s3Upload = mock(S3Upload.class);
        redisUtil = mock(RedisUtil.class);
        notificationRedisRepository = mock(NotificationRedisRepository.class);
        rabbitAdmin = mock(AmqpAdmin.class);
        projectDirectExchange = mock(DirectExchange.class);
        connectionFactory = mock(ConnectionFactory.class);
        projectService = new ProjectServiceImpl(
                projectRepository, projectMemberRepository, userRepository, issueRepository, releaseRepository, releaseApprovalRepository, modelMapper, s3Upload,
                redisUtil, notificationRedisRepository, rabbitAdmin, projectDirectExchange, connectionFactory);
    }

    @Test
    @DisplayName("3.1 프로젝트 생성")
    void testAddProject() throws IOException {
        // 테스트를 위한 mock 프로젝트 생성 정보
        String mockUserEmail = "test@releaser.com";

        User mockUser = new User(
                "userName", mockUserEmail, null, 'Y'
        );
        ProjectInfoRequestDTO mockReqDTO = new ProjectInfoRequestDTO(
                "project Title", "project Content", "project Team", null
        );
        Project mockProject = new Project(
                1L, "project Title", "project Content", "project Team", "s3Url", "testLink", 'Y'
        );

        // userRepository.findByEmail() 메서드가 mockUser를 반환하도록 설정 (접근한 유저 체크)
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockUser));

        // s3Upload.upload() 메서드가 S3 URL 반환하도록 설정 (이미지를 S3 URL로 변환 과정)
        when(s3Upload.upload(any(), anyString(), anyString())).thenReturn("s3Url");

        // projectRepository.save() 메서드가 mockProject를 반환하도록 설정 (프로젝트 정보를 바탕으로 프로젝트 생성)
        when(projectRepository.save(any())).thenReturn(mockProject);

        // 프로젝트 생성 서비스 호출
        ProjectInfoResponseDTO result = projectService.addProject(mockUserEmail, mockReqDTO);

        // 실제 결과가 null인지 체크
        assertNotNull(result);

        // 각 메서드가 호출 됐는지 확인
        verify(userRepository, times(1)).findByEmail(mockUserEmail);
        verify(projectRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("3.2 프로젝트 수정 - 프로젝트 PM이 수정한 경우")
    void testModifyProjectWithPM() throws IOException {
        // 테스트를 위한 mock 프로젝트 생성 정보
        Long mockProjectId = 1L;
        String mockUserEmail = "test@releaser.com";

        ProjectInfoRequestDTO mockReqDTO = new ProjectInfoRequestDTO(
                "project Update Title", "project Update Content", "project Team", null
        );
        User mockUser = new User(
                "pmUserName", mockUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                mockProjectId, "project Title", "project Content", "project Team", "s3Url", "testLink", 'Y'
        );
        ProjectMember mockPM = new ProjectMember(
                1L, 'L', 'Y', mockUser, mockProject
        );
        List<ProjectMember> memberList = new ArrayList<>();
        memberList.add(mockPM);

        // projectRepository.findById() 메서드가 mockProject를 반환하도록 설정
        when(projectRepository.findById(mockProjectId)).thenReturn(Optional.of(mockProject));

        // userRepository.findByEmail() 메서드가 mockUser를 반환하도록 설정 (접근한 유저 정보 설정)
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockUser));

        // projectMemberRepository.findByProject() 메서드가 memberList를 반환하도록 설정 (해당 프로젝트의 멤버 리스트를 설정)
        when(projectMemberRepository.findByProject(mockProject)).thenReturn(memberList);

        // s3Upload.upload() 메서드가 S3URL로 반환하도록 설정 (요청받은 이미지를 s3URL로 변환하는 과정)
        when(s3Upload.upload(any(), anyString(), anyString())).thenReturn("s3Url");

        // projectRepository.save() 메서드가 mockProject를 반환하도록 설정 (요청 받은 프로젝트 정보를 바탕으로 프로젝트 수정)
        when(projectRepository.save(any())).thenReturn(mockProject);

        // 프로젝트 수정 서비스 호출
        ProjectInfoResponseDTO result = projectService.modifyProject(mockProjectId, mockUserEmail, mockReqDTO);

        // 실제 결과가 존재하는 지 체크
        assertNotNull(result);

        // 각 메서드가 호출 됐는지 확인
        verify(projectRepository, times(1)).findById(mockProjectId);
        verify(userRepository, times(1)).findByEmail(mockUserEmail);
        verify(projectMemberRepository, times(1)).findByProject(mockProject);
        verify(projectRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("3.2 프로젝트 수정 - PM이 아닌 다른 멤버가 수정한 경우")
    void testModifyProjectWithoutPM() throws IOException {
        // 테스트를 위한 mock 프로젝트 수정 정보
        Long mockProjectId = 1L;
        String mockUserEmail = "testMember@releaser.com";

        ProjectInfoRequestDTO mockReqDTO = new ProjectInfoRequestDTO(
                "project Update Title", "project Update Content", "project Team", null
        );
        User mockMemberUser = new User(
                "memberUserName", mockUserEmail, null, 'Y'
        );
        Project mockProject = new Project(
                mockProjectId, "project Title", "project Content", "project Team", "s3Url", "testLink", 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                2L, 'M', 'Y', mockMemberUser, mockProject
        );
        // 해당 프로젝트 멤버 리스트
        List<ProjectMember> memberList = new ArrayList<>();
        memberList.add(mockMember);

        // projectRepository.findById() 메서드가 mockProject를 반환하도록 설정
        when(projectRepository.findById(mockProjectId)).thenReturn(Optional.of(mockProject));

        // userRepository.findByEmail() 메서드가 mockMemberUser를 반환하도록 설정 (접근한 유저의 정보)
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockMemberUser));

        // projectMemberRepository.findByProject() 메서드가 memberList를 반환하도록 설정 (해당 프로젝트의 멤버 리스트)
        when(projectMemberRepository.findByProject(mockProject)).thenReturn(memberList);

        // 예외 메시지 검증용
        String expectedExceptionMessage = String.valueOf(NOT_PROJECT_PM);

        // 테스트 실행 및 예외 검증 (PM이 아닌 멤버가 수정할 경우 예외 발생)
        assertThrows(CustomException.class, () -> projectService.modifyProject(mockProjectId, mockUserEmail, mockReqDTO), expectedExceptionMessage);

        // 각 메서드가 호출 됐는지 확인
        verify(projectRepository, times(1)).findById(mockProjectId);
        verify(userRepository, times(1)).findByEmail(mockUserEmail);
        verify(projectMemberRepository, times(1)).findByProject(mockProject);
    }

    @Test
    @DisplayName("3.3 프로젝트 삭제")
    void testRemoveProject() {
        // 테스트를 위한 mock 프로젝트 삭제 정보
        Long mockProjectId = 123L;

        Project mockProject = new Project(
                mockProjectId, "project Title", "project Content", "project Team", null, "testLink", 'Y'
        );

        // projectRepository.findById() 메서드가 mockProject를 반환하도록 설정
        when(projectRepository.findById(mockProjectId)).thenReturn(Optional.of(mockProject));

        // 프로젝트 삭제 서비스 호출
        String result = projectService.removeProject(mockProjectId);

        // 결과 검증
        assertEquals("프로젝트가 삭제되었습니다.", result);

        // 각 메서드가 호출됐는지 확인
        verify(projectRepository, times(1)).findById(mockProjectId);
        verify(projectRepository, times(1)).deleteById(mockProject.getProjectId());
        verify(issueRepository, times(1)).deleteByIssueNum();
        verify(releaseApprovalRepository, times(1)).deleteByReleaseApproval();
    }

    @Test
    @DisplayName("3.4 프로젝트 조회")
    void testFindProjects() {
        // 테스트를 위한 mock 프로젝트 조회 정보
        String mockUserEmail = "test@releaser.com";

        User mockUser = new User(
                "Test User", mockUserEmail, null, 'Y'
        );
        Project mockLeaderProject = new Project(
                1L, "test project1Title", "test project1Content", "test project1Team",
                null, "testLink", 'Y'
        );
        Project mockMemberProject = new Project(
                2L, "test project1Title", "test project1Content", "test project1Team",
                null, "testLink", 'Y'
        );
        ProjectMember mockLeaderMember = new ProjectMember(
                1L, 'L', 'Y', mockUser, mockLeaderProject
        );
        ProjectMember mockMember = new ProjectMember(
                2L, 'M', 'Y', mockUser, mockMemberProject
        );

        // 프로젝트 멤버를 담을 리스트를 빈 리스트로 초기화
        List<ProjectMember> projectMemberList = new ArrayList<>();

        // 해당 유저가 참여하고 있는 프로젝트의 리스트에 멤버 정보 담기
        projectMemberList.add(mockLeaderMember);
        projectMemberList.add(mockMember);

        // userRepository.findByEmail() 메서드가 mockUser 반환하도록 설정
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockUser));

        // projectMemberRepository.findByUser() 메서드가 projectMemberList를 반환하도록 설정 (해당 유저의 참여 중인 프로젝트의 멤버 정보 조회)
        when(projectMemberRepository.findByUser(mockUser)).thenReturn(projectMemberList);

        // 프로젝트 조회 서비스 호출
        GetProjectResponseDTO result = projectService.findProjects(mockUserEmail);

        // 예상되는 GetProjectResponseDTO 객체 생성
        List<GetProjectDataDTO> expectedGetCreateProjectList = new ArrayList<>();
        List<GetProjectDataDTO> expectedGetEnterProjectList = new ArrayList<>();

        expectedGetCreateProjectList.add(modelMapper.map(mockLeaderProject, GetProjectDataDTO.class));
        expectedGetEnterProjectList.add(modelMapper.map(mockMemberProject, GetProjectDataDTO.class));

        GetProjectResponseDTO expectedResponse = GetProjectResponseDTO.builder()
                .getCreateProjectList(expectedGetCreateProjectList)
                .getEnterProjectList(expectedGetEnterProjectList)
                .build();

        // 예상된 결과와 실제 결과 비교
        assertEquals(expectedResponse.getGetCreateProjectList().size(), result.getGetCreateProjectList().size());
        assertEquals(expectedResponse.getGetEnterProjectList().size(), result.getGetEnterProjectList().size());

        // 각 메서드가 호출됐는지 확인
        verify(userRepository, times(1)).findByEmail(mockUserEmail);
        verify(projectMemberRepository, times(1)).findByUser(mockUser);
    }

    @Test
    @DisplayName("10.1 프로젝트 내 통합검색 - 이슈 검색")
    void testFindIssueSearch() {
        // 테스트를 위한 mock 검색 정보
        Long mockProjectId = 1L;
        String mockFilterType = "issue";

        FilterIssueRequestDTO mockIssueReqDTO = new FilterIssueRequestDTO(
                Date.valueOf("2023-08-01"), Date.valueOf("2023-08-09"), 1L,
                "1.0.0", "1.2.0",
                "NEW", "title"
        );
        User mockUser = new User(
                "userName", "test@releaser.com", null, 'Y'

        );
        Project mockProject = new Project(
                mockProjectId, "project Title", "project Content", "project Team", null, "testLink", 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                1L, 'L', 'Y', mockUser, mockProject
        );

        // projectRepository.getProjectMemberPostionPM() 메서드가 mockMember를 반환하도록 설정 (해당 프로젝트의 PM 정보 조회)
        when(projectRepository.getProjectMemberPostionPM(mockProjectId)).thenReturn(mockMember);

        // 검색 서비스 호출
        ProjectSearchResponseDTO result = projectService.findProjectSearch(mockProjectId, mockFilterType, mockIssueReqDTO, null);

        // 결과가 null인지 확인
        assertNotNull(result);

        // 각 메서드가 호출 됐는지 확인
        verify(projectRepository, times(1)).getProjectMemberPostionPM(mockProjectId);
    }

    @Test
    @DisplayName("10.1 프로젝트 내 통합검색 - 릴리즈 검색")
    void testFindReleaseSearch() {
        // 테스트를 위한 mock 검색 정보
        Long mockProjectId = 1L;
        String mockFilterType = "release";

        FilterReleaseRequestDTO mockReleaseReqDTO = new FilterReleaseRequestDTO(
                "1.0.0", "2.0.0", "Title"
        );
        User mockUser = new User(
                "userName", "test@releaser.com", null, 'Y'

        );
        Project mockProject = new Project(
                mockProjectId, "project Title", "project Content", "project Team", null, "testLink", 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                1L, 'L', 'Y', mockUser, mockProject
        );

        // projectRepository.getProjectMemberPostionPM() 메서드가 mockMember를 반환하도록 설정 (해당 프로젝트의 PM 정보 조회)
        when(projectRepository.getProjectMemberPostionPM(mockProjectId)).thenReturn(mockMember);

        // 검색 서비스 호출
        ProjectSearchResponseDTO result = projectService.findProjectSearch(mockProjectId, mockFilterType, null, mockReleaseReqDTO);

        // 결과가 null인지 확인
        assertNotNull(result);

        // 각 메서드가 호출됐는지 확인
        verify(projectRepository, times(1)).getProjectMemberPostionPM(mockProjectId);
    }


}





