package com.teamproject.japan_newhire_rag_backend.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.Role;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.RoleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.JobGradeRepository;

@ExtendWith(MockitoExtension.class)
class AccessReferenceQueryServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private JobGradeRepository jobGradeRepository;

    private AccessReferenceQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccessReferenceQueryServiceImpl(roleRepository, jobGradeRepository);
    }

    @Test
    void findRoleIdsByRoleTypesConvertsSingleRoleType() {
        Role manager = role(10L, "MANAGER");
        when(roleRepository.findByRoleCodeIn(Set.of("MANAGER")))
                .thenReturn(List.of(manager));

        assertEquals(
                Set.of(10L),
                service.findRoleIdsByRoleTypes(List.of(RoleType.MANAGER)));
    }

    @Test
    void findRoleIdsByRoleTypesUsesOneBatchQueryAndRemovesDuplicates() {
        Role employee = role(10L, "EMPLOYEE");
        Role manager = role(20L, "MANAGER");
        Set<String> roleCodes = Set.of("EMPLOYEE", "MANAGER");
        when(roleRepository.findByRoleCodeIn(roleCodes))
                .thenReturn(List.of(employee, manager));

        Set<Long> result = service.findRoleIdsByRoleTypes(List.of(
                RoleType.EMPLOYEE,
                RoleType.MANAGER,
                RoleType.EMPLOYEE));

        assertEquals(Set.of(10L, 20L), result);
        verify(roleRepository, times(1)).findByRoleCodeIn(roleCodes);
    }

    @Test
    void findRoleIdsByRoleTypesReturnsEmptyWithoutQueryForNullOrEmptyInput() {
        assertEquals(Set.of(), service.findRoleIdsByRoleTypes(null));
        assertEquals(Set.of(), service.findRoleIdsByRoleTypes(List.of()));
        verifyNoInteractions(roleRepository);
    }

    @Test
    void findRoleIdsByRoleTypesRemovesNullElements() {
        Role employee = role(10L, "EMPLOYEE");
        when(roleRepository.findByRoleCodeIn(Set.of("EMPLOYEE")))
                .thenReturn(List.of(employee));

        assertEquals(
                Set.of(10L),
                service.findRoleIdsByRoleTypes(Arrays.asList(null, RoleType.EMPLOYEE, null)));
        verify(roleRepository).findByRoleCodeIn(Set.of("EMPLOYEE"));
    }

    @Test
    void findRoleIdsByRoleTypesFailsWhenAnyRoleDefinitionIsMissing() {
        Role employee = role(10L, "EMPLOYEE");
        when(roleRepository.findByRoleCodeIn(Set.of("EMPLOYEE", "MANAGER")))
                .thenReturn(List.of(employee));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.findRoleIdsByRoleTypes(
                        List.of(RoleType.EMPLOYEE, RoleType.MANAGER)));

        assertEquals(
                "Role definitions not found for roleCodes: [MANAGER]",
                exception.getMessage());
    }

    @Test
    void findJobGradeLevelReturnsStoredGradeLevel() {
        JobGrade jobGrade = mock(JobGrade.class);
        when(jobGrade.getGradeLevel()).thenReturn(3);
        when(jobGradeRepository.findById(100L)).thenReturn(Optional.of(jobGrade));

        assertEquals(3, service.findJobGradeLevel(100L));
    }

    @Test
    void findJobGradeLevelReturnsNullWithoutQueryForNullId() {
        assertNull(service.findJobGradeLevel(null));
        verifyNoInteractions(jobGradeRepository);
    }

    @Test
    void findJobGradeLevelFailsWhenNonNullIdDoesNotExist() {
        when(jobGradeRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.findJobGradeLevel(999L));

        assertEquals("JobGrade not found for jobGradeId: 999", exception.getMessage());
    }

    private Role role(Long roleId, String roleCode) {
        Role role = mock(Role.class);
        lenient().when(role.getRoleId()).thenReturn(roleId);
        when(role.getRoleCode()).thenReturn(roleCode);
        return role;
    }
}
