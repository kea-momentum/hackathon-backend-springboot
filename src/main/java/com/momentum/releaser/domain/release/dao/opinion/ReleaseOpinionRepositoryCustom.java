package com.momentum.releaser.domain.release.dao.opinion;

import java.util.List;

import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.domain.release.dto.ReleaseDataDto.ReleaseOpinionsDataDTO;

/**
 * @see ReleaseOpinionRepositoryImpl
 */
public interface ReleaseOpinionRepositoryCustom {

    List<ReleaseOpinionsDataDTO> getDtosByReleaseNote(ReleaseNote releaseNote);
}
