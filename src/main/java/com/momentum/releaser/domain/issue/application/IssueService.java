package com.momentum.releaser.domain.issue.application;

import java.util.List;

import com.momentum.releaser.domain.issue.dto.IssueRequestDto.IssueInfoRequestDTO;
import com.momentum.releaser.domain.issue.dto.IssueRequestDto.RegisterOpinionRequestDTO;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.*;

/**
 * 이슈와 관련된 기능을 제공하는 인터페이스입니다.
 */
public interface IssueService {

    /**
     * 7.1 이슈 생성
     */
    IssueIdResponseDTO addIssue(String userEmail, Long projectId, IssueInfoRequestDTO issueInfoReq);

    /**
     * 7.2 이슈 수정
     */
    IssueModifyResponseDTO modifyIssue(Long issueId, String email, IssueInfoRequestDTO updateReq);

    /**
     * 7.3 이슈 제거
     */
    String removeIssue(Long issueId);

    /**
     * 7.4 프로젝트별 모든 이슈 조회
     */
    AllIssueListResponseDTO findAllIssues(Long projectId);

    /**
     * 7.5 프로젝트별 해결 & 미연결 이슈 조회
     */
    List<DoneIssuesResponseDTO> findDoneIssues(Long projectId, String status);

    /**
     * 7.6 릴리즈 노트별 연결된 이슈 조회
     */
    List<ConnectionIssuesResponseDTO> findConnectIssues(Long projectId, Long releaseId);

    /**
     * 7.7 이슈별 조회
     */
    IssueDetailsDTO findIssue(Long issueId, String email);

    /**
     * 7.8 이슈 상태 변경
     */
    String modifyIssueLifeCycle(Long issueId, Integer index, String lifeCycle);

    /**
     * 8.1 이슈 의견 추가
     */
    List<OpinionInfoResponseDTO> addIssueOpinion(Long issueId, String email, RegisterOpinionRequestDTO opinionReq);

    /**
     * 8.2 이슈 의견 삭제
     */
    List<OpinionInfoResponseDTO> removeIssueOpinion(Long opinionId, String email);

}
