package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.Role;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.UserRole;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.UserRoleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;

class InternalCurrentUserQueryServiceTest {

    @Test
    void usesOnlyNonRevokedAssignmentsAndExcludesInactiveRoles() {
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        InternalCurrentUserQueryService service = new InternalCurrentUserQueryService(
                appUserRepository, employeeRepository, userRoleRepository);
        AppUser appUser = mock(AppUser.class);
        Employee employee = mock(Employee.class);
        Department department = mock(Department.class);
        JobGrade jobGrade = mock(JobGrade.class);
        UserRole activeAssignment = assignment("EMPLOYEE", true);
        UserRole inactiveRoleAssignment = assignment("MANAGER", false);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(employeeRepository.findByAppUser_AppUserId(1L)).thenReturn(Optional.of(employee));
        when(employee.getEmployeeId()).thenReturn(10L);
        when(employee.getDepartment()).thenReturn(department);
        when(employee.getJobGrade()).thenReturn(jobGrade);
        when(employee.getEmployeeType()).thenReturn(EmployeeType.GENERAL);
        when(department.getDepartmentId()).thenReturn(100L);
        when(jobGrade.getGradeLevel()).thenReturn(1);
        when(userRoleRepository.findByAppUser_AppUserIdAndRevokedAtIsNull(1L))
                .thenReturn(List.of(activeAssignment, inactiveRoleAssignment));

        CurrentUserContext result = service.getCurrentUserContext(1L);

        assertEquals(Set.of(RoleType.EMPLOYEE), result.roles());
        verify(userRoleRepository).findByAppUser_AppUserIdAndRevokedAtIsNull(1L);
    }

    private UserRole assignment(String roleCode, boolean active) {
        UserRole userRole = mock(UserRole.class);
        Role role = mock(Role.class);
        when(userRole.getRole()).thenReturn(role);
        when(role.getRoleCode()).thenReturn(roleCode);
        when(role.isActive()).thenReturn(active);
        return userRole;
    }
}
