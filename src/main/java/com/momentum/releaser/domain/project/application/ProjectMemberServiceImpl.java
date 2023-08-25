package com.momentum.releaser.domain.project.application;

import static com.momentum.releaser.global.config.BaseResponseStatus.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import com.momentum.releaser.domain.project.dao.ProjectMemberRepository;
import com.momentum.releaser.domain.project.dao.ProjectRepository;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.project.dto.ProjectMemberDataDto.ProjectMemberInfoDTO;
import com.momentum.releaser.domain.project.dto.ProjectMemberResponseDto.InviteProjectMemberResponseDTO;
import com.momentum.releaser.domain.project.dto.ProjectMemberResponseDto.MembersResponseDTO;
import com.momentum.releaser.domain.project.mapper.ProjectMemberMapper;
import com.momentum.releaser.domain.release.dao.approval.ReleaseApprovalRepository;
import com.momentum.releaser.domain.release.dao.release.ReleaseRepository;
import com.momentum.releaser.domain.release.domain.ReleaseApproval;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.global.exception.CustomException;

/**
 * 프로젝트 멤버와 관련된 기능을 제공하는 서비스 구현 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ReleaseApprovalRepository releaseApprovalRepository;
    private final ReleaseRepository releaseRepository;

    /**
     * 4.1 프로젝트 멤버 조회
     *
     * @author chaeanna
     * @date 2023-07-05
     * @param email 사용자의 이메일
     */
    @Override
    @Transactional
    public MembersResponseDTO findProjectMembers(Long projectId, String email) {
        // Token UserInfo
        User user = getUserByEmail(email);
        Project project = getProjectById(projectId);

        ProjectMember accessMember = findProjectMemberByUserAndProject(user, project);

        List<ProjectMemberInfoDTO> memberList = getProjectMembersRes(project, accessMember);

        MembersResponseDTO getMembersRes = MembersResponseDTO.builder()
                .link(project.getLink())
                .memberList(memberList)
                .build();

        return getMembersRes;
    }

    /**
     * 4.2 프로젝트 멤버 추가
     *
     * @author chaenna
     * @date 2023-07-20
     * @param email 사용자의 이메일
     */
    @Override
    @Transactional
    public InviteProjectMemberResponseDTO addProjectMember(String link, String email) {
        // Token UserInfo
        User user = getUserByEmail(email);

        // link check
        Project project = getProjectByLink(link);

        InviteProjectMemberResponseDTO res = InviteProjectMemberResponseDTO.builder()
                .projectId(project.getProjectId())
                .projectName(project.getTitle())
                .build();

        // projectMember 존재여부 확인
        if (isProjectMember(user, project)) {
            throw new CustomException(ALREADY_EXISTS_PROJECT_MEMBER, res);
        }
        // member 추가
        ProjectMember member = addProjectMember(project, user);
        // approval 추가
        addReleaseApprovalsForProjectMember(member, project);
        return res;
    }

    /**
     * 4.3 프로젝트 멤버 제거
     *
     * @author chaeanna
     * @date 2023-07-05
     * @param email 사용자의 이메일
     */
    @Override
    @Transactional
    public String removeProjectMember(Long memberId, String email) {
        User user = getUserByEmail(email);
        ProjectMember projectMember = getProjectMemberById(memberId);

        if (!isProjectLeader(user, projectMember.getProject())) {
            throw new CustomException(NOT_PROJECT_PM);
        }

        projectMemberRepository.deleteById(projectMember.getMemberId());
        releaseApprovalRepository.deleteByReleaseApproval();

        return "프로젝트 멤버가 제거되었습니다.";
    }

    /**
     * 4.4 프로젝트 멤버 탈퇴
     *
     * @author chaeanna
     * @date 2023-07-08
     * @param email 사용자의 이메일
     */
    @Override
    @Transactional
    public String removeWithdrawProjectMember(Long projectId, String email) {
        User user = getUserByEmail(email);

        Project project = getProjectById(projectId);

        // project member 찾기
        ProjectMember member = findProjectMemberByUserAndProject(user, project);
        // project member status = 'N'
        projectMemberRepository.deleteById(member.getMemberId());
        // approval delete
        releaseApprovalRepository.deleteByReleaseApproval();

        return "프로젝트 탈퇴가 완료되었습니다.";
    }

    // =================================================================================================================

    /**
     * email로 User 조회
     *
     * @author chaeanna
     * @date 2023-07-05
     * @param email 사용자의 이메일
     * @return User 조회된 유저 엔티티
     * @throws CustomException 사용자가 존재하지 않을 경우 예외 발생
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new CustomException(NOT_EXISTS_USER));
    }

    /**
     * link로 Project 조회
     *
     * @author chaeanna
     * @date 2023-07-20
     * @param link 프로젝트 가입 링크
     * @return Project 조회된 프로젝트 엔티티
     * @throws CustomException 프로젝트가 존재하지 않을 경우 예외 발생
     */
    private Project getProjectByLink(String link) {
        return projectRepository.findByLink(link).orElseThrow(() -> new CustomException(NOT_EXISTS_LINK));
    }

    /**
     * projectId로 Project 조회
     *
     * @author chaeanna
     * @date 2023-07-05
     * @param projectId 프로젝트 식별 번호
     * @return Project 조회된 프로젝트 엔티티
     * @throws CustomException 프로젝트가 존재하지 않을 경우 예외 발생
     */
    private Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_PROJECT));
    }

    /**
     * 프로젝트 멤버 조회 결과 생성
     *
     * @author chaeanna
     * @date 2023-07-05
     * @param project 프로젝트 엔티티
     * @param accessMember 접근한 프로젝트 멤버 엔티티
     * @return List<ProjectMemberInfoDTO> 프로젝트 멤버 정보 목록 DTO
     */
    private List<ProjectMemberInfoDTO> getProjectMembersRes(Project project, ProjectMember accessMember) {
        // accessMember의 직책 (L : 리더, M : 멤버)
        char position = accessMember.getPosition();

        // accessMember가 리더인 경우 deleteYN을 'Y', 그렇지 않은 경우 'N'
        char deleteYN = (position == 'L') ? 'Y' : 'N';

        // 모든 프로젝트 멤버를 가져와 ProjectMemberInfoDTO 매핑
        return projectMemberRepository.findByProject(project)
                .stream()
                .map(member -> {
                    ProjectMemberInfoDTO membersRes = ProjectMemberMapper.INSTANCE.toGetMembersRes(member);
                    membersRes.setDeleteYN(deleteYN);
                    return membersRes;
                })
                .collect(Collectors.toList());
    }

    /**
     * 해당 사용자가 프로젝트 멤버인지 여부 확인
     *
     * @author chaeanna
     * @date 2023-07-20
     * @param user 사용자 엔티티
     * @param project 프로젝트 엔티티
     * @return 사용자가 프로젝트 멤버인 경우 true, 아닌 경우 false 반환
     */
    private boolean isProjectMember(User user, Project project) {
        return projectMemberRepository.findByUserAndProject(user, project).isPresent();
    }

    /**
     * 프로젝트 멤버를 추가
     *
     * @author chaeanna
     * @date 2023-07-20
     * @param project 프로젝트 엔티티
     * @param user 사용자 엔티티
     * @return ProjectMember 추가된 프로젝트 멤버
     */
    private ProjectMember addProjectMember(Project project, User user) {
        // 'M'(Member)인 새로운 프로젝트 멤버 생성
        ProjectMember projectMember = ProjectMember.builder()
                .position('M')
                .user(user)
                .project(project)
                .build();

        return projectMemberRepository.save(projectMember);
    }

    /**
     * 프로젝트 멤버에 대한 릴리스 승인 정보를 추가
     *
     * @author chaeanna
     * @date 2023-07-20
     * @param member 프로젝트 멤버 엔티티
     * @param project 프로젝트 엔티티
     */
    private void addReleaseApprovalsForProjectMember(ProjectMember member, Project project) {
        // 프로젝트에 속한 모든 릴리스 노트 조회
        List<ReleaseNote> releaseNotes = releaseRepository.findAllByProject(project);
        if (releaseNotes != null) {

            // 각 릴리스 노트에 대해 프로젝트 멤버와 관련된 ReleaseApproval을 생성하여 저장
            List<ReleaseApproval> releaseApprovals = new ArrayList<>();
            for (ReleaseNote releaseNote : releaseNotes) {
                ReleaseApproval releaseApproval = ReleaseApproval.builder()
                        .member(member)
                        .release(releaseNote)
                        .build();

                releaseApprovals.add(releaseApproval);
            }

            releaseApprovalRepository.saveAll(releaseApprovals);
        }
    }

    /**
     * memberId로 프로젝트 멤버 조회
     *
     * @author chaeanna
     * @date 2023-07-05
     * @param memberId 멤버 식별 번호
     * @return ProjectMember 조회된 프로젝트 멤버 엔티티
     * @throws CustomException 해당 memberId로 조회된 프로젝트 멤버가 없을 경우 예외 발생
     */
    private ProjectMember getProjectMemberById(Long memberId) {
        return projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_PROJECT_MEMBER));
    }

    /**
     * 해당 사용자가 프로젝트의 리더인지 여부 확인
     *
     * @author chaeanna
     * @date 2023-07-05
     * @param user 사용자 엔티티
     * @param project 프로젝트 엔티티
     * @return 사용자가 프로젝트의 리더인 경우 true, 아닌 경우 false 반환
     */
    private boolean isProjectLeader(User user, Project project) {
        // 사용자와 프로젝트를 매개변수로 하여 해당 사용자의 프로젝트 멤버 정보를 가져옵니다.
        ProjectMember accessMember = findProjectMemberByUserAndProject(user, project);

        // accessMember가 null이 아니고 직책이 'L' (리더)인 경우에만 리더로 간주합니다.
        return accessMember != null && accessMember.getPosition() == 'L';
    }

    /**
     * 사용자와 프로젝트를 기반으로 프로젝트 멤버 조회
     *
     * @author chaeanna
     * @date 2023-07-05
     * @param user 사용자 엔티티
     * @param project 프로젝트 엔티티
     * @return ProjectMember 조회된 프로젝트 멤버 엔티티
     */
    private ProjectMember findProjectMemberByUserAndProject(User user, Project project) {
        // 사용자와 프로젝트를 매개변수로 하여 프로젝트 멤버를 조회합니다.
        return projectMemberRepository.findByUserAndProject(user, project).orElseThrow(() -> new CustomException(NOT_EXISTS_PROJECT_MEMBER));
    }


}
