package com.momentum.releaser.domain.project.application;


import com.momentum.releaser.domain.project.dao.ProjectMemberRepository;
import com.momentum.releaser.domain.project.dao.ProjectRepository;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.project.dto.ProjectMemberResponseDto.InviteProjectMemberResponseDTO;
import com.momentum.releaser.domain.project.dto.ProjectMemberResponseDto.MembersResponseDTO;
import com.momentum.releaser.domain.release.dao.approval.ReleaseApprovalRepository;
import com.momentum.releaser.domain.release.dao.release.ReleaseRepository;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.momentum.releaser.global.config.BaseResponseStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectMemberServiceImplTest {

    private ProjectMemberService projectMemberService;
    private ProjectMemberRepository projectMemberRepository;
    private ProjectRepository projectRepository;
    private UserRepository userRepository;
    private ReleaseApprovalRepository releaseApprovalRepository;
    private ReleaseRepository releaseRepository;

    @BeforeEach
    void setUp() {
        projectMemberRepository = mock(ProjectMemberRepository.class);
        projectRepository = mock(ProjectRepository.class);
        userRepository = mock(UserRepository.class);
        releaseApprovalRepository = mock(ReleaseApprovalRepository.class);
        releaseRepository = mock(ReleaseRepository.class);
        projectMemberService = new ProjectMemberServiceImpl(
                projectMemberRepository, projectRepository, userRepository, releaseApprovalRepository, releaseRepository);
    }

    @Test
    @DisplayName("4.1 프로젝트 멤버 조회")
    void testFindProjectMembers() {
        // 테스트를 위한 Mock 프로젝트 멤버 조회 정보
        Long mockProjectId = 1L;
        String mockUserEmail = "testLeader@releaser.com";

        Project mockProject = new Project(
                mockProjectId, "projectTitle", "projectContent", "projectTeam", null, "testlinktestlink", 'Y'
        );
        User mockLeaderUser = new User(
                "testLeaderUser", mockUserEmail, null, 'Y'
        );
        User mockMemberUser = new User(
                "testMemberUser", "testMemberUser@releaser.com", null, 'Y'
        );
        ProjectMember mockAccessMember = new ProjectMember(
                1L, 'L', 'Y', mockLeaderUser, mockProject
        );
        ProjectMember mockMember = new ProjectMember(
                2L, 'M', 'Y', mockMemberUser, mockProject
        );

        // 프로젝트 멤버를 담은 리스트를 빈 리스트로 초기화
        List<ProjectMember> projectMembers = new ArrayList<>();

        // 해당 프로젝트의 멤버들 추가
        projectMembers.add(mockAccessMember);
        projectMembers.add(mockMember);

        // projectRepository.findById() 메서드가 mockProject를 반환하도록 설정
        when(projectRepository.findById(mockProjectId)).thenReturn(Optional.of(mockProject));

        // userRepository.findByEmail() 메서드가 mockLeaderUser를 반환하도록 설정 (접근 유저 정보 조회)
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockLeaderUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockAccessMember를 반환하도록 설정 (user, project 정보로 프로젝트 멤버 정보 조회)
        when(projectMemberRepository.findByUserAndProject(mockLeaderUser, mockProject)).thenReturn(Optional.of(mockAccessMember));

        // projectMemberRepository.findByProject() 메서드가 projectMembers를 반환하도록 설정 (해당 프로젝트의 멤버들 조회)
        when(projectMemberRepository.findByProject(mockProject)).thenReturn(projectMembers);

        // 프로젝트 멤버 조회 서비스 호출
        MembersResponseDTO result = projectMemberService.findProjectMembers(mockProjectId, mockUserEmail);

        // 예상된 결과와 실제 결과 비교
        assertNotNull(result);
        assertEquals(mockProject.getLink(), result.getLink());
        assertNotNull(result.getMemberList());
        assertEquals(2, result.getMemberList().size());
        assertEquals('Y', result.getMemberList().get(0).getDeleteYN());
        assertEquals('Y', result.getMemberList().get(1).getDeleteYN());

        // 각 메서드가 호출됐는지 확인
        verify(projectRepository, times(1)).findById(mockProjectId);
        verify(userRepository, times(1)).findByEmail(mockUserEmail);
        verify(projectMemberRepository, times(1)).findByUserAndProject(mockLeaderUser, mockProject);
        verify(projectMemberRepository, times(1)).findByProject(mockProject);
    }

    @Test
    @DisplayName("4.2 프로젝트 멤버 추가 - 이미 존재하는 멤버를 추가하는 경우")
    void testAddProjectMember_AlreadyExists() {
        // 테스트를 위한 mock 프로젝트 멤버 추가 정보
        String mockLink = "testLink";
        String mockUserEmail = "testUser@releaser.com";

        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, mockLink, 'Y'
        );
        User mockLeaderUser = new User(
                "testUserName", mockUserEmail, null, 'Y'
        );
        ProjectMember mockAccessMember = new ProjectMember(
                1L, 'L', 'Y', mockLeaderUser, mockProject
        );

        // projectRepository.findByLink() 메서드가 mockProject를 반환하도록 설정 (해당 link로 참여할 프로젝트 조회)
        when(projectRepository.findByLink(mockLink)).thenReturn(Optional.of(mockProject));

        // userRepository.findByEmail() 메서드가 mockLeaderUser를 반환하도록 설정
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockLeaderUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockAccessMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockLeaderUser, mockProject)).thenReturn(Optional.of(mockAccessMember));

        // 예외 메시지 검증용 (이미 존재하는 멤버일 경우)
        String expectedExceptionMessage = String.valueOf(ALREADY_EXISTS_PROJECT_MEMBER);

        // 프로젝트 멤버 추가 서비스 호출
        assertThrows(CustomException.class, () ->
            projectMemberService.addProjectMember(mockLink, mockUserEmail), expectedExceptionMessage);

        // 각 메서드가 호출 됐는지 확인
        verify(projectRepository, times(1)).findByLink(mockLink);
        verify(userRepository, times(1)).findByEmail(mockUserEmail);
        verify(projectMemberRepository, times(1)).findByUserAndProject(mockLeaderUser, mockProject);
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
        verify(releaseRepository, never()).findAllByProject(mockProject);
        verify(releaseApprovalRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("4.2 프로젝트 멤버 추가 - 새로운 멤버를 추가하는 경우")
    void testAddProjectMember_NewMember() {
        // 테스트를 위한 mock 프로젝트 멤버 추가 정보
        String mockLink = "testLink";
        String mockUserEmail = "testUser@releaser.com";

        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, mockLink, 'Y'
        );
        User mockUser = new User(
                "testUserName", mockUserEmail, null, 'Y'
        );

        // projectRepository.findByLink() 메서드가 mockProject를 반환하도록 설정 (해당 link로 참여할 프로젝트 조회)
        when(projectRepository.findByLink(mockLink)).thenReturn(Optional.of(mockProject));

        // userRepository.findByEmail() 메서드가 mockUser를 반환하도록 설정 (로그인되어 있는지 확인)
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockUser));

        // projectMemberRepository.findByUserAndProject() 메서드가 null을 반환하도록 설정 (존재하는 멤버인지 확인)
        when(projectMemberRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.empty());

        // 프로젝트 멤버 추가 서비스 호출
        InviteProjectMemberResponseDTO result = projectMemberService.addProjectMember(mockLink, mockUserEmail);

        // 예상된 결과와 실제 결과 비교
        assertNotNull(result);
        assertEquals(mockProject.getProjectId(), result.getProjectId());
        assertEquals(mockProject.getTitle(), result.getProjectName());

        // 각 메서드가 호출됐는지 확인
        verify(projectRepository, times(1)).findByLink(mockLink);
        verify(userRepository, times(1)).findByEmail(mockUserEmail);
        verify(projectMemberRepository, times(1)).findByUserAndProject(mockUser, mockProject);
        verify(projectMemberRepository, times(1)).save(any(ProjectMember.class));
        verify(releaseRepository, times(1)).findAllByProject(mockProject);
        verify(releaseApprovalRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("4.3 프로젝트 멤버 제거 - pm인 경우 삭제 가능")
    void testRemoveProjectMember() {
        // 테스트를 위한 mock 프로젝트 멤버 제거 정보
        Long mockMemberId = 1L;
        String mockEmail = "testLeader@releaser.com";

        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam",
                null, "testLink", 'Y'
        );
        User mockAccessUser = new User(
                "testUserName", mockEmail, null, 'Y'
        );
        User mockRemovedUser = new User(
                "testUserName", "remove@releaser.com", null, 'Y'
        );
        ProjectMember mockLeaderMember = new ProjectMember(
                1L, 'L', 'Y', mockAccessUser, mockProject
        );
        ProjectMember mockMember = new ProjectMember(
                2L, 'M', 'Y', mockRemovedUser, mockProject
        );

        // userRepository.findByEmail() 메서드가 mockAccessUser를 반환하도록 설정 (접근한 유저 정보 조회)
        when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.of(mockAccessUser));

        // projectMemberRepository.findById() 메서드가 mockMember를 반환하도록 설정 (제거할 멤버의 존재 여부 확인)
        when(projectMemberRepository.findById(mockMemberId)).thenReturn(Optional.of(mockMember));

        // projectMemberRepository.findByUserAndProject() 메서드가 mockLeaderMember를 반환하도록 설정
        when(projectMemberRepository.findByUserAndProject(mockAccessUser, mockProject)).thenReturn(Optional.of(mockLeaderMember));

        // 프로젝트 멤버 제거 서비스 호출
        String result = projectMemberService.removeProjectMember(mockMemberId, mockEmail);

        // 결과 검증
        assertEquals("프로젝트 멤버가 제거되었습니다.", result);

        // 각 메서드가 호출됐는지 확인
        verify(userRepository, times(1)).findByEmail(mockEmail);
        verify(projectMemberRepository, times(1)).findById(mockMemberId);
        verify(projectMemberRepository, times(1)).findByUserAndProject(mockAccessUser, mockProject);
        verify(projectMemberRepository, times(1)).deleteById(mockMember.getMemberId());
        verify(releaseApprovalRepository, times(1)).deleteByReleaseApproval();
    }

    @Test
    @DisplayName("4.3 프로젝트 멤버 제거 - member인 경우 삭제 불가능")
    void testRemoveProjectMember_Impossible() {
        // 테스트를 위한 mock 프로젝트 멤버 제거 정보
        Long mockMemberId = 1L;
        String mockEmail = "testMember@releaser.com";

        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam",
                null, "testLink", 'Y'
        );
        User mockAccessUser = new User(
                "testUserName", mockEmail, null, 'Y'
        );
        User mockRemovedUser = new User(
                "testUserName", "remove@releaser.com", null, 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                2L, 'M', 'Y', mockRemovedUser, mockProject
        );

        // userRepository.findByEmail() 메서드가 mockAccessUser를 반환하도록 설정
        when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.of(mockAccessUser));

        // projectMemberRepository.findById() 메서드가 mockMember를 반환하도록 설정
        when(projectMemberRepository.findById(mockMemberId)).thenReturn(Optional.of(mockMember));

        // 예외 메시지 검증용 (해당 프로젝트의 PM이 아닌 경우)
        String expectedExceptionMessage = String.valueOf(NOT_PROJECT_PM);

        // 프로젝트 멤버 제거 서비스 호출
        assertThrows(CustomException.class, () ->
            projectMemberService.removeProjectMember(mockMemberId, mockEmail), expectedExceptionMessage);

        // 각 메서드 호출됐는지 확인
        verify(userRepository, times(1)).findByEmail(mockEmail);
        verify(projectMemberRepository, times(1)).findById(mockMemberId);
    }

}