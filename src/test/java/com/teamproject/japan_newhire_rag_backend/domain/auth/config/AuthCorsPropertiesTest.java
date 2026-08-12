package com.teamproject.japan_newhire_rag_backend.domain.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class AuthCorsPropertiesTest {

    @Test
    void acceptsLocalhostHttpOrigin() {
        AuthCorsProperties properties = properties("http://localhost:5173");

        assertEquals(List.of("http://localhost:5173"), properties.allowedOrigins());
    }

    @Test
    void acceptsHttpsOrigin() {
        AuthCorsProperties properties = properties("https://app.example.com");

        assertEquals(List.of("https://app.example.com"), properties.allowedOrigins());
    }

    @Test
    void rejectsExactWildcard() {
        assertInvalid("*");
    }

    @Test
    void rejectsOriginContainingWildcard() {
        assertInvalid("https://*.example.com");
    }

    @Test
    void rejectsInvalidUri() {
        assertInvalid("not-an-origin");
    }

    @Test
    void rejectsOriginContainingPath() {
        assertInvalid("https://example.com/path");
    }

    @Test
    void trimsOriginBeforeStoringIt() {
        AuthCorsProperties properties = properties("  https://app.example.com  ");

        assertEquals(List.of("https://app.example.com"), properties.allowedOrigins());
    }

    @Test
    void rejectsQueryFragmentAndUserInfo() {
        assertInvalid("https://example.com?x=1");
        assertInvalid("https://example.com#fragment");
        assertInvalid("https://user@example.com");
    }

    private AuthCorsProperties properties(String origin) {
        return new AuthCorsProperties(List.of(origin));
    }

    private void assertInvalid(String origin) {
        assertThrows(IllegalArgumentException.class, () -> properties(origin));
    }
}
