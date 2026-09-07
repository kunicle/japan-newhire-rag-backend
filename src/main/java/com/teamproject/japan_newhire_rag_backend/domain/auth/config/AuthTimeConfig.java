package com.teamproject.japan_newhire_rag_backend.domain.auth.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthTimeConfig {

    @Bean
    public Clock authClock() {
        return Clock.system(
                ZoneId.of("Asia/Tokyo"));
    }
}