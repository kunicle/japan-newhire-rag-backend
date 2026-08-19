package com.teamproject.japan_newhire_rag_backend.document.access.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessDepartment;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRole;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.AccessReferenceQueryService;

@ExtendWith(MockitoExtension.class)
class AccessRuleAssemblerTest {

    @Mock
    private AccessReferenceQueryService accessReferenceQueryService;

    private AccessRuleAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new AccessRuleAssembler(accessReferenceQueryService);
    }

    @Test
    void assemblesAllRuleFields() {
        com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule entity =
                entity(100L, "RESTRICTED", "AND", true, true);
        List<DocumentAccessRole> roleRows = List.of(roleRow(10L), roleRow(20L));
        List<DocumentAccessDepartment> departmentRows = List.of(departmentRow(30L), departmentRow(40L));
        when(accessReferenceQueryService.findJobGradeLevel(100L)).thenReturn(3);

        DocumentAccessRule result = assembler.assemble(entity, roleRows, departmentRows);

        assertTrue(result.active());
        assertEquals(AccessScope.RESTRICTED, result.accessScope());
        assertEquals(Set.of(10L, 20L), result.allowedRoleIds());
        assertEquals(Set.of(30L, 40L), result.allowedDepartmentIds());
        assertEquals(3, result.minimumJobGradeLevel());
        assertTrue(result.newEmployeeOnly());
        assertEquals(ConditionOperator.AND, result.conditionOperator());
    }

    @Test
    void assemblesEmptyRoleAndDepartmentRows() {
        com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule entity =
                entity(null, "ALL", "OR", false, true);
        when(accessReferenceQueryService.findJobGradeLevel(null)).thenReturn(null);

        DocumentAccessRule result = assembler.assemble(entity, List.of(), List.of());

        assertEquals(Set.of(), result.allowedRoleIds());
        assertEquals(Set.of(), result.allowedDepartmentIds());
    }

    @Test
    void passesNullMinimumJobGradeIdAndUsesNullResult() {
        com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule entity =
                entity(null, "RESTRICTED", "OR", true, true);
        when(accessReferenceQueryService.findJobGradeLevel(null)).thenReturn(null);

        DocumentAccessRule result = assembler.assemble(entity, List.of(), List.of());

        verify(accessReferenceQueryService).findJobGradeLevel(null);
        assertNull(result.minimumJobGradeLevel());
    }

    @Test
    void convertsAccessScopeAndConditionOperatorStrings() {
        com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule entity =
                entity(null, "ALL", "OR", false, true);
        when(accessReferenceQueryService.findJobGradeLevel(null)).thenReturn(null);

        DocumentAccessRule result = assembler.assemble(entity, List.of(), List.of());

        assertEquals(AccessScope.ALL, result.accessScope());
        assertEquals(ConditionOperator.OR, result.conditionOperator());
        assertFalse(result.newEmployeeOnly());
    }

    @Test
    void rejectsNullEntity() {
        assertThrows(IllegalArgumentException.class, () -> assembler.assemble(null, List.of(), List.of()));
        verifyNoInteractions(accessReferenceQueryService);
    }

    @Test
    void rejectsNullRoleRows() {
        com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule entity =
                mock(com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule.class);

        assertThrows(IllegalArgumentException.class, () -> assembler.assemble(entity, null, List.of()));
        verifyNoInteractions(accessReferenceQueryService);
    }

    @Test
    void rejectsNullDepartmentRows() {
        com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule entity =
                mock(com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule.class);

        assertThrows(IllegalArgumentException.class, () -> assembler.assemble(entity, List.of(), null));
        verifyNoInteractions(accessReferenceQueryService);
    }

    @Test
    void propagatesJobGradeLookupException() {
        com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule entity =
                mock(com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule.class);
        IllegalStateException expected = new IllegalStateException("job grade lookup failed");
        when(entity.getMinimumJobGradeId()).thenReturn(100L);
        when(accessReferenceQueryService.findJobGradeLevel(100L)).thenThrow(expected);

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> assembler.assemble(entity, List.of(roleRow(10L)), List.of()));

        assertSame(expected, actual);
    }

    @Test
    void propagatesPureRuleInvariantViolation() {
        com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule entity =
                entity(null, "ALL", "OR", false, true);
        when(accessReferenceQueryService.findJobGradeLevel(null)).thenReturn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> assembler.assemble(entity, List.of(roleRow(10L)), List.of()));
    }

    private com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule entity(
            Long minimumJobGradeId,
            String accessScope,
            String conditionOperator,
            boolean newEmployeeOnly,
            boolean active) {
        com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule entity =
                mock(com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule.class);
        when(entity.getMinimumJobGradeId()).thenReturn(minimumJobGradeId);
        when(entity.getAccessScope()).thenReturn(accessScope);
        when(entity.getConditionOperator()).thenReturn(conditionOperator);
        when(entity.isNewEmployeeOnly()).thenReturn(newEmployeeOnly);
        when(entity.isActive()).thenReturn(active);
        return entity;
    }

    private DocumentAccessRole roleRow(Long roleId) {
        DocumentAccessRole row = mock(DocumentAccessRole.class);
        when(row.getRoleId()).thenReturn(roleId);
        return row;
    }

    private DocumentAccessDepartment departmentRow(Long departmentId) {
        DocumentAccessDepartment row = mock(DocumentAccessDepartment.class);
        when(row.getDepartmentId()).thenReturn(departmentId);
        return row;
    }
}
