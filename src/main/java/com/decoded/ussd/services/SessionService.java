package com.decoded.ussd.services;

import com.decoded.ussd.data.UssdSession;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    @CachePut(cacheNames = "session", key = "#session.id")
    public UssdSession createUssdSession(UssdSession session) {
        return session;
    }

    @Cacheable(cacheNames = "session", key = "#id")
    public UssdSession findById(String id) {
        return null;
    }

    @CachePut(cacheNames = "session", key = "#id")
    public UssdSession updateSession(String id, UssdSession session) {
        return session;
    }

    @CacheEvict(cacheNames = "session", key = "#id")
    public void deleteSession(String id) {
        //deleting the session
    }

}
