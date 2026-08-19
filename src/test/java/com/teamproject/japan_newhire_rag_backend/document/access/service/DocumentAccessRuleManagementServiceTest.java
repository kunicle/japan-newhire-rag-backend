package com.teamproject.japan_newhire_rag_backend.document.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;
import com.teamproject.japan_newhire_rag_backend.document.access.adapter.AccessRuleAssembler;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessDepartment;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRole;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessDepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessRoleRepository;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessRuleRepository;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.AccessReferenceQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;

class DocumentAccessRuleManagementServiceTest {

    private DocumentVersionRepository versionRepository;
    private DocumentAccessRuleRepository ruleRepository;
    private DocumentAccessRoleRepository roleRepository;
    private DocumentAccessDepartmentRepository departmentRepository;
    private AccessReferenceQueryService referenceQueryService;
    private DocumentAccessRuleManagementService service;
    private DocumentVersion target;
    private DocumentAccessRule savedRule;

    @BeforeEach
    void setUp() {
        versionRepository = mock(DocumentVersionRepository.class);
        ruleRepository = mock(DocumentAccessRuleRepository.class);
        roleRepository = mock(DocumentAccessRoleRepository.class);
        departmentRepository = mock(DocumentAccessDepartmentRepository.class);
        referenceQueryService = mock(AccessReferenceQueryService.class);
        service = new DocumentAccessRuleManagementService(
                versionRepository,
                ruleRepository,
                roleRepository,
                departmentRepository,
                referenceQueryService);
        target = version(20L, activeDocument());
        when(versionRepository.findForUpdateByDocument_DocumentId(10L))
                .thenReturn(List.of(target));
        when(ruleRepository.findByDocumentVersion_DocumentVersionId(20L))
                .thenReturn(java.util.Optional.empty());
        when(ruleRepository.save(any(DocumentAccessRule.class))).thenAnswer(invocation -> {
            DocumentAccessRule rule = invocation.getArgument(0, DocumentAccessRule.class);
            savedRule = spy(rule);
            doReturn(30L).when(savedRule).getDocumentAccessRuleId();
            return savedRule;
        });
        when(roleRepository.findByDocumentAccessRule_DocumentAccessRuleId(30L))
                .thenReturn(List.of());
        when(departmentRepository.findByDocumentAccessRule_DocumentAccessRuleId(30L))
                .thenReturn(List.of());
    }

    @Test
    void createsAllRuleWithoutChildren() {
        DocumentAccessRuleResult result = replace(allCommand());

        assertThat(result.accessScope()).isEqualTo(AccessScope.ALL);
        assertThat(result.conditionOperator()).isEqualTo(ConditionOperator.OR);
        assertThat(result.roleIds()).isEmpty();
        assertThat(result.departmentIds()).isEmpty();
        assertThat(result.active()).isTrue();
        verify(roleRepository).saveAll(List.of());
        verify(departmentRepository).saveAll(List.of());
    }

    @Test
    void createsRestrictedRoleOnlyRule() {
        when(referenceQueryService.findRoleIdsByRoleTypes(Set.of(RoleType.HR_MANAGER)))
                .thenReturn(Set.of(8L));

        DocumentAccessRuleResult result = replace(command(
                ConditionOperator.OR, Set.of(RoleType.HR_MANAGER), Set.of(), null, false));

        assertThat(result.roleIds()).containsExactly(8L);
        assertThat(savedRoles()).extracting(DocumentAccessRole::getRoleId).containsExactly(8L);
    }

    @Test
    void createsRestrictedDepartmentOnlyRule() {
        DocumentAccessRuleResult result = replace(command(
                ConditionOperator.OR, Set.of(), Set.of(22L), null, false));

        assertThat(result.departmentIds()).containsExactly(22L);
        assertThat(savedDepartments())
                .extracting(DocumentAccessDepartment::getDepartmentId)
                .containsExactly(22L);
    }

    @Test
    void createsRestrictedMinimumJobGradeOnlyRule() {
        when(referenceQueryService.findJobGradeLevel(50L)).thenReturn(3);

        DocumentAccessRuleResult result = replace(command(
                ConditionOperator.OR, Set.of(), Set.of(), 50L, false));

        assertThat(result.minimumJobGradeId()).isEqualTo(50L);
        verify(referenceQueryService).findJobGradeLevel(50L);
    }

    @Test
    void createsRestrictedNewEmployeeOnlyRule() {
        DocumentAccessRuleResult result = replace(command(
                ConditionOperator.OR, Set.of(), Set.of(), null, true));

        assertThat(result.newEmployeeOnly()).isTrue();
    }

    @Test
    void createsRestrictedMixedAndRule() {
        when(referenceQueryService.findRoleIdsByRoleTypes(Set.of(RoleType.MANAGER)))
                .thenReturn(Set.of(9L));
        when(referenceQueryService.findJobGradeLevel(50L)).thenReturn(3);

        DocumentAccessRuleResult result = replace(command(
                ConditionOperator.AND, Set.of(RoleType.MANAGER), Set.of(31L), 50L, true));

        assertThat(result.conditionOperator()).isEqualTo(ConditionOperator.AND);
        assertThat(result.roleIds()).containsExactly(9L);
        assertThat(result.departmentIds()).containsExactly(31L);
        assertThat(result.minimumJobGradeId()).isEqualTo(50L);
        assertThat(result.newEmployeeOnly()).isTrue();
    }

    @Test
    void reconfiguresExistingAllRuleToRestrictedUsingSameParent() {
        DocumentAccessRule existing = existingRule(
                AccessScope.ALL, null, null, false, 41L);
        when(ruleRepository.findByDocumentVersion_DocumentVersionId(20L))
                .thenReturn(java.util.Optional.of(existing));
        when(roleRepository.findByDocumentAccessRule_DocumentAccessRuleId(41L))
                .thenReturn(List.of());
        when(departmentRepository.findByDocumentAccessRule_DocumentAccessRuleId(41L))
                .thenReturn(List.of());

        DocumentAccessRuleResult result = service.replace(
                10L, 20L,
                command(ConditionOperator.OR, Set.of(), Set.of(4L), null, false),
                77L);

        assertThat(result.accessRuleId()).isEqualTo(41L);
        assertThat(result.accessScope()).isEqualTo(AccessScope.RESTRICTED);
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void reconfiguresExistingRestrictedRuleToAllAndRemovesStaleChildren() {
        DocumentAccessRule existing = existingRule(
                AccessScope.RESTRICTED, ConditionOperator.AND, 50L, true, 41L);
        DocumentAccessRole oldRole = DocumentAccessRole.create(existing, 1L);
        DocumentAccessDepartment oldDepartment = DocumentAccessDepartment.create(existing, 2L);
        stubExisting(existing, List.of(oldRole), List.of(oldDepartment));

        DocumentAccessRuleResult result = service.replace(10L, 20L, allCommand(), 77L);

        assertThat(result.accessScope()).isEqualTo(AccessScope.ALL);
        assertThat(result.conditionOperator()).isEqualTo(ConditionOperator.OR);
        assertThat(result.minimumJobGradeId()).isNull();
        assertThat(result.newEmployeeOnly()).isFalse();
        verify(roleRepository).deleteAll(List.of(oldRole));
        verify(departmentRepository).deleteAll(List.of(oldDepartment));
    }

    @Test
    void replacesRestrictedChildren() {
        DocumentAccessRule existing = existingRule(
                AccessScope.RESTRICTED, ConditionOperator.OR, null, false, 41L);
        DocumentAccessRole oldRole = DocumentAccessRole.create(existing, 1L);
        DocumentAccessDepartment oldDepartment = DocumentAccessDepartment.create(existing, 2L);
        stubExisting(existing, List.of(oldRole), List.of(oldDepartment));
        when(referenceQueryService.findRoleIdsByRoleTypes(Set.of(RoleType.SYSTEM_ADMIN)))
                .thenReturn(Set.of(7L));

        DocumentAccessRuleResult result = service.replace(
                10L, 20L,
                command(ConditionOperator.OR, Set.of(RoleType.SYSTEM_ADMIN), Set.of(8L), null, false),
                77L);

        assertThat(result.roleIds()).containsExactly(7L);
        assertThat(result.departmentIds()).containsExactly(8L);
        verify(roleRepository).deleteAll(List.of(oldRole));
        verify(departmentRepository).deleteAll(List.of(oldDepartment));
    }

    @Test
    void commandDefensivelyCopiesRoleAndDepartmentSets() {
        Set<RoleType> roles = new HashSet<>(Set.of(RoleType.EMPLOYEE));
        Set<Long> departments = new HashSet<>(Set.of(1L));
        DocumentAccessRuleCommand command = command(
                ConditionOperator.OR, roles, departments, null, false);

        roles.add(RoleType.MANAGER);
        departments.add(2L);

        assertThat(command.roles()).containsExactly(RoleType.EMPLOYEE);
        assertThat(command.departmentIds()).containsExactly(1L);
    }

    @Test
    void duplicateInputCannotProduceDuplicateChildren() {
        Set<Long> departments = new HashSet<>(List.of(2L, 2L, 2L));

        DocumentAccessRuleResult result = replace(command(
                ConditionOperator.OR, Set.of(), departments, null, false));

        assertThat(result.departmentIds()).containsExactly(2L);
        assertThat(savedDepartments()).hasSize(1);
    }

    @Test
    void rejectsRestrictedRuleWithoutCondition() {
        assertThrows(IllegalArgumentException.class, () -> replace(command(
                ConditionOperator.OR, Set.of(), Set.of(), null, false)));
        verifyNoInteractions(versionRepository);
    }

    @Test
    void rejectsAllRuleWithEachConditionType() {
        assertThrows(IllegalArgumentException.class, () -> replace(new DocumentAccessRuleCommand(
                AccessScope.ALL, ConditionOperator.OR, Set.of(RoleType.EMPLOYEE), Set.of(), null, false)));
        assertThrows(IllegalArgumentException.class, () -> replace(new DocumentAccessRuleCommand(
                AccessScope.ALL, ConditionOperator.OR, Set.of(), Set.of(1L), null, false)));
        assertThrows(IllegalArgumentException.class, () -> replace(new DocumentAccessRuleCommand(
                AccessScope.ALL, ConditionOperator.OR, Set.of(), Set.of(), 1L, false)));
        assertThrows(IllegalArgumentException.class, () -> replace(new DocumentAccessRuleCommand(
                AccessScope.ALL, ConditionOperator.OR, Set.of(), Set.of(), null, true)));
        verifyNoInteractions(versionRepository);
    }

    @Test
    void entityAlsoRejectsInvalidAllOwnFields() {
        assertThrows(IllegalArgumentException.class, () -> DocumentAccessRule.create(
                target, AccessScope.ALL, null, 1L, false, 77L));
        assertThrows(IllegalArgumentException.class, () -> DocumentAccessRule.create(
                target, AccessScope.ALL, null, null, true, 77L));
    }

    @Test
    void throwsResourceNotFoundWhenVersionNotInLockedList() {
        when(versionRepository.findForUpdateByDocument_DocumentId(10L)).thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class, () -> replace(allCommand()));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verifyNoInteractions(ruleRepository);
    }

    @Test
    void throwsResourceNotFoundWhenDocumentInactive() {
        assertInvalidDocument("INACTIVE", null);
    }

    @Test
    void throwsResourceNotFoundWhenDocumentDeleted() {
        assertInvalidDocument("ACTIVE", LocalDateTime.now());
    }

    @Test
    void mapsUnknownRoleReferenceToResourceNotFound() {
        when(referenceQueryService.findRoleIdsByRoleTypes(Set.of(RoleType.EMPLOYEE)))
                .thenThrow(new IllegalStateException("unknown"));

        BusinessException exception = assertThrows(BusinessException.class, () -> replace(command(
                ConditionOperator.OR, Set.of(RoleType.EMPLOYEE), Set.of(), null, false)));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("존재하지 않는 역할입니다.");
    }

    @Test
    void mapsUnknownMinimumJobGradeToResourceNotFound() {
        when(referenceQueryService.findJobGradeLevel(50L))
                .thenThrow(new IllegalStateException("unknown"));

        BusinessException exception = assertThrows(BusinessException.class, () -> replace(command(
                ConditionOperator.OR, Set.of(), Set.of(), 50L, false)));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("존재하지 않는 직급입니다.");
    }

    @Test
    void rejectsNullArgumentsAndCommandNullElements() {
        assertThrows(IllegalArgumentException.class, () -> service.replace(null, 20L, allCommand(), 77L));
        assertThrows(IllegalArgumentException.class, () -> service.replace(10L, null, allCommand(), 77L));
        assertThrows(IllegalArgumentException.class, () -> service.replace(10L, 20L, null, 77L));
        assertThrows(IllegalArgumentException.class, () -> service.replace(10L, 20L, allCommand(), null));
        Set<RoleType> rolesWithNull = new HashSet<>();
        rolesWithNull.add(null);
        Set<Long> departmentsWithNull = new HashSet<>();
        departmentsWithNull.add(null);
        assertThrows(IllegalArgumentException.class, () -> new DocumentAccessRuleCommand(
                AccessScope.RESTRICTED, ConditionOperator.OR, rolesWithNull, Set.of(), null, false));
        assertThrows(IllegalArgumentException.class, () -> new DocumentAccessRuleCommand(
                AccessScope.RESTRICTED, ConditionOperator.OR, Set.of(), departmentsWithNull, null, false));
        verifyNoInteractions(versionRepository);
    }

    @Test
    void usesDocumentVersionPessimisticLockQuery() {
        replace(allCommand());

        verify(versionRepository).findForUpdateByDocument_DocumentId(10L);
    }

    @Test
    void preservesCreatedByWhenReconfiguring() {
        DocumentAccessRule existing = existingRule(
                AccessScope.ALL, null, null, false, 41L);
        stubExisting(existing, List.of(), List.of());

        DocumentAccessRuleResult result = service.replace(
                10L, 20L,
                command(ConditionOperator.OR, Set.of(), Set.of(3L), null, false),
                77L);

        assertThat(result.createdBy()).isEqualTo(55L);
    }

    @Test
    void usesActorAsCreatedByForNewRule() {
        DocumentAccessRuleResult result = replace(allCommand());

        assertThat(result.createdBy()).isEqualTo(77L);
        assertThat(savedRule.getCreatedBy()).isEqualTo(77L);
    }

    @Test
    void writeStateCanBeAssembledByExistingAccessRuleAssembler() {
        when(referenceQueryService.findRoleIdsByRoleTypes(Set.of(RoleType.MANAGER)))
                .thenReturn(Set.of(9L));
        when(referenceQueryService.findJobGradeLevel(50L)).thenReturn(3);
        replace(command(
                ConditionOperator.AND, Set.of(RoleType.MANAGER), Set.of(31L), 50L, true));

        com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule domainRule =
                new AccessRuleAssembler(referenceQueryService)
                        .assemble(savedRule, savedRoles(), savedDepartments());

        assertThat(domainRule.accessScope()).isEqualTo(AccessScope.RESTRICTED);
        assertThat(domainRule.conditionOperator()).isEqualTo(ConditionOperator.AND);
        assertThat(domainRule.allowedRoleIds()).containsExactly(9L);
        assertThat(domainRule.allowedDepartmentIds()).containsExactly(31L);
        assertThat(domainRule.minimumJobGradeLevel()).isEqualTo(3);
    }

    private DocumentAccessRuleResult replace(DocumentAccessRuleCommand command) {
        return service.replace(10L, 20L, command, 77L);
    }

    private DocumentAccessRuleCommand allCommand() {
        return new DocumentAccessRuleCommand(
                AccessScope.ALL, null, Set.of(), Set.of(), null, false);
    }

    private DocumentAccessRuleCommand command(
            ConditionOperator operator,
            Set<RoleType> roles,
            Set<Long> departments,
            Long minimumJobGradeId,
            boolean newEmployeeOnly) {
        return new DocumentAccessRuleCommand(
                AccessScope.RESTRICTED,
                operator,
                roles,
                departments,
                minimumJobGradeId,
                newEmployeeOnly);
    }

    private DocumentAccessRule existingRule(
            AccessScope scope,
            ConditionOperator operator,
            Long minimumJobGradeId,
            boolean newEmployeeOnly,
            Long id) {
        DocumentAccessRule rule = spy(DocumentAccessRule.create(
                target, scope, operator, minimumJobGradeId, newEmployeeOnly, 55L));
        doReturn(id).when(rule).getDocumentAccessRuleId();
        return rule;
    }

    private void stubExisting(
            DocumentAccessRule existing,
            List<DocumentAccessRole> roles,
            List<DocumentAccessDepartment> departments) {
        Long id = existing.getDocumentAccessRuleId();
        when(ruleRepository.findByDocumentVersion_DocumentVersionId(20L))
                .thenReturn(java.util.Optional.of(existing));
        when(roleRepository.findByDocumentAccessRule_DocumentAccessRuleId(id)).thenReturn(roles);
        when(departmentRepository.findByDocumentAccessRule_DocumentAccessRuleId(id))
                .thenReturn(departments);
    }

    private void assertInvalidDocument(String status, LocalDateTime deletedAt) {
        Document document = mock(Document.class);
        when(document.getDocumentStatus()).thenReturn(status);
        when(document.getDeletedAt()).thenReturn(deletedAt);
        target = version(20L, document);
        when(versionRepository.findForUpdateByDocument_DocumentId(10L))
                .thenReturn(List.of(target));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> replace(allCommand()));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verifyNoInteractions(ruleRepository);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<DocumentAccessRole> savedRoles() {
        ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(roleRepository).saveAll(captor.capture());
        List<DocumentAccessRole> result = new ArrayList<>();
        captor.getValue().forEach(row -> result.add((DocumentAccessRole) row));
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<DocumentAccessDepartment> savedDepartments() {
        ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(departmentRepository).saveAll(captor.capture());
        List<DocumentAccessDepartment> result = new ArrayList<>();
        captor.getValue().forEach(row -> result.add((DocumentAccessDepartment) row));
        return result;
    }

    private Document activeDocument() {
        return Document.create(null, "규정", null, 1L);
    }

    private DocumentVersion version(Long id, Document document) {
        DocumentVersion version = spy(DocumentVersion.create(
                document,
                "v" + id,
                LocalDate.now(),
                null,
                "규정.txt",
                "/documents/규정.txt",
                10L));
        doReturn(id).when(version).getDocumentVersionId();
        return version;
    }
}
