package com.teamproject.japan_newhire_rag_backend.document.access.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessDepartment;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRole;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessDepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessRoleRepository;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessRuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.AccessReferenceQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@ExtendWith(MockitoExtension.class)
class DocumentAccessServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private DocumentAccessRuleRepository documentAccessRuleRepository;

    @Mock
    private DocumentAccessRoleRepository documentAccessRoleRepository;

    @Mock
    private DocumentAccessDepartmentRepository documentAccessDepartmentRepository;

    @Mock
    private AccessReferenceQueryService accessReferenceQueryService;

    private DocumentAccessService service;

    @BeforeEach
    void setUp() {
        service = new DocumentAccessService(
                currentUserProvider,
                documentAccessRuleRepository,
                documentAccessRoleRepository,
                documentAccessDepartmentRepository,
                accessReferenceQueryService);
    }

    @Test
    void returnsFalseAndStopsWhenRuleDoesNotExist() {
        when(documentAccessRuleRepository.findByDocumentVersion_DocumentVersionId(10L))
                .thenReturn(Optional.empty());

        assertFalse(service.canAccess(10L));

        verify(documentAccessRuleRepository).findByDocumentVersion_DocumentVersionId(10L);
        verifyNoInteractions(
                documentAccessRoleRepository,
                documentAccessDepartmentRepository,
                currentUserProvider,
                accessReferenceQueryService);
    }

    @Test
    void returnsFalseForInactiveRuleThroughEvaluator() {
        configureRule(10L, 100L, false, "ALL", "OR", null, false, List.of(), List.of());
        CurrentUserContext context = context(Set.of(), null, 1, EmployeeType.GENERAL);
        when(currentUserProvider.getCurrentUser()).thenReturn(context);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(context.roles())).thenReturn(Set.of());

        assertFalse(service.canAccess(10L));

        verify(currentUserProvider).getCurrentUser();
    }

    @Test
    void returnsTrueForActiveAllRule() {
        configureRule(10L, 100L, true, "ALL", "OR", null, false, List.of(), List.of());
        CurrentUserContext context = context(Set.of(), null, 1, EmployeeType.GENERAL);
        when(currentUserProvider.getCurrentUser()).thenReturn(context);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(context.roles())).thenReturn(Set.of());

        assertTrue(service.canAccess(10L));
    }

    @Test
    void returnsTrueWhenRestrictedConditionIsSatisfied() {
        DocumentAccessRole roleRow = roleRow(20L);
        configureRule(10L, 100L, true, "RESTRICTED", "OR", null, false,
                List.of(roleRow), List.of());
        CurrentUserContext context = context(Set.of(RoleType.MANAGER), 30L, 2, EmployeeType.GENERAL);
        when(currentUserProvider.getCurrentUser()).thenReturn(context);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(context.roles())).thenReturn(Set.of(20L));

        assertTrue(service.canAccess(10L));
    }

    @Test
    void returnsFalseWhenRestrictedConditionIsNotSatisfied() {
        DocumentAccessRole roleRow = roleRow(20L);
        configureRule(10L, 100L, true, "RESTRICTED", "OR", null, false,
                List.of(roleRow), List.of());
        CurrentUserContext context = context(Set.of(RoleType.EMPLOYEE), 30L, 2, EmployeeType.GENERAL);
        when(currentUserProvider.getCurrentUser()).thenReturn(context);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(context.roles())).thenReturn(Set.of(99L));

        assertFalse(service.canAccess(10L));
    }

    @Test
    void usesRowsLoadedForTheExactRuleId() {
        DocumentAccessRole roleRow = roleRow(20L);
        DocumentAccessDepartment departmentRow = departmentRow(30L);
        configureRule(10L, 100L, true, "RESTRICTED", "AND", null, false,
                List.of(roleRow), List.of(departmentRow));
        CurrentUserContext context = context(Set.of(RoleType.MANAGER), 30L, 2, EmployeeType.GENERAL);
        when(currentUserProvider.getCurrentUser()).thenReturn(context);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(context.roles())).thenReturn(Set.of(20L));

        assertTrue(service.canAccess(10L));

        verify(documentAccessRoleRepository)
                .findByDocumentAccessRule_DocumentAccessRuleId(100L);
        verify(documentAccessDepartmentRepository)
                .findByDocumentAccessRule_DocumentAccessRuleId(100L);
        verify(roleRow).getRoleId();
        verify(departmentRow).getDepartmentId();
    }

    @Test
    void usesCurrentUserContextFromProvider() {
        configureRule(10L, 100L, true, "ALL", "OR", null, false, List.of(), List.of());
        CurrentUserContext context = context(Set.of(RoleType.MANAGER), 30L, 2, EmployeeType.NEW_HIRE);
        when(currentUserProvider.getCurrentUser()).thenReturn(context);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(context.roles())).thenReturn(Set.of(20L));

        assertTrue(service.canAccess(10L));

        verify(currentUserProvider).getCurrentUser();
        verify(accessReferenceQueryService).findRoleIdsByRoleTypes(context.roles());
    }

    @Test
    void propagatesAccessRuleAssemblerException() {
        DocumentAccessRole roleRow = roleRow(20L);
        configureRule(10L, 100L, true, "ALL", "OR", null, false,
                List.of(roleRow), List.of());

        assertThrows(IllegalArgumentException.class, () -> service.canAccess(10L));

        verifyNoInteractions(currentUserProvider);
    }

    @Test
    void propagatesAccessReferenceQueryServiceException() {
        com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule ruleEntity =
                configureRule(10L, 100L, true, "RESTRICTED", "AND", 200L, false,
                        List.of(roleRow(20L)), List.of());
        IllegalStateException expected = new IllegalStateException("job grade lookup failed");
        when(accessReferenceQueryService.findJobGradeLevel(200L)).thenThrow(expected);

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> service.canAccess(10L));

        assertSame(expected, actual);
        verify(ruleEntity).getMinimumJobGradeId();
        verifyNoInteractions(currentUserProvider);
    }

    @Test
    void rejectsNullDocumentVersionId() {
        assertThrows(IllegalArgumentException.class, () -> service.canAccess(null));
        verifyNoInteractions(
                currentUserProvider,
                documentAccessRuleRepository,
                documentAccessRoleRepository,
                documentAccessDepartmentRepository,
                accessReferenceQueryService);
    }

    private com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule configureRule(
            Long documentVersionId,
            Long documentAccessRuleId,
            boolean active,
            String accessScope,
            String conditionOperator,
            Long minimumJobGradeId,
            boolean newEmployeeOnly,
            List<DocumentAccessRole> roleRows,
            List<DocumentAccessDepartment> departmentRows) {
        com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule ruleEntity =
                mock(com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule.class);
        when(documentAccessRuleRepository.findByDocumentVersion_DocumentVersionId(documentVersionId))
                .thenReturn(Optional.of(ruleEntity));
        when(ruleEntity.getDocumentAccessRuleId()).thenReturn(documentAccessRuleId);
        when(documentAccessRoleRepository.findByDocumentAccessRule_DocumentAccessRuleId(documentAccessRuleId))
                .thenReturn(roleRows);
        when(documentAccessDepartmentRepository
                .findByDocumentAccessRule_DocumentAccessRuleId(documentAccessRuleId))
                .thenReturn(departmentRows);
        lenient().when(ruleEntity.getMinimumJobGradeId()).thenReturn(minimumJobGradeId);
        lenient().when(accessReferenceQueryService.findJobGradeLevel(minimumJobGradeId)).thenReturn(null);
        lenient().when(ruleEntity.getAccessScope()).thenReturn(accessScope);
        lenient().when(ruleEntity.getConditionOperator()).thenReturn(conditionOperator);
        lenient().when(ruleEntity.isNewEmployeeOnly()).thenReturn(newEmployeeOnly);
        lenient().when(ruleEntity.isActive()).thenReturn(active);
        return ruleEntity;
    }

    private DocumentAccessRole roleRow(Long roleId) {
        DocumentAccessRole roleRow = mock(DocumentAccessRole.class);
        when(roleRow.getRoleId()).thenReturn(roleId);
        return roleRow;
    }

    private DocumentAccessDepartment departmentRow(Long departmentId) {
        DocumentAccessDepartment departmentRow = mock(DocumentAccessDepartment.class);
        when(departmentRow.getDepartmentId()).thenReturn(departmentId);
        return departmentRow;
    }

    private CurrentUserContext context(
            Set<RoleType> roles,
            Long departmentId,
            Integer jobGradeLevel,
            EmployeeType employeeType) {
        return new CurrentUserContext(1L, 2L, roles, departmentId, jobGradeLevel, employeeType);
    }
}
