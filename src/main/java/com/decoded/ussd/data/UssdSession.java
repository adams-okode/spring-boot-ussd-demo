package com.decoded.ussd.data;

import java.io.Serializable;

import lombok.Data;

@Data
public class UssdSession implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String id;
    private String sessionId;
    private String serviceCode;
    private String phoneNumber;
    private String text;
    private String previousMenuLevel;
    private String currentMenuLevel;
}
