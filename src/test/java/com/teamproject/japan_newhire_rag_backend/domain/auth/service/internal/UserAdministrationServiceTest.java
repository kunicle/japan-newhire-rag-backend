package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.CreateUserRequest;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.Role;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.UserRole;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.UpdateUserRolesRequest;
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
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmploymentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.DepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.JobGradeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordService;

class UserAdministrationServiceTest {

    AppUserRepository appUserRepository;
    EmployeeRepository employeeRepository;
    DepartmentRepository departmentRepository;
    JobGradeRepository jobGradeRepository;
    CurrentUserProvider currentUserProvider;
    AuditLogRecordService auditLogRecordService;
    RoleRepository roleRepository;
    UserRoleRepository userRoleRepository;
    UserAdministrationService service;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        jobGradeRepository = mock(JobGradeRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        auditLogRecordService = mock(AuditLogRecordService.class);
        roleRepository = mock(RoleRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        service = new UserAdministrationService(
                appUserRepository,
                employeeRepository,
                departmentRepository,
                jobGradeRepository,
                new BCryptPasswordEncoder(),
                currentUserProvider,
                auditLogRecordService,
                roleRepository,
                userRoleRepository);
        when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserContext(
                99L, 999L, Set.of(RoleType.SYSTEM_ADMIN), 1L, 1,
                EmployeeType.GENERAL));
    }

    @Test
    void createsActiveAppUserAndEmployedEmployeeWithoutRoleAndRecordsActor() {
        Department department = activeDepartment();
        JobGrade jobGrade = activeJobGrade();
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(jobGradeRepository.findById(20L)).thenReturn(Optional.of(jobGrade));
        when(appUserRepository.save(any())).thenAnswer(invocation -> {
            AppUser value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "appUserId", 1L);
            return value;
        });
        when(employeeRepository.save(any())).thenAnswer(invocation -> {
            Employee value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "employeeId", 2L);
            return value;
        });

        var response = service.createUser(request());

        assertEquals(AccountStatus.ACTIVE, response.accountStatus());
        assertEquals(EmploymentStatus.EMPLOYED, response.employmentStatus());
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        assertNotEquals("raw-password", userCaptor.getValue().getPasswordHash());
        assertTrue(new BCryptPasswordEncoder().matches(
                "raw-password", userCaptor.getValue().getPasswordHash()));
        ArgumentCaptor<AuditLogRecordCommand> auditCaptor =
                ArgumentCaptor.forClass(AuditLogRecordCommand.class);
        verify(auditLogRecordService).record(auditCaptor.capture());
        assertEquals(99L, auditCaptor.getValue().actorUserId());
        assertEquals(AuditActionType.USER_CREATED, auditCaptor.getValue().actionType());
        assertEquals(2L, auditCaptor.getValue().changedValue().get("employeeId"));
    }

    @Test
    void rejectsDuplicateEmailBeforeWriting() {
        when(appUserRepository.existsByEmail("new@example.com")).thenReturn(true);
        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.createUser(request()));
        assertEquals(UserAdministrationErrorCode.EMAIL_ALREADY_EXISTS,
                exception.getErrorCode());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateEmployeeNumberBeforeWriting() {
        when(employeeRepository.existsByEmployeeNumber("E-100")).thenReturn(true);
        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.createUser(request()));
        assertEquals(UserAdministrationErrorCode.EMPLOYEE_NUMBER_ALREADY_EXISTS,
                exception.getErrorCode());
    }

    @Test
    void rejectsUnavailableDepartmentAndJobGrade() {
        BusinessException missingDepartment = assertThrows(
                BusinessException.class, () -> service.createUser(request()));
        assertEquals(UserAdministrationErrorCode.DEPARTMENT_NOT_AVAILABLE,
                missingDepartment.getErrorCode());

        Department department = activeDepartment();
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        BusinessException missingGrade = assertThrows(
                BusinessException.class, () -> service.createUser(request()));
        assertEquals(UserAdministrationErrorCode.JOB_GRADE_NOT_AVAILABLE,
                missingGrade.getErrorCode());
    }

    @Test
    void onlyAllowsActiveInactiveTransitionsAndRecordsAudit() {
        AppUser active = AppUser.createActive("a@example.com", "hash");
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(active));
        assertEquals(AccountStatus.INACTIVE, service.deactivate(1L).accountStatus());
        assertEquals(AccountStatus.ACTIVE, service.activate(1L).accountStatus());

        ArgumentCaptor<AuditLogRecordCommand> captor =
                ArgumentCaptor.forClass(AuditLogRecordCommand.class);
        verify(auditLogRecordService, org.mockito.Mockito.times(2)).record(captor.capture());
        assertEquals(AuditActionType.ACCOUNT_DEACTIVATED,
                captor.getAllValues().get(0).actionType());
        assertEquals(AuditActionType.ACCOUNT_ACTIVATED,
                captor.getAllValues().get(1).actionType());
    }

    @Test
    void lockedAccountCannotBeActivatedOrDeactivated() {
        AppUser locked = AppUser.createActive("a@example.com", "hash");
        for (int count = 0; count < 5; count++) {
            locked.recordLoginFailure(java.time.LocalDateTime.now());
        }
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(locked));
        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.activate(1L));
        assertEquals(UserAdministrationErrorCode.INVALID_ACCOUNT_STATUS_TRANSITION,
                exception.getErrorCode());
        assertThrows(BusinessException.class, () -> service.deactivate(1L));
    }

    @Test
    void propagatesAuditFailureSoTransactionCanRollBack() {
        AppUser active = AppUser.createActive("a@example.com", "hash");
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(active));
        org.mockito.Mockito.doThrow(new IllegalStateException("audit failed"))
                .when(auditLogRecordService).record(any());
        assertThrows(IllegalStateException.class, () -> service.deactivate(1L));
    }

    @Test
    void updatesRoleSetByGrantingRevokingAndKeepingExistingAssignments() {
        AppUser target = AppUser.createActive("target@example.com", "hash");
        ReflectionTestUtils.setField(target, "appUserId", 1L);
        AppUser actor = AppUser.createActive("admin@example.com", "hash");
        ReflectionTestUtils.setField(actor, "appUserId", 99L);
        Role employee = role(10L, RoleType.EMPLOYEE, true);
        Role manager = role(20L, RoleType.MANAGER, true);
        Role hrManager = role(30L, RoleType.HR_MANAGER, true);
        UserRole employeeAssignment = UserRole.grant(
                target, employee, actor, java.time.LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(employeeAssignment, "userRoleId", 100L);
        UserRole hrAssignment = UserRole.grant(
                target, hrManager, actor, java.time.LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(hrAssignment, "userRoleId", 300L);

        when(appUserRepository.findForUpdateByAppUserId(1L)).thenReturn(Optional.of(target));
        when(appUserRepository.findById(99L)).thenReturn(Optional.of(actor));
        when(roleRepository.findByRoleCodeIn(any()))
                .thenReturn(java.util.List.of(employee, manager));
        when(userRoleRepository.findForUpdateByAppUser_AppUserIdAndRevokedAtIsNull(1L))
                .thenReturn(java.util.List.of(employeeAssignment, hrAssignment));
        when(userRoleRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            UserRole value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "userRoleId", 200L);
            return value;
        });

        var response = service.updateRoles(1L, new UpdateUserRolesRequest(
                Set.of(RoleType.EMPLOYEE, RoleType.MANAGER)));

        assertEquals(Set.of(RoleType.EMPLOYEE, RoleType.MANAGER), response.roles());
        assertEquals(99L, hrAssignment.getRevokedBy().getAppUserId());
        assertTrue(hrAssignment.getRevokedAt() != null);
        verify(userRoleRepository).saveAndFlush(any(UserRole.class));
        ArgumentCaptor<AuditLogRecordCommand> auditCaptor =
                ArgumentCaptor.forClass(AuditLogRecordCommand.class);
        verify(auditLogRecordService, org.mockito.Mockito.times(2)).record(auditCaptor.capture());
        assertEquals(Set.of(AuditActionType.ROLE_GRANTED, AuditActionType.ROLE_REVOKED),
                auditCaptor.getAllValues().stream()
                        .map(AuditLogRecordCommand::actionType).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void rejectsDeletedUserOrUnavailableRoleAndNoOpDoesNotWrite() {
        AppUser deleted = AppUser.createActive("deleted@example.com", "hash");
        ReflectionTestUtils.setField(deleted, "deletedAt", java.time.LocalDateTime.now());
        when(appUserRepository.findForUpdateByAppUserId(1L)).thenReturn(Optional.of(deleted));
        BusinessException missingUser = assertThrows(BusinessException.class,
                () -> service.updateRoles(1L,
                        new UpdateUserRolesRequest(Set.of(RoleType.EMPLOYEE))));
        assertEquals(UserAdministrationErrorCode.APP_USER_NOT_FOUND, missingUser.getErrorCode());

        AppUser target = AppUser.createActive("target@example.com", "hash");
        ReflectionTestUtils.setField(target, "appUserId", 1L);
        when(appUserRepository.findForUpdateByAppUserId(1L)).thenReturn(Optional.of(target));
        when(appUserRepository.findById(99L)).thenReturn(Optional.of(target));
        when(roleRepository.findByRoleCodeIn(any())).thenReturn(java.util.List.of());
        BusinessException missingRole = assertThrows(BusinessException.class,
                () -> service.updateRoles(1L,
                        new UpdateUserRolesRequest(Set.of(RoleType.EMPLOYEE))));
        assertEquals(UserAdministrationErrorCode.ROLE_NOT_AVAILABLE, missingRole.getErrorCode());
    }

    @Test
    void identicalRoleSetIsNoOp() {
        AppUser target = AppUser.createActive("target@example.com", "hash");
        ReflectionTestUtils.setField(target, "appUserId", 1L);
        AppUser actor = AppUser.createActive("admin@example.com", "hash");
        ReflectionTestUtils.setField(actor, "appUserId", 99L);
        Role employee = role(10L, RoleType.EMPLOYEE, true);
        UserRole assignment = UserRole.grant(
                target, employee, actor, java.time.LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(assignment, "userRoleId", 100L);
        when(appUserRepository.findForUpdateByAppUserId(1L)).thenReturn(Optional.of(target));
        when(appUserRepository.findById(99L)).thenReturn(Optional.of(actor));
        when(roleRepository.findByRoleCodeIn(any())).thenReturn(java.util.List.of(employee));
        when(userRoleRepository.findForUpdateByAppUser_AppUserIdAndRevokedAtIsNull(1L))
                .thenReturn(java.util.List.of(assignment));

        service.updateRoles(1L,
                new UpdateUserRolesRequest(Set.of(RoleType.EMPLOYEE)));

        verify(userRoleRepository, never()).saveAndFlush(any());
        verify(auditLogRecordService, never()).record(any());
        assertEquals(null, assignment.getRevokedAt());
    }

    private Role role(Long id, RoleType type, boolean active) {
        Role value = mock(Role.class);
        when(value.getRoleId()).thenReturn(id);
        when(value.getRoleCode()).thenReturn(type.name());
        when(value.isActive()).thenReturn(active);
        return value;
    }

    private CreateUserRequest request() {
        return new CreateUserRequest(
                "new@example.com", "raw-password", "E-100", "New Hire",
                10L, 20L, EmployeeType.NEW_HIRE, LocalDate.of(2026, 8, 13));
    }

    private Department activeDepartment() {
        Department value = mock(Department.class);
        when(value.getDepartmentStatus()).thenReturn(DepartmentStatus.ACTIVE);
        return value;
    }

    private JobGrade activeJobGrade() {
        JobGrade value = mock(JobGrade.class);
        when(value.isActive()).thenReturn(true);
        return value;
    }
}
