package com.momentum.releaser.global.common.property;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CustomProperty implements InitializingBean {

    private final UrlProperty urlProperty;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(urlProperty, "urlProperty must not be null.");
        log.info("urlProperty: {}", urlProperty);
    }
}
