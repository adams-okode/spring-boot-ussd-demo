package com.decoded.ussd.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.decoded.ussd.data.UssdSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class SessionServiceITest {

    @Autowired
    private SessionService sessionService = null;

    @Test
    void testCache() {
        UssdSession sessionDto = setup();
        assertNotNull(sessionDto);
    }

    private UssdSession setup() {
        UssdSession sessionDto = new UssdSession();
        sessionDto.setId("254702759950");
        sessionDto.setPhoneNumber("");
        sessionDto.setServiceCode("");
        return sessionDto;
    }
}
