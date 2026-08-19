package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.auth.config.AuthBootstrapProperties;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.Role;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.UserRole;
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

class SystemAdminBootstrapServiceTest {

    AppUserRepository appUserRepository;
    EmployeeRepository employeeRepository;
    DepartmentRepository departmentRepository;
    JobGradeRepository jobGradeRepository;
    RoleRepository roleRepository;
    UserRoleRepository userRoleRepository;
    BCryptPasswordEncoder passwordEncoder;
    SystemAdminBootstrapService service;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        jobGradeRepository = mock(JobGradeRepository.class);
        roleRepository = mock(RoleRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        service = new SystemAdminBootstrapService(
                appUserRepository, employeeRepository, departmentRepository,
                jobGradeRepository, roleRepository, userRoleRepository, passwordEncoder);
    }

    @Test
    void skipsWhenAnActiveSystemAdminAssignmentExists() {
        when(userRoleRepository
                .existsByRole_RoleCodeAndRole_IsActiveTrueAndRevokedAtIsNull("SYSTEM_ADMIN"))
                .thenReturn(true);

        service.bootstrap(properties());

        verify(appUserRepository, never()).save(any());
        verify(roleRepository, never()).findByRoleCode(any());
    }

    @Test
    void createsUserEmployeeAndSelfGrantedSystemAdmin() {
        Role role = activeRole();
        Department department = activeDepartment();
        JobGrade jobGrade = activeJobGrade();
        when(roleRepository.findByRoleCode("SYSTEM_ADMIN")).thenReturn(Optional.of(role));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(jobGradeRepository.findById(20L)).thenReturn(Optional.of(jobGrade));
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.bootstrap(properties());

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        AppUser user = userCaptor.getValue();
        assertEquals("admin@example.com", user.getEmail());
        assertNotEquals("raw-secret", user.getPasswordHash());
        assertTrue(passwordEncoder.matches("raw-secret", user.getPasswordHash()));

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        Employee employee = employeeCaptor.getValue();
        assertSame(user, employee.getAppUser());
        assertSame(department, employee.getDepartment());
        assertSame(jobGrade, employee.getJobGrade());
        assertEquals(EmployeeType.GENERAL, employee.getEmployeeType());
        assertEquals(EmploymentStatus.EMPLOYED, employee.getEmploymentStatus());

        ArgumentCaptor<UserRole> assignmentCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(assignmentCaptor.capture());
        UserRole assignment = assignmentCaptor.getValue();
        assertSame(user, assignment.getAppUser());
        assertSame(user, assignment.getGrantedBy());
        assertSame(role, assignment.getRole());
        verify(userRoleRepository).flush();
    }

    @Test
    void failsSafelyForPartialUserOrEmployeeData() {
        when(appUserRepository.existsByEmail("admin@example.com")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.bootstrap(properties()));
        verify(appUserRepository, never()).save(any());

        when(appUserRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(employeeRepository.existsByEmployeeNumber("BOOT-001")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.bootstrap(properties()));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void rejectsMissingOrInactiveReferenceData() {
        assertThrows(IllegalStateException.class, () -> service.bootstrap(properties()));

        Role inactiveRole = mock(Role.class);
        when(roleRepository.findByRoleCode("SYSTEM_ADMIN"))
                .thenReturn(Optional.of(inactiveRole));
        assertThrows(IllegalStateException.class, () -> service.bootstrap(properties()));

        Role activeRole = activeRole();
        when(roleRepository.findByRoleCode("SYSTEM_ADMIN")).thenReturn(Optional.of(activeRole));
        assertThrows(IllegalStateException.class, () -> service.bootstrap(properties()));

        Department activeDepartment = activeDepartment();
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(activeDepartment));
        assertThrows(IllegalStateException.class, () -> service.bootstrap(properties()));
    }

    @Test
    void creationRunsInOneTransactionAndPropagatesIntermediateFailure() throws Exception {
        Transactional transactional = SystemAdminBootstrapService.class
                .getMethod("bootstrap", AuthBootstrapProperties.class)
                .getAnnotation(Transactional.class);
        assertTrue(transactional != null);
        Role role = activeRole();
        Department department = activeDepartment();
        JobGrade jobGrade = activeJobGrade();
        when(roleRepository.findByRoleCode("SYSTEM_ADMIN")).thenReturn(Optional.of(role));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(jobGradeRepository.findById(20L)).thenReturn(Optional.of(jobGrade));
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeRepository.save(any())).thenThrow(new IllegalStateException("save failed"));

        assertThrows(IllegalStateException.class, () -> service.bootstrap(properties()));
        verify(userRoleRepository, never()).save(any());
    }

    private AuthBootstrapProperties properties() {
        return new AuthBootstrapProperties(
                true, "admin@example.com", "raw-secret", "BOOT-001",
                "Bootstrap Admin", 10L, 20L);
    }

    private Role activeRole() {
        Role role = mock(Role.class);
        when(role.isActive()).thenReturn(true);
        return role;
    }

    private Department activeDepartment() {
        Department department = mock(Department.class);
        when(department.getDepartmentStatus()).thenReturn(DepartmentStatus.ACTIVE);
        return department;
    }

    private JobGrade activeJobGrade() {
        JobGrade jobGrade = mock(JobGrade.class);
        when(jobGrade.isActive()).thenReturn(true);
        return jobGrade;
    }
}
