package com.decoded.ussd.data;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.Data;

@Data
@RedisHash(value = "sessions", timeToLive = 180)
public class UssdSession implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    private String sessionId;
    private String serviceCode;
    private String phoneNumber;
    private String text;
    private String previousMenuLevel;
    private String currentMenuLevel;
}
