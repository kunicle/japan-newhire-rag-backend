package com.teamproject.japan_newhire_rag_backend.domain.auth.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";

    private final AccessTokenService accessTokenService;
    private final InternalJwtAuthenticationQueryService authenticationQueryService;

    public JwtAuthenticationFilter(
            AccessTokenService accessTokenService,
            InternalJwtAuthenticationQueryService authenticationQueryService
    ) {
        this.accessTokenService = accessTokenService;
        this.authenticationQueryService = authenticationQueryService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(request.getHeader(AUTHORIZATION_HEADER));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return;
        }

        try {
            Long appUserId = accessTokenService.validateAndExtractAppUserId(token);
            JwtAuthenticationUser user = authenticationQueryService.load(appUserId);
            List<SimpleGrantedAuthority> authorities = user.roles().stream()
                    .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.name()))
                    .toList();
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.appUserId(),
                    null,
                    authorities);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | AuthenticationException exception) {
            SecurityContextHolder.clearContext();
        }
    }
}
