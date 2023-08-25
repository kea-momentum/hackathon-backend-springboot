package com.momentum.releaser.domain.release.dao.opinion;

import static com.momentum.releaser.domain.project.domain.QProjectMember.projectMember;
import static com.momentum.releaser.domain.release.domain.QReleaseOpinion.releaseOpinion;
import static com.momentum.releaser.domain.user.domain.QUser.user;

import java.util.List;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.domain.release.dto.QReleaseDataDto_ReleaseOpinionsDataDTO;
import com.momentum.releaser.domain.release.dto.ReleaseDataDto.ReleaseOpinionsDataDTO;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReleaseOpinionRepositoryImpl implements ReleaseOpinionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 릴리즈 노트의 의견 목록을 가져올 때 DTO로 변환해서 반환한다.
     *
     * @author seonwoo
     * @date 2023-07-24
     * @param releaseNote 릴리즈 노트 엔티티
     */
    @Override
    public List<ReleaseOpinionsDataDTO> getDtosByReleaseNote(ReleaseNote releaseNote) {

        return queryFactory
                .select(new QReleaseDataDto_ReleaseOpinionsDataDTO(
                        releaseOpinion.releaseOpinionId.as("opinionId"),
                        releaseOpinion.opinion,
                        Expressions.cases().when(releaseOpinion.member.status.eq('N'))
                                .then(0L)
                                .otherwise(releaseOpinion.member.memberId),
                        releaseOpinion.member.user.name.as("memberName"),
                        releaseOpinion.member.user.img.as("memberImg")
                ))
                .from(releaseOpinion)
                .leftJoin(releaseOpinion.member, projectMember)
                .leftJoin(releaseOpinion.member.user, user)
                .where(releaseOpinion.release.eq(releaseNote))
                .fetch();
    }
}
