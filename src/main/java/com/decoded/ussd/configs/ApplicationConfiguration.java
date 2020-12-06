package com.decoded.ussd.configs;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Configuration
@ConfigurationProperties(prefix = "decoded")
@Data
public class ApplicationConfiguration {

    private CacheConfigurationProperties cache;

    @Getter(value = AccessLevel.PUBLIC)
    private class CacheConfigurationProperties {
        private Integer port;
        private String host;
        private String password;
        private String defaultTtl;
        // private Map<String, String> cachesTtl;
    }
}
