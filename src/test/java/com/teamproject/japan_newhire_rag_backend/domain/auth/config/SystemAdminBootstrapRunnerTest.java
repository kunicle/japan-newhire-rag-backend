package com.teamproject.japan_newhire_rag_backend.domain.auth.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.SystemAdminBootstrapService;

class SystemAdminBootstrapRunnerTest {

    @Test
    void disabledConfigurationDoesNothing() throws Exception {
        AuthBootstrapProperties properties = new AuthBootstrapProperties(
                false, null, null, null, null, null, null);
        SystemAdminBootstrapService service = mock(SystemAdminBootstrapService.class);

        new SystemAdminBootstrapRunner(properties, service)
                .run(new DefaultApplicationArguments());

        verify(service, never()).bootstrap(properties);
    }

    @Test
    void enabledConfigurationRunsBootstrap() throws Exception {
        AuthBootstrapProperties properties = validProperties("raw-secret");
        SystemAdminBootstrapService service = mock(SystemAdminBootstrapService.class);

        new SystemAdminBootstrapRunner(properties, service)
                .run(new DefaultApplicationArguments());

        verify(service).bootstrap(properties);
    }

    @Test
    void enabledConfigurationRequiresEveryValueWithoutExposingPassword() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new AuthBootstrapProperties(
                        true, "admin@example.com", "raw-secret", "BOOT-001",
                        "Admin", null, 20L));
        assertFalse(exception.getMessage().contains("raw-secret"));
        assertFalse(validProperties("raw-secret").toString().contains("raw-secret"));
    }

    private AuthBootstrapProperties validProperties(String password) {
        AuthBootstrapProperties properties = new AuthBootstrapProperties(
                true, "admin@example.com", password, "BOOT-001", "Admin", 10L, 20L);
        assertTrue(properties.enabled());
        return properties;
    }
}
