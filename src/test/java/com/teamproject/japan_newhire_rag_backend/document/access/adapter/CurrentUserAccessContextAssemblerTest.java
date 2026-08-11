package com.teamproject.japan_newhire_rag_backend.document.access.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.document.access.UserAccessContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.AccessReferenceQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@ExtendWith(MockitoExtension.class)
class CurrentUserAccessContextAssemblerTest {

    @Mock
    private AccessReferenceQueryService accessReferenceQueryService;

    private CurrentUserAccessContextAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new CurrentUserAccessContextAssembler(accessReferenceQueryService);
    }

    @Test
    void assemblesAllAccessContextValues() {
        Set<RoleType> roles = Set.of(RoleType.EMPLOYEE, RoleType.MANAGER);
        Set<Long> roleIds = Set.of(10L, 20L);
        CurrentUserContext context = createContext(roles, 30L, 4, EmployeeType.NEW_HIRE);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(roles)).thenReturn(roleIds);

        UserAccessContext result = assembler.assemble(context);

        assertEquals(roleIds, result.roleIds());
        assertEquals(30L, result.departmentId());
        assertEquals(4, result.jobGradeLevel());
        assertTrue(result.isNewEmployee());
    }

    @Test
    void passesEmptyRolesAndUsesReturnedRoleIds() {
        Set<RoleType> roles = Set.of();
        Set<Long> roleIds = Set.of();
        CurrentUserContext context = createContext(roles, 30L, 4, EmployeeType.GENERAL);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(roles)).thenReturn(roleIds);

        UserAccessContext result = assembler.assemble(context);

        verify(accessReferenceQueryService).findRoleIdsByRoleTypes(roles);
        assertEquals(roleIds, result.roleIds());
    }

    @Test
    void preservesNullDepartmentId() {
        CurrentUserContext context = createContext(Set.of(RoleType.EMPLOYEE), null, 4, EmployeeType.GENERAL);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(context.roles())).thenReturn(Set.of(10L));

        UserAccessContext result = assembler.assemble(context);

        assertNull(result.departmentId());
    }

    @Test
    void rejectsMissingJobGradeLevel() {
        CurrentUserContext context = createContext(Set.of(), 30L, null, EmployeeType.GENERAL);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(context.roles())).thenReturn(Set.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> assembler.assemble(context));

        assertEquals("CurrentUserContext에 jobGradeLevel이 없습니다.", exception.getMessage());
    }

    @Test
    void mapsNewHireToNewEmployee() {
        CurrentUserContext context = createContext(Set.of(), 30L, 4, EmployeeType.NEW_HIRE);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(context.roles())).thenReturn(Set.of());

        assertTrue(assembler.assemble(context).isNewEmployee());
    }

    @Test
    void mapsGeneralToNotNewEmployee() {
        CurrentUserContext context = createContext(Set.of(), 30L, 4, EmployeeType.GENERAL);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(context.roles())).thenReturn(Set.of());

        assertFalse(assembler.assemble(context).isNewEmployee());
    }

    @Test
    void mapsNullEmployeeTypeToNotNewEmployee() {
        CurrentUserContext context = createContext(Set.of(), 30L, 4, null);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(context.roles())).thenReturn(Set.of());

        assertFalse(assembler.assemble(context).isNewEmployee());
    }

    @Test
    void rejectsNullContext() {
        assertThrows(IllegalArgumentException.class, () -> assembler.assemble(null));
        verifyNoInteractions(accessReferenceQueryService);
    }

    @Test
    void propagatesRoleLookupException() {
        CurrentUserContext context = createContext(Set.of(RoleType.EMPLOYEE), 30L, 4, EmployeeType.GENERAL);
        IllegalStateException expected = new IllegalStateException("role lookup failed");
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(context.roles())).thenThrow(expected);

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> assembler.assemble(context));

        assertSame(expected, actual);
    }

    private CurrentUserContext createContext(
            Set<RoleType> roles,
            Long departmentId,
            Integer jobGradeLevel,
            EmployeeType employeeType) {
        return new CurrentUserContext(1L, 2L, roles, departmentId, jobGradeLevel, employeeType);
    }
}
