package com.decoded.ussd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class UssdApplicationTests {

	@Autowired
    private RedisCacheManager redisCacheManager = null;

    @Test
    void redisCacheConfigurationPropertiesTest() {
        
        
    }

}
