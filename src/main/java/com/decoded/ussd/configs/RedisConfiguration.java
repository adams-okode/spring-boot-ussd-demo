package com.decoded.ussd.configs;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@EnableCaching
// this solution adds phantom keys in Redis and other app like python reading
// these keys will get two values for same key(1 orignal and 1 phantom)
public class RedisConfiguration extends CachingConfigurerSupport {

    @Value("${decoded.cache.host}")
    private String host;

    @Value("${decoded.cache.port}")
    private int port;

    @Value("${decoded.cache.password}")
    private String password;

    @Value("${decoded.cache.default-ttl}")
    private String defaultTTL;

   

    private org.springframework.data.redis.cache.RedisCacheConfiguration createCacheConfiguration(
            long timeoutInSeconds) {
        return org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(timeoutInSeconds));
    }

    @Bean
    public CacheManager cacheManager(LettuceConnectionFactory redisConnectionFactory) {
        Map<String, org.springframework.data.redis.cache.RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // if (Objects.nonNull(cachesTTL)) {
        //     for (Entry<String, String> cacheNameAndTimeout : cachesTTL.entrySet()) {
        //         cacheConfigurations.put(cacheNameAndTimeout.getKey(),
        //                 createCacheConfiguration(Long.parseLong(cacheNameAndTimeout.getValue())));
        //     }
        // }
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(createCacheConfiguration(Long.parseLong(defaultTTL)))
                .withInitialCacheConfigurations(cacheConfigurations).build();
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(host);
        redisStandaloneConfiguration.setPort(port);
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

}
