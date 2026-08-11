package com.teamproject.japan_newhire_rag_backend.document.access.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
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
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.AccessReferenceQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@ExtendWith(MockitoExtension.class)
class DocumentAccessScopeServiceTest {

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

    private DocumentAccessScopeService service;

    @BeforeEach
    void setUp() {
        service = new DocumentAccessScopeService(
                currentUserProvider,
                documentAccessRuleRepository,
                documentAccessRoleRepository,
                documentAccessDepartmentRepository,
                accessReferenceQueryService);
    }

    @Test
    void returnsEmptySetWithoutInteractionsForEmptyCandidates() {
        assertEquals(Set.of(), service.filterAccessibleDocumentVersionIds(List.of()));

        verifyNoInteractions(
                currentUserProvider,
                documentAccessRuleRepository,
                documentAccessRoleRepository,
                documentAccessDepartmentRepository,
                accessReferenceQueryService);
    }

    @Test
    void excludesCandidateWithoutRule() {
        Collection<Long> candidates = List.of(10L, 20L);
        var rule = rule(100L, 10L, true, "ALL", "OR", null, false);
        configureBatch(candidates, List.of(rule), List.of(), List.of());
        configureCurrentUser(Set.of(), null, 1);

        assertEquals(Set.of(10L), service.filterAccessibleDocumentVersionIds(candidates));
    }

    @Test
    void returnsEmptySetAndStopsWhenNoRulesExist() {
        Collection<Long> candidates = List.of(10L);
        when(documentAccessRuleRepository.findByDocumentVersion_DocumentVersionIdIn(candidates))
                .thenReturn(List.of());

        assertEquals(Set.of(), service.filterAccessibleDocumentVersionIds(candidates));

        verifyNoInteractions(
                currentUserProvider,
                documentAccessRoleRepository,
                documentAccessDepartmentRepository,
                accessReferenceQueryService);
    }

    @Test
    void includesVersionForActiveAllRule() {
        Collection<Long> candidates = List.of(10L);
        var rule = rule(100L, 10L, true, "ALL", "OR", null, false);
        configureBatch(candidates, List.of(rule), List.of(), List.of());
        configureCurrentUser(Set.of(), null, 1);

        assertEquals(Set.of(10L), service.filterAccessibleDocumentVersionIds(candidates));
    }

    @Test
    void includesOnlyRestrictedRuleWhoseConditionIsSatisfied() {
        Collection<Long> candidates = List.of(10L, 20L);
        var allowedRule = rule(100L, 10L, true, "RESTRICTED", "OR", null, false);
        var deniedRule = rule(200L, 20L, true, "RESTRICTED", "OR", null, false);
        DocumentAccessRole allowedRole = role(allowedRule, 1L);
        DocumentAccessRole deniedRole = role(deniedRule, 2L);
        configureBatch(
                candidates,
                List.of(allowedRule, deniedRule),
                List.of(allowedRole, deniedRole),
                List.of());
        configureCurrentUser(Set.of(RoleType.MANAGER), null, 1);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(Set.of(RoleType.MANAGER)))
                .thenReturn(Set.of(1L));

        assertEquals(Set.of(10L), service.filterAccessibleDocumentVersionIds(candidates));
    }

    @Test
    void loadsCurrentUserAndRoleIdsOnlyOnceForMultipleCandidates() {
        Collection<Long> candidates = List.of(10L, 20L);
        var firstRule = rule(100L, 10L, true, "ALL", "OR", null, false);
        var secondRule = rule(200L, 20L, true, "ALL", "OR", null, false);
        configureBatch(candidates, List.of(firstRule, secondRule), List.of(), List.of());
        CurrentUserContext context = configureCurrentUser(Set.of(RoleType.EMPLOYEE), null, 1);

        assertEquals(Set.of(10L, 20L), service.filterAccessibleDocumentVersionIds(candidates));

        verify(currentUserProvider, times(1)).getCurrentUser();
        verify(accessReferenceQueryService, times(1)).findRoleIdsByRoleTypes(context.roles());
    }

    @Test
    void loadsEachDistinctMinimumJobGradeOnlyOnce() {
        Collection<Long> candidates = List.of(10L, 20L, 30L);
        var firstRule = rule(100L, 10L, true, "RESTRICTED", "OR", 5L, false);
        var secondRule = rule(200L, 20L, true, "RESTRICTED", "OR", 5L, false);
        var thirdRule = rule(300L, 30L, true, "RESTRICTED", "OR", 6L, false);
        configureBatch(candidates, List.of(firstRule, secondRule, thirdRule), List.of(), List.of());
        configureCurrentUser(Set.of(), null, 10);
        when(accessReferenceQueryService.findJobGradeLevel(5L)).thenReturn(2);
        when(accessReferenceQueryService.findJobGradeLevel(6L)).thenReturn(3);

        assertEquals(Set.of(10L, 20L, 30L), service.filterAccessibleDocumentVersionIds(candidates));

        verify(accessReferenceQueryService, times(1)).findJobGradeLevel(5L);
        verify(accessReferenceQueryService, times(1)).findJobGradeLevel(6L);
        verify(accessReferenceQueryService, never()).findJobGradeLevel(null);
    }

    @Test
    void rejectsNullCandidates() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.filterAccessibleDocumentVersionIds(null));

        verifyNoInteractions(
                currentUserProvider,
                documentAccessRuleRepository,
                documentAccessRoleRepository,
                documentAccessDepartmentRepository,
                accessReferenceQueryService);
    }

    private void configureBatch(
            Collection<Long> candidates,
            List<com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule> rules,
            List<DocumentAccessRole> roles,
            List<DocumentAccessDepartment> departments) {
        Set<Long> ruleIds = rules.stream()
                .map(com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule
                        ::getDocumentAccessRuleId)
                .collect(java.util.stream.Collectors.toSet());
        when(documentAccessRuleRepository.findByDocumentVersion_DocumentVersionIdIn(candidates))
                .thenReturn(rules);
        when(documentAccessRoleRepository
                .findByDocumentAccessRule_DocumentAccessRuleIdIn(ruleIds))
                .thenReturn(roles);
        when(documentAccessDepartmentRepository
                .findByDocumentAccessRule_DocumentAccessRuleIdIn(ruleIds))
                .thenReturn(departments);
    }

    private CurrentUserContext configureCurrentUser(
            Set<RoleType> roles,
            Long departmentId,
            Integer jobGradeLevel) {
        CurrentUserContext context = new CurrentUserContext(
                1L,
                2L,
                roles,
                departmentId,
                jobGradeLevel,
                EmployeeType.GENERAL);
        when(currentUserProvider.getCurrentUser()).thenReturn(context);
        when(accessReferenceQueryService.findRoleIdsByRoleTypes(roles)).thenReturn(Set.of());
        return context;
    }

    private com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule rule(
            Long ruleId,
            Long versionId,
            boolean active,
            String scope,
            String operator,
            Long minimumJobGradeId,
            boolean newEmployeeOnly) {
        var rule = mock(
                com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule.class);
        DocumentVersion version = mock(DocumentVersion.class);
        when(rule.getDocumentAccessRuleId()).thenReturn(ruleId);
        lenient().when(rule.getDocumentVersion()).thenReturn(version);
        lenient().when(version.getDocumentVersionId()).thenReturn(versionId);
        when(rule.isActive()).thenReturn(active);
        when(rule.getAccessScope()).thenReturn(scope);
        when(rule.getConditionOperator()).thenReturn(operator);
        when(rule.getMinimumJobGradeId()).thenReturn(minimumJobGradeId);
        when(rule.isNewEmployeeOnly()).thenReturn(newEmployeeOnly);
        return rule;
    }

    private DocumentAccessRole role(
            com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule rule,
            Long roleId) {
        DocumentAccessRole role = mock(DocumentAccessRole.class);
        when(role.getDocumentAccessRule()).thenReturn(rule);
        when(role.getRoleId()).thenReturn(roleId);
        return role;
    }
}
