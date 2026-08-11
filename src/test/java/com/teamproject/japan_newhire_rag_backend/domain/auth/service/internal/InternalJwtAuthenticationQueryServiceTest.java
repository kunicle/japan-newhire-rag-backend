package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.Role;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.UserRole;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.UserRoleRepository;

@ExtendWith(MockitoExtension.class)
class InternalJwtAuthenticationQueryServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private InternalJwtAuthenticationQueryService service;

    @BeforeEach
    void setUp() {
        service = new InternalJwtAuthenticationQueryService(
                appUserRepository,
                userRoleRepository);
    }

    @Test
    void loadReturnsLatestActiveRolesForActiveAccount() {
        AppUser appUser = appUser(AccountStatus.ACTIVE, null);
        UserRole employee = userRole("EMPLOYEE", true, null);
        UserRole manager = userRole("MANAGER", true, null);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(userRoleRepository.findByAppUser_AppUserIdAndRevokedAtIsNull(1L))
                .thenReturn(List.of(employee, manager));

        JwtAuthenticationUser result = service.load(1L);

        assertEquals(1L, result.appUserId());
        assertEquals(Set.of(RoleType.EMPLOYEE, RoleType.MANAGER), result.roles());
    }

    @Test
    void loadExcludesRevokedUserRole() {
        AppUser appUser = appUser(AccountStatus.ACTIVE, null);
        UserRole current = userRole("EMPLOYEE", true, null);
        UserRole revoked = mock(UserRole.class);
        when(revoked.getRevokedAt()).thenReturn(LocalDateTime.now());
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(userRoleRepository.findByAppUser_AppUserIdAndRevokedAtIsNull(1L))
                .thenReturn(List.of(current, revoked));

        assertEquals(Set.of(RoleType.EMPLOYEE), service.load(1L).roles());
        verify(revoked, org.mockito.Mockito.never()).getRole();
    }

    @Test
    void loadExcludesInactiveRole() {
        AppUser appUser = appUser(AccountStatus.ACTIVE, null);
        UserRole active = userRole("EMPLOYEE", true, null);
        UserRole inactive = userRole("MANAGER", false, null);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(userRoleRepository.findByAppUser_AppUserIdAndRevokedAtIsNull(1L))
                .thenReturn(List.of(active, inactive));

        assertEquals(Set.of(RoleType.EMPLOYEE), service.load(1L).roles());
    }

    @Test
    void loadRejectsUnknownActiveRoleCode() {
        AppUser appUser = appUser(AccountStatus.ACTIVE, null);
        UserRole unknown = userRole("UNKNOWN_ROLE", true, null);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(userRoleRepository.findByAppUser_AppUserIdAndRevokedAtIsNull(1L))
                .thenReturn(List.of(unknown));

        assertThrows(AuthenticationServiceException.class, () -> service.load(1L));
    }

    @Test
    void loadRejectsMissingAppUser() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> service.load(1L));
    }

    @Test
    void loadRejectsInactiveAppUser() {
        AppUser appUser = appUser(AccountStatus.INACTIVE, null);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));

        assertThrows(DisabledException.class, () -> service.load(1L));
    }

    @Test
    void loadRejectsLockedAppUser() {
        AppUser appUser = appUser(AccountStatus.LOCKED, null);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));

        assertThrows(LockedException.class, () -> service.load(1L));
    }

    @Test
    void loadRejectsDeletedAppUser() {
        AppUser appUser = appUser(AccountStatus.ACTIVE, LocalDateTime.now());
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));

        assertThrows(DisabledException.class, () -> service.load(1L));
    }

    private AppUser appUser(AccountStatus accountStatus, LocalDateTime deletedAt) {
        AppUser appUser = mock(AppUser.class);
        lenient().when(appUser.getAccountStatus()).thenReturn(accountStatus);
        when(appUser.getDeletedAt()).thenReturn(deletedAt);
        return appUser;
    }

    private UserRole userRole(
            String roleCode,
            boolean active,
            LocalDateTime revokedAt
    ) {
        Role role = mock(Role.class);
        UserRole userRole = mock(UserRole.class);
        lenient().when(userRole.getRevokedAt()).thenReturn(revokedAt);
        lenient().when(userRole.getRole()).thenReturn(role);
        lenient().when(role.isActive()).thenReturn(active);
        lenient().when(role.getRoleCode()).thenReturn(roleCode);
        return userRole;
    }
}
