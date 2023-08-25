package com.momentum.releaser;

import com.momentum.releaser.global.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@EnableJpaAuditing // BaseTime 클래스처럼 JPA auditing 기능 활성화
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ReleaserApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReleaserApplication.class, args);
    }
}
