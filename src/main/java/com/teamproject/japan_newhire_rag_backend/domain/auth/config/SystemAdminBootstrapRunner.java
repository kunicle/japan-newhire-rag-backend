package com.teamproject.japan_newhire_rag_backend.domain.auth.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.SystemAdminBootstrapService;

@Component
@EnableConfigurationProperties(AuthBootstrapProperties.class)
public class SystemAdminBootstrapRunner implements ApplicationRunner {

    private final AuthBootstrapProperties properties;
    private final SystemAdminBootstrapService bootstrapService;

    public SystemAdminBootstrapRunner(
            AuthBootstrapProperties properties,
            SystemAdminBootstrapService bootstrapService
    ) {
        this.properties = properties;
        this.bootstrapService = bootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.enabled()) {
            bootstrapService.bootstrap(properties);
        }
    }
}
