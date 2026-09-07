package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.Role;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.UserRole;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.UserRoleRepository;

@Service
@Transactional(readOnly = true)
public class InternalJwtAuthenticationQueryService {

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;

    public InternalJwtAuthenticationQueryService(
            AppUserRepository appUserRepository,
            UserRoleRepository userRoleRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public JwtAuthenticationUser load(Long appUserId) {
        AppUser appUser = appUserRepository.findById(appUserId)
                .orElseThrow(() -> new BadCredentialsException(
                        "Authenticated AppUser does not exist"));
        validateAccount(appUser);

        Set<RoleType> roles = userRoleRepository
                .findByAppUser_AppUserIdAndRevokedAtIsNull(appUserId)
                .stream()
                .filter(userRole -> userRole.getRevokedAt() == null)
                .map(UserRole::getRole)
                .filter(Role::isActive)
                .map(Role::getRoleCode)
                .map(this::toRoleType)
                .collect(Collectors.toUnmodifiableSet());

        return new JwtAuthenticationUser(appUserId, roles);
    }

    private void validateAccount(AppUser appUser) {
        if (appUser.getDeletedAt() != null) {
            throw new DisabledException("Account is not available");
        }
        if (appUser.getAccountStatus() == AccountStatus.LOCKED) {
            throw new LockedException("Account is locked");
        }
        if (appUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new DisabledException("Account is inactive");
        }
    }

    private RoleType toRoleType(String roleCode) {
        try {
            return RoleType.valueOf(roleCode);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AuthenticationServiceException(
                    "Unknown active role code: " + roleCode,
                    exception);
        }
    }
}
