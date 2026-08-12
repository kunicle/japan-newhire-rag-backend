package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.UserRole;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.UserRoleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(readOnly = true)
public class InternalCurrentUserQueryService {

    private final AppUserRepository appUserRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRoleRepository userRoleRepository;

    public InternalCurrentUserQueryService(
            AppUserRepository appUserRepository,
            EmployeeRepository employeeRepository,
            UserRoleRepository userRoleRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.employeeRepository = employeeRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public CurrentUserContext getCurrentUserContext(Long appUserId) {
        appUserRepository.findById(appUserId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "AppUser not found for appUserId: " + appUserId));

        Employee employee = employeeRepository.findByAppUser_AppUserId(appUserId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee not found for appUserId: " + appUserId));

        Set<RoleType> roles = userRoleRepository
                .findByAppUser_AppUserIdAndRevokedAtIsNull(appUserId)
                .stream()
                .filter(userRole -> userRole.getRole().isActive())
                .map(this::toRoleType)
                .collect(Collectors.toUnmodifiableSet());

        return new CurrentUserContext(
                appUserId,
                employee.getEmployeeId(),
                roles,
                employee.getDepartment().getDepartmentId(),
                employee.getJobGrade().getGradeLevel(),
                employee.getEmployeeType());
    }

    private RoleType toRoleType(UserRole userRole) {
        String roleCode = userRole.getRole().getRoleCode();

        try {
            return RoleType.valueOf(roleCode);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("Unknown role code: " + roleCode, exception);
        }
    }
}
