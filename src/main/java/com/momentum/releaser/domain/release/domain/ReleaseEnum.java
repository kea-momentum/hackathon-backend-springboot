package com.momentum.releaser.domain.release.domain;

import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ReleaseEnum {

    public enum ReleaseVersionType {
        MAJOR,  // 메이저 버전 자릿수
        MINOR,  // 마이너 버전 자릿수
        PATCH,  // 패치 버전 자릿수

    }

    public enum ReleaseDeployStatus {
        PLANNING,  // 배포 예정
        DEPLOYED,  // 배포 허가
        DENIED,  // 배포 거부
    }

    /**
     * Request body에 null이거나 enum으로 정의되지 않은 값이 들어오는 경우 예외를 발생시킨다.
     */
    @JsonCreator
    public static ReleaseVersionType parsing(String input) {
        return Stream.of(ReleaseVersionType.values())
                .filter(releaseVersionType -> releaseVersionType.toString().equals(input.toUpperCase()))
                .findFirst()
                .orElseThrow(null);
    }
}
