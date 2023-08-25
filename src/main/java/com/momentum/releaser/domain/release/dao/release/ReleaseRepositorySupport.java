package com.momentum.releaser.domain.release.dao.release;

import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

import com.momentum.releaser.domain.release.domain.ReleaseNote;

@Repository
public class ReleaseRepositorySupport extends QuerydslRepositorySupport {
    private final JPAQueryFactory jpaQueryFactory;

    public ReleaseRepositorySupport(JPAQueryFactory jpaQueryFactory) {
        super(ReleaseNote.class);
        this.jpaQueryFactory = jpaQueryFactory;
    }
}
