package com.momentum.releaser.domain.user.application;

import static com.momentum.releaser.global.common.Base64.getImageUrlFromBase64;
import static com.momentum.releaser.global.common.CommonEnum.DEFAULT_USER_PROFILE_IMG;
import static com.momentum.releaser.global.config.BaseResponseStatus.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.momentum.releaser.domain.issue.dao.IssueRepository;
import com.momentum.releaser.domain.project.dao.ProjectMemberRepository;
import com.momentum.releaser.domain.project.dao.ProjectRepository;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.release.dao.approval.ReleaseApprovalRepository;
import com.momentum.releaser.domain.release.dao.release.ReleaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.domain.user.dto.UserRequestDto.UserUpdateImgRequestDTO;
import com.momentum.releaser.domain.user.dto.UserResponseDto.UserProfileImgResponseDTO;
import com.momentum.releaser.domain.user.mapper.UserMapper;
import com.momentum.releaser.global.config.aws.S3Upload;
import com.momentum.releaser.global.exception.CustomException;

/**
 * 사용자 관리와 관련된 기능을 제공하는 서비스 구현 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ReleaseApprovalRepository releaseApprovalRepository;
    private final S3Upload s3Upload;

    /**
     * 1.1 사용자 프로필 이미지 조회
     *
     * @author seonwoo
     * @date 2023-07-12
     * @param userEmail 조회할 사용자의 이메일 주소
     */
    @Override
    public UserProfileImgResponseDTO findUserProfileImg(String userEmail) {
        // 이메일로 사용자 정보를 조회, 사용자 프로필 이미지 정보로 변환하여 반환
        User user = getUserByEmail(userEmail);
        return UserMapper.INSTANCE.toUserProfileImgResponseDto(user);
    }

    /**
     * 1.2 사용자 프로필 이미지 변경
     *
     * @author seonwoo
     * @date 2023-07-31 (월)
     * @param userEmail 사용자 이메일
     * @throws IOException 파일 입출력 관련 예외
     */
    @Transactional
    @Override
    public UserProfileImgResponseDTO modifyUserProfileImg(String userEmail, UserUpdateImgRequestDTO userUpdateImgRequestDto) throws IOException {
        // 사용자 식별 번호로 사용자 정보 조회
        User user = getUserByEmail(userEmail);
        // 기존 프로필 이미지가 있을 경우 삭제
        deleteIfExistProfileImg(user);
        user.updateImg(uploadUserProfileImg(userUpdateImgRequestDto));
        return UserMapper.INSTANCE.toUserProfileImgResponseDto(user);
    }

    /**
     * 1.3 사용자 프로필 이미지 삭제
     *
     * @author seonwoo
     * @date 2023-07-12
     * @param userEmail 사용자 이메일
     */
    @Override
    public UserProfileImgResponseDTO removeUserProfileImg(String userEmail) {
        // 사용자 식별 번호로 사용자 정보 조회
        User user = getUserByEmail(userEmail);
        // 프로필 이미지가 있을 경우 삭제
        deleteIfExistProfileImg(user);
        // 프로필 이미지를 삭제한 후 기본 이미지로 저장
        saveAfterDeleteProfileImg(user);
        return UserMapper.INSTANCE.toUserProfileImgResponseDto(user);
    }

    /**
     * 1.4 사용자 탈퇴
     *
     * @author chaeanna
     * @date 2023-08-13
     * @param userEmail 사용자 이메일
     */
    @Override
    @Transactional
    public String removeUser(String userEmail) {
        // 사용자 식별 번호로 사용자 정보 조회
        User user = getUserByEmail(userEmail);

        // 관리자인 프로젝트가 있는지 체크
        List<ProjectMember> members = checkProjectPMWithUser(user);

        // 참여 중인 프로젝트 탈퇴하기
        withdrawProject(members);

        return deleteUser(user);
    }

    // =================================================================================================================

    /**
     * 사용자 식별 번호를 이용해 사용자 엔티티를 가져온다.
     *
     * @author seonwoo
     * @date 2023-07-11
     * @param userId 사용자 식별 번호
     * @return User 사용자 엔티티
     * @throws CustomException 사용자 정보가 존재하지 않을 경우 예외 발생
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_USER));
    }

    /**
     * 사용자 이메일을 이용해 사용자 엔티티를 가져온다.
     *
     * @author seonwoo
     * @date 2023-07-12
     * @param email 사용자 이메일 주소
     * @return User 사용자 엔티티
     * @throws CustomException 사용자 정보가 존재하지 않을 경우 예외 발생
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_USER));
    }

    /**
     * 사용자로부터 받은 프로필 이미지를 S3에 업로드한다.
     *
     * @author seonwoo
     * @date 2023-07-11
     * @param userUpdateImgRequestDto 사용자로부터 받은 프로필 이미지 정보
     * @return String 업로드된 이미지의 S3 URL
     * @throws IOException 이미지 파일 처리 중 예외 발생
     * @throws CustomException 프로필 이미지 업로드에 실패한 경우 예외 발생
     */
    private String uploadUserProfileImg(UserUpdateImgRequestDTO userUpdateImgRequestDto) throws IOException {
        String img = userUpdateImgRequestDto.getImage();

        if (img.isEmpty()) {
            // 만약 사용자로부터 받은 이미지 데이터가 없는 경우 기본 프로필로 대체한다.
            return DEFAULT_USER_PROFILE_IMG.url();
        }

        // Base64로 인코딩된 이미지 파일을 파일 형태로 가져온다.
        File file = getImageUrlFromBase64(img);

        String url = s3Upload.upload(file, file.getName(), "users");

        if (file.delete()) {
            return url;
        } else {
            throw new CustomException(FAILED_TO_UPDATE_USER_PROFILE_IMG);
        }
    }

    /**
     * 사용자의 이미지 값이 null이 아닌 경우 한 번 지운다.
     *
     * @author seonwoo
     * @date 2023-07-11
     * @param user 사용자 엔티티
     */
    private void deleteIfExistProfileImg(User user) {
        // 사용자의 프로필 이미지가 기본 이미지도, null도 아닌 경우 기존에 저장된 파일을 S3에서 삭제한다.
        if (!Objects.equals(user.getImg(), DEFAULT_USER_PROFILE_IMG.url()) && user.getImg() != null) {
            String img = user.getImg();

            if (img.length() > 55) {
                s3Upload.delete(user.getImg().substring(55));
            }

        }
    }

    /**
     * 사용자의 프로필 이미지 파일을 S3에서 삭제한 후 데이터베이스에서 값을 지우는 메서드입니다.
     *
     * @author seonwoo
     * @date 2023-07-12
     * @param user 프로필 이미지를 삭제할 사용자 엔티티
     */
    private void saveAfterDeleteProfileImg(User user) {
        user.updateImg(DEFAULT_USER_PROFILE_IMG.url());
        userRepository.save(user);
    }

    /**
     * 해당 유저가 관리자로 있는 프로젝트가 존재할 경우 예외를 발생시키는 메서드입니다.
     *
     * @param user 탈퇴하려는 사용자
     * @date 2023-08-13
     * @author chaeanna
     */
    private List<ProjectMember> checkProjectPMWithUser(User user) {
        // 탈퇴하려는 사용자가 참여중인 프로젝트 멤버 정보 조회
        List<ProjectMember> pm = projectMemberRepository.findByUser(user);

        // 사용자가 어떤 프로젝트에도 참여중이지 않을 경우
        if (pm.isEmpty()) {
            return null;
        }

        // 멤버 역할이 PM일 경우 예외 발생
        for (ProjectMember projectMember : pm) {
            if (projectMember.getPosition() == 'L') {
                throw new CustomException(PROJECT_DELETION_REQUIRED_FOR_USER_WITHDRAWAL);
            }
        }

        return pm;
    }

    /**
     * 참여 중인 프로젝트 탈퇴하기
     *
     * @param members 탈퇴하려는 멤버
     * @date 2023-08-13
     * @author chaeanna
     */
    private void withdrawProject(List<ProjectMember> members) {
        // 사용자가 프로젝트에 참여 중이지 않아서 탈퇴할 프로젝트가 없는 경우
        if (members == null) {
            return;
        }

        for (ProjectMember member : members) {
            // project member status = 'N' 변경
            projectMemberRepository.deleteById(member.getMemberId());
            // approval 삭제
            releaseApprovalRepository.deleteByReleaseApproval();
        }
    }

    /**
     * 사용자와 프로젝트를 기반으로 프로젝트 멤버 조회
     *
     * @author chaeanna
     * @date 2023-08-13
     * @param user 사용자 엔티티
     * @param project 프로젝트 엔티티
     * @return ProjectMember 조회된 프로젝트 멤버 엔티티
     */
    private ProjectMember findProjectMemberByUserAndProject(User user, Project project) {
        // 사용자와 프로젝트를 매개변수로 하여 프로젝트 멤버를 조회합니다.
        return projectMemberRepository.findByUserAndProject(user, project).orElseThrow(() -> new CustomException(NOT_EXISTS_PROJECT_MEMBER));
    }

    /**
     * 사용자 탈퇴
     *
     * @param user 탈퇴하려는 사용자
     * @return String "탈퇴가 완료되었습니다."
     * @date 2023-08-13
     * @author chaeanna
     */
    private String deleteUser(User user) {
        userRepository.deleteById(user.getUserId());

        return "탈퇴가 완료되었습니다.";
    }

}
