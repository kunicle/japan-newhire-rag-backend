package com.teamproject.japan_newhire_rag_backend.domain.auth.controller;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import com.teamproject.japan_newhire_rag_backend.domain.auth.config.AuthCookieProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RefreshTokenCookieManager {

    private static final String MISSING_TOKEN_MESSAGE = "Invalid refresh token";

    private final AuthCookieProperties properties;

    public RefreshTokenCookieManager(AuthCookieProperties properties) {
        this.properties = properties;
    }

    public String requireToken(HttpServletRequest request) {
        return Arrays.stream(request.getCookies() == null ? new Cookie[0] : request.getCookies())
                .filter(cookie -> properties.name().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElseThrow(() -> new BadCredentialsException(MISSING_TOKEN_MESSAGE));
    }

    public void write(HttpServletResponse response, String rawRefreshToken) {
        addCookie(response, rawRefreshToken, properties.maxAge());
    }

    public void clear(HttpServletResponse response) {
        addCookie(response, "", Duration.ZERO);
    }

    private void addCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(properties.name(), value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(properties.path())
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
