package com.momentum.releaser.global.exception;


import com.momentum.releaser.domain.project.dto.ProjectMemberResponseDto.InviteProjectMemberResponseDTO;
import com.momentum.releaser.global.config.BaseResponseStatus;
import lombok.Getter;

//@AllArgsConstructor
@Getter
public class CustomException extends RuntimeException {
    private final BaseResponseStatus exceptionStatus;
    private Long releaseId;
    private InviteProjectMemberResponseDTO inviteProjectMemberRes;

    public CustomException(BaseResponseStatus status) {
        super(status.getErrorMessage(null));
        this.exceptionStatus = status;
        this.releaseId = null;
    }

    public CustomException(BaseResponseStatus status, Long releaseId) {
        super(status.getErrorMessage(releaseId));
        this.exceptionStatus = status;
        this.releaseId = releaseId;
    }

    public CustomException(BaseResponseStatus status, InviteProjectMemberResponseDTO inviteProjectMemberRes) {
        super(status.getErrorMessageDto(inviteProjectMemberRes));
        this.exceptionStatus = status;
        this.inviteProjectMemberRes = inviteProjectMemberRes;
    }




}