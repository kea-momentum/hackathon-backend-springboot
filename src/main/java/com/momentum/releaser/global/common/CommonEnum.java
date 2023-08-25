package com.momentum.releaser.global.common;

/**
 * S3 기본 이미지를 불러오도록 하는 Enum
 */
public enum CommonEnum {
    DEFAULT_USER_PROFILE_IMG("https://releaserbucket.s3.ap-northeast-2.amazonaws.com/default/momentum.png"),
    DEFAULT_PROJECT_IMG("https://releaserbucket.s3.ap-northeast-2.amazonaws.com/default/releaser.png");

    private final String url;

    CommonEnum(String url) {
        this.url = url;
    }

    public String url() {
        return url;
    }
}
