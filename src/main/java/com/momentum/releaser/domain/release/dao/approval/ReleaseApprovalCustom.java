package com.momentum.releaser.domain.release.dao.approval;

import com.momentum.releaser.domain.release.domain.ReleaseNote;

/**
 * @see ReleaseApprovalRepositoryImpl
 */
public interface ReleaseApprovalCustom {
    void deleteByReleaseNote(ReleaseNote releaseNote);

    void deleteByReleaseApproval();
}
