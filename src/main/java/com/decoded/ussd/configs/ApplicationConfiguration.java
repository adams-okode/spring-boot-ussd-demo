package com.decoded.ussd.configs;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static lombok.AccessLevel.PUBLIC;

@Configuration
@ConfigurationProperties(prefix = "decoded")
@Data
public class ApplicationConfiguration {

    private CacheConfigurationProperties cache;

    @Getter(value = PUBLIC)
    private class CacheConfigurationProperties {
        private Integer port;
        private String host;
        private String password;
        private String defaultTtl;
        // private Map<String, String> cachesTtl;
    }
}
