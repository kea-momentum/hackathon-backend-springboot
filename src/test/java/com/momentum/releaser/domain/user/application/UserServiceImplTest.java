package com.momentum.releaser.domain.user.application;

import static com.momentum.releaser.global.config.BaseResponseStatus.PROJECT_DELETION_REQUIRED_FOR_USER_WITHDRAWAL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.momentum.releaser.domain.project.dao.ProjectMemberRepository;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.release.dao.approval.ReleaseApprovalRepository;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.domain.user.dto.UserRequestDto.UserUpdateImgRequestDTO;
import com.momentum.releaser.domain.user.dto.UserResponseDto.UserProfileImgResponseDTO;
import com.momentum.releaser.global.config.aws.S3Upload;
import com.momentum.releaser.global.exception.CustomException;

class UserServiceImplTest {
    
    private UserServiceImpl userService;
    private UserRepository userRepository;
    private ProjectMemberRepository projectMemberRepository;
    private ReleaseApprovalRepository releaseApprovalRepository;
    private S3Upload s3Upload;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        projectMemberRepository = mock(ProjectMemberRepository.class);
        releaseApprovalRepository = mock(ReleaseApprovalRepository.class);
        s3Upload = mock(S3Upload.class);
        userService = new UserServiceImpl(
                userRepository, projectMemberRepository, releaseApprovalRepository, s3Upload
        );
    }

    @Test
    @DisplayName("1.2 사용자 프로필 이미지 변경")
    void testModifyUserProfileImg() throws IOException {
        // 테스트를 위한 mock 사용자 프로필 이미지 변경 정보
        String mockUserEmail = "test@releaser.com";

        UserUpdateImgRequestDTO mockReqDTO = new UserUpdateImgRequestDTO(
                "data:image/jpeg;base64,imgURL.jpeg"
        );
        User mockUser = new User(
                "testUser1Name", mockUserEmail, "data:image/jpeg;base64,img.jpeg", 'Y'
        );

        // userRepository.findByEmail() 메서드가 mockUser를 반환하도록 설정
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockUser));

        // 사용자 프로필 이미지 변경 서비스 호출
        UserProfileImgResponseDTO result = userService.modifyUserProfileImg(mockUserEmail, mockReqDTO);

        // 결과 검증
        assertNotNull(result);

        // 각 메서드 호출됐는지 확인
        verify(userRepository, times(1)).findByEmail(mockUserEmail);
    }

    @Test
    @DisplayName("1.4 사용자 탈퇴")
    void testRemoveUser() {
        // 테스트를 위한 mock 사용자 탈퇴 정보
        String mockUserEmail = "test@releaser.com";

        User mockUser = new User(
                "testUser1Name", mockUserEmail, "data:image/jpeg;base64,img.jpeg", 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        Project mockProject2 = new Project(
                2L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                1L, 'M', 'Y', mockUser, mockProject

        );
        ProjectMember mockMember2 = new ProjectMember(
                2L, 'M', 'Y', mockUser, mockProject2

        );
        // 해당 유저가 참여중인 프로젝트 리스트 초기화
        List<ProjectMember> mockProjectList = new ArrayList<>();
        // 프로젝트 리스트에 추가
        mockProjectList.add(mockMember);
        mockProjectList.add(mockMember2);

        // userRepository.findByEmail() 메서드가 mockUser를 반환하도록 설정
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockUser));

        // projectMemberRepository.findByUser() 메서드가 mockProjectList를 반환하도록 설정 (참여 중인 프로젝트 리스트)
        when(projectMemberRepository.findByUser(mockUser)).thenReturn(mockProjectList);

        // 사용자 탈퇴 서비스 호출
        String result = userService.removeUser(mockUserEmail);

        // 결과 검증
        assertEquals("탈퇴가 완료되었습니다.", result);

        // 각 메서드 호출됐는지 확인
        verify(userRepository, times(1)).findByEmail(mockUserEmail);
        verify(projectMemberRepository, times(1)).findByUser(mockUser);
    }

    @Test
    @DisplayName("1.4 사용자 탈퇴 - 관리자인 프로젝트가 존재할 경우 예외 발생")
    void testRemoveUser_AlreadyExistsPMProject() {
        // 테스트를 위한 mock 사용자 탈퇴 정보
        String mockUserEmail = "test@releaser.com";

        User mockUser = new User(
                "testUser1Name", mockUserEmail, "data:image/jpeg;base64,img.jpeg", 'Y'
        );
        Project mockProject = new Project(
                1L, "projectTitle", "projectContent", "projectTeam", null, "testLink", 'Y'
        );
        ProjectMember mockMember = new ProjectMember(
                1L, 'L', 'Y', mockUser, mockProject

        );
        // 해당 유저가 참여중인 프로젝트 리스트 초기화
        List<ProjectMember> mockProjectList = new ArrayList<>();
        // 프로젝트 리스트에 추가
        mockProjectList.add(mockMember);

        // userRepository.findByEmail() 메서드가 mockUser를 반환하도록 설정
        when(userRepository.findByEmail(mockUserEmail)).thenReturn(Optional.of(mockUser));

        // projectMemberRepository.findByUser() 메서드가 mockProjectList를 반환하도록 설정 (참여 중인 프로젝트 리스트)
        when(projectMemberRepository.findByUser(mockUser)).thenReturn(mockProjectList);

        // 예외 메시지 검증용
        String expectedExceptionMessage = String.valueOf(PROJECT_DELETION_REQUIRED_FOR_USER_WITHDRAWAL);

        // 사용자 탈퇴 서비스 호출 (관리자인 프로젝트가 존재할 경우 예외 발생)
        assertThrows(CustomException.class, () -> userService.removeUser(mockUserEmail), expectedExceptionMessage);

        // 각 메서드 호출됐는지 확인
        verify(userRepository, times(1)).findByEmail(mockUserEmail);
        verify(projectMemberRepository, times(1)).findByUser(mockUser);
    }

}