package com.teamproject.japan_newhire_rag_backend.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "japan-newhire-rag-backend",
                "status", "ok"
        );
    }
}
