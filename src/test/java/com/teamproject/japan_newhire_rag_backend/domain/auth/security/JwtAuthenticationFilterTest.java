package com.teamproject.japan_newhire_rag_backend.domain.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private AccessTokenService accessTokenService;

    @Mock
    private InternalJwtAuthenticationQueryService authenticationQueryService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(
                accessTokenService,
                authenticationQueryService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingAuthorizationHeaderContinuesWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(accessTokenService, authenticationQueryService);
    }

    @Test
    void validBearerTokenStoresLongPrincipalAndLatestAuthorities() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(accessTokenService.validateAndExtractAppUserId("access-token")).thenReturn(1L);
        when(authenticationQueryService.load(1L)).thenReturn(new JwtAuthenticationUser(
                1L,
                Set.of(RoleType.EMPLOYEE, RoleType.MANAGER)));

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertInstanceOf(Long.class, authentication.getPrincipal());
        assertEquals(1L, authentication.getPrincipal());
        assertNull(authentication.getCredentials());
        assertEquals(
                Set.of(
                        new SimpleGrantedAuthority("ROLE_EMPLOYEE"),
                        new SimpleGrantedAuthority("ROLE_MANAGER")),
                Set.copyOf(authentication.getAuthorities()));
        verify(authenticationQueryService).load(1L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void expiredOrInvalidJwtContinuesWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
        when(accessTokenService.validateAndExtractAppUserId("expired-token"))
                .thenThrow(new BadJwtException("JWT expired"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(authenticationQueryService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tamperedJwtContinuesWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer tampered-token");
        when(accessTokenService.validateAndExtractAppUserId("tampered-token"))
                .thenThrow(new BadJwtException("Invalid signature"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(authenticationQueryService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void inactiveAccountContinuesWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(accessTokenService.validateAndExtractAppUserId("access-token")).thenReturn(1L);
        when(authenticationQueryService.load(1L))
                .thenThrow(new DisabledException("Account is inactive"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void lockedAccountContinuesWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(accessTokenService.validateAndExtractAppUserId("access-token")).thenReturn(1L);
        when(authenticationQueryService.load(1L))
                .thenThrow(new LockedException("Account is locked"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void deletedAccountContinuesWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(accessTokenService.validateAndExtractAppUserId("access-token")).thenReturn(1L);
        when(authenticationQueryService.load(1L))
                .thenThrow(new DisabledException("Account is not available"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void malformedBearerHeaderContinuesWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic credentials");

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(accessTokenService, authenticationQueryService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void existingAuthenticationIsPreservedWithoutDuplicateJwtProcessing() throws Exception {
        Authentication existing = new UsernamePasswordAuthenticationToken(
                99L,
                null,
                Set.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(existing);

        filter.doFilter(request, response, filterChain);

        assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
        verify(request, never()).getHeader("Authorization");
        verifyNoInteractions(accessTokenService, authenticationQueryService);
        verify(filterChain).doFilter(request, response);
    }
}
