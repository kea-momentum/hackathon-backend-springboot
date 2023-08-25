package com.momentum.releaser.global.common.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

import com.momentum.releaser.global.common.property.model.Image;

import lombok.Data;

@Data
@Component
@ConfigurationProperties("url")
public class UrlProperty {

    @NestedConfigurationProperty
    private Image image;
}
