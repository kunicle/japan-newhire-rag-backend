package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.AccountStatusResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.CreateUserRequest;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.CreateUserResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.UpdateUserRolesRequest;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.UserRolesResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.Role;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.UserRole;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.error.UserAdministrationErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.RoleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.UserRoleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.DepartmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.DepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.JobGradeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordService;

@Service
@Transactional
public class UserAdministrationService {

    private final AppUserRepository appUserRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final JobGradeRepository jobGradeRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogRecordService auditLogRecordService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public UserAdministrationService(
            AppUserRepository appUserRepository,
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            JobGradeRepository jobGradeRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserProvider currentUserProvider,
            AuditLogRecordService auditLogRecordService,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.jobGradeRepository = jobGradeRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
        this.auditLogRecordService = auditLogRecordService;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public CreateUserResponse createUser(CreateUserRequest request) {
        Long actorAppUserId = currentUserProvider.getCurrentUser().appUserId();
        if (appUserRepository.existsByEmail(request.email())) {
            throw new BusinessException(UserAdministrationErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (employeeRepository.existsByEmployeeNumber(request.employeeNumber())) {
            throw new BusinessException(
                    UserAdministrationErrorCode.EMPLOYEE_NUMBER_ALREADY_EXISTS);
        }

        Department department = departmentRepository.findById(request.departmentId())
                .filter(value -> value.getDeletedAt() == null)
                .filter(value -> value.getDepartmentStatus() == DepartmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        UserAdministrationErrorCode.DEPARTMENT_NOT_AVAILABLE));
        JobGrade jobGrade = jobGradeRepository.findById(request.jobGradeId())
                .filter(JobGrade::isActive)
                .orElseThrow(() -> new BusinessException(
                        UserAdministrationErrorCode.JOB_GRADE_NOT_AVAILABLE));

        try {
            AppUser appUser = appUserRepository.save(AppUser.createActive(
                    request.email(), passwordEncoder.encode(request.password())));
            Employee employee = employeeRepository.save(Employee.createEmployed(
                    appUser,
                    department,
                    jobGrade,
                    request.employeeNumber(),
                    request.employeeName(),
                    request.employeeType(),
                    request.hireDate()));
            employeeRepository.flush();

            auditLogRecordService.record(new AuditLogRecordCommand(
                    actorAppUserId,
                    AuditActionType.USER_CREATED,
                    appUser.getAppUserId(),
                    null,
                    Map.of(
                            "accountStatus", AccountStatus.ACTIVE,
                            "employeeId", employee.getEmployeeId()),
                    null,
                    null));

            return new CreateUserResponse(
                    appUser.getAppUserId(),
                    employee.getEmployeeId(),
                    appUser.getAccountStatus(),
                    employee.getEmploymentStatus());
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(UserAdministrationErrorCode.USER_DATA_CONFLICT);
        }
    }

    public AccountStatusResponse activate(Long appUserId) {
        return changeStatus(appUserId, AccountStatus.INACTIVE, true);
    }

    public AccountStatusResponse deactivate(Long appUserId) {
        return changeStatus(appUserId, AccountStatus.ACTIVE, false);
    }

    public UserRolesResponse updateRoles(Long appUserId, UpdateUserRolesRequest request) {
        Long actorAppUserId = currentUserProvider.getCurrentUser().appUserId();
        AppUser targetUser = appUserRepository.findForUpdateByAppUserId(appUserId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(
                        UserAdministrationErrorCode.APP_USER_NOT_FOUND));
        AppUser actor = actorAppUserId.equals(appUserId)
                ? targetUser
                : appUserRepository.findById(actorAppUserId)
                        .orElseThrow(() -> new BusinessException(
                                UserAdministrationErrorCode.APP_USER_NOT_FOUND));

        Set<RoleType> requestedRoles = Set.copyOf(request.roles());
        Map<RoleType, Role> availableRoles = loadAvailableRoles(requestedRoles);
        List<UserRole> activeAssignments = userRoleRepository
                .findForUpdateByAppUser_AppUserIdAndRevokedAtIsNull(appUserId);
        Map<RoleType, UserRole> activeByType = activeAssignments.stream()
                .collect(Collectors.toMap(
                        assignment -> toRoleType(assignment.getRole()),
                        Function.identity(),
                        (first, duplicate) -> {
                            throw new IllegalStateException(
                                    "Duplicate active role assignment for " + appUserId);
                        },
                        () -> new EnumMap<>(RoleType.class)));

        LocalDateTime changedAt = LocalDateTime.now();
        Set<RoleType> revokedRoles = new HashSet<>(activeByType.keySet());
        revokedRoles.removeAll(requestedRoles);
        for (RoleType roleType : revokedRoles) {
            UserRole assignment = activeByType.get(roleType);
            assignment.revoke(actor, changedAt);
            recordRoleAudit(actorAppUserId, AuditActionType.ROLE_REVOKED,
                    assignment.getUserRoleId(), assignment.getRole(), true);
        }

        Set<RoleType> grantedRoles = new HashSet<>(requestedRoles);
        grantedRoles.removeAll(activeByType.keySet());
        for (RoleType roleType : grantedRoles) {
            Role role = availableRoles.get(roleType);
            UserRole assignment = userRoleRepository.saveAndFlush(
                    UserRole.grant(targetUser, role, actor, changedAt));
            recordRoleAudit(actorAppUserId, AuditActionType.ROLE_GRANTED,
                    assignment.getUserRoleId(), role, false);
        }

        return new UserRolesResponse(appUserId, requestedRoles);
    }

    private Map<RoleType, Role> loadAvailableRoles(Set<RoleType> requestedRoles) {
        List<Role> roles = roleRepository.findByRoleCodeIn(
                requestedRoles.stream().map(Enum::name).toList());
        Map<RoleType, Role> result = new EnumMap<>(RoleType.class);
        for (Role role : roles) {
            RoleType roleType = toRoleType(role);
            if (!role.isActive() || result.put(roleType, role) != null) {
                throw new BusinessException(UserAdministrationErrorCode.ROLE_NOT_AVAILABLE);
            }
        }
        if (!result.keySet().equals(requestedRoles)) {
            throw new BusinessException(UserAdministrationErrorCode.ROLE_NOT_AVAILABLE);
        }
        return result;
    }

    private RoleType toRoleType(Role role) {
        try {
            return RoleType.valueOf(role.getRoleCode());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("Unknown role code: " + role.getRoleCode(), exception);
        }
    }

    private void recordRoleAudit(
            Long actorAppUserId,
            AuditActionType actionType,
            Long userRoleId,
            Role role,
            boolean revoked
    ) {
        Map<String, ?> snapshot = Map.of(
                "roleId", role.getRoleId(),
                "roleType", toRoleType(role));
        auditLogRecordService.record(new AuditLogRecordCommand(
                actorAppUserId,
                actionType,
                userRoleId,
                revoked ? snapshot : null,
                revoked ? null : snapshot,
                null,
                null));
    }

    private AccountStatusResponse changeStatus(
            Long appUserId,
            AccountStatus requiredCurrentStatus,
            boolean activate
    ) {
        Long actorAppUserId = currentUserProvider.getCurrentUser().appUserId();
        AppUser appUser = appUserRepository.findById(appUserId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(
                        UserAdministrationErrorCode.APP_USER_NOT_FOUND));

        if (appUser.getAccountStatus() != requiredCurrentStatus) {
            throw new BusinessException(
                    UserAdministrationErrorCode.INVALID_ACCOUNT_STATUS_TRANSITION);
        }

        AccountStatus changedStatus;
        AuditActionType actionType;
        if (activate) {
            appUser.activate();
            changedStatus = AccountStatus.ACTIVE;
            actionType = AuditActionType.ACCOUNT_ACTIVATED;
        } else {
            appUser.deactivate();
            changedStatus = AccountStatus.INACTIVE;
            actionType = AuditActionType.ACCOUNT_DEACTIVATED;
        }

        auditLogRecordService.record(new AuditLogRecordCommand(
                actorAppUserId,
                actionType,
                appUserId,
                Map.of("accountStatus", requiredCurrentStatus),
                Map.of("accountStatus", changedStatus),
                null,
                null));
        return new AccountStatusResponse(appUserId, changedStatus);
    }
}
