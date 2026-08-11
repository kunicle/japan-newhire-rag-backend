package com.teamproject.japan_newhire_rag_backend.domain.auth.provider.impl;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalCurrentUserQueryService;

@Component
public class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

    private final InternalCurrentUserQueryService currentUserQueryService;

    public SpringSecurityCurrentUserProvider(
            InternalCurrentUserQueryService currentUserQueryService
    ) {
        this.currentUserQueryService = currentUserQueryService;
    }

    @Override
    public CurrentUserContext getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication is not available in the SecurityContext");
        }

        if (!authentication.isAuthenticated()) {
            throw new InsufficientAuthenticationException("Authentication is not authenticated");
        }

        Long appUserId = extractAppUserId(authentication.getPrincipal());
        return currentUserQueryService.getCurrentUserContext(appUserId);
    }

    private Long extractAppUserId(Object principal) {
        if (principal instanceof Long appUserId) {
            return appUserId;
        }

        if (principal instanceof CharSequence value) {
            try {
                return Long.valueOf(value.toString());
            } catch (NumberFormatException exception) {
                throw appUserIdUnavailable(principal, exception);
            }
        }

        throw appUserIdUnavailable(principal, null);
    }

    private AuthenticationServiceException appUserIdUnavailable(
            Object principal,
            Exception cause
    ) {
        String principalType = principal == null
                ? "null"
                : principal.getClass().getName();
        String message = "Cannot extract appUserId from Authentication principal type: "
                + principalType;

        return cause == null
                ? new AuthenticationServiceException(message)
                : new AuthenticationServiceException(message, cause);
    }
}
