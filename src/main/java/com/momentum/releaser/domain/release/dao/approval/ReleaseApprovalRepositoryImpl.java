package com.momentum.releaser.domain.release.dao.approval;

import static com.momentum.releaser.domain.release.domain.QReleaseApproval.releaseApproval;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.momentum.releaser.domain.release.domain.ReleaseNote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReleaseApprovalRepositoryImpl implements ReleaseApprovalCustom {
    private final JPAQueryFactory queryFactory;

    /**
     * 주어진 릴리즈 노트에 대한 모든 릴리즈 동의 정보를 삭제
     *
     * @param releaseNote 삭제하려는 릴리즈 노트 정보
     * @author seonwoo
     * @date 2023-07-09
     */
    @Override
    public void deleteByReleaseNote(ReleaseNote releaseNote) {
        queryFactory
                .delete(releaseApproval)
                .where(releaseApproval.release.eq(releaseNote))
                .execute();
    }

    /**
     * 릴리즈 노트가 없거나 혹은 멤버가 없는 모든 릴리즈 동의 정보 삭제
     *
     * @author chaeanna
     * @date 2023-07-10
     */
    @Override
    public void deleteByReleaseApproval() {
        queryFactory
                .delete(releaseApproval)
                .where(releaseApproval.release.isNull()
                        .or(releaseApproval.member.isNull()))
                .execute();
    }
}
