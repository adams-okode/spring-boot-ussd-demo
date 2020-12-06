package com.decoded.ussd.repositories;

import com.decoded.ussd.data.UssdSession;


import org.springframework.data.repository.CrudRepository;

public interface UssdSessionRepository extends CrudRepository<UssdSession, String> {
}