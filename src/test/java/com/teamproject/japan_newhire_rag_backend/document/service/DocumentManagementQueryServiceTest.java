package com.teamproject.japan_newhire_rag_backend.document.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessDepartment;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRole;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessDepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessRoleRepository;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessRuleRepository;
import com.teamproject.japan_newhire_rag_backend.document.category.entity.DocumentCategory;
import com.teamproject.japan_newhire_rag_backend.document.category.repository.DocumentCategoryRepository;
import com.teamproject.japan_newhire_rag_backend.document.controller.dto.DocumentManagementDetailResponse;
import com.teamproject.japan_newhire_rag_backend.document.controller.dto.DocumentManagementListItemResponse;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.repository.DocumentRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.Role;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.RoleRepository;

class DocumentManagementQueryServiceTest {

    private DocumentRepository documentRepository;
    private DocumentVersionRepository versionRepository;
    private DocumentCategoryRepository categoryRepository;
    private DocumentAccessRuleRepository ruleRepository;
    private DocumentAccessRoleRepository accessRoleRepository;
    private DocumentAccessDepartmentRepository departmentRepository;
    private RoleRepository roleRepository;
    private DocumentManagementQueryService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        versionRepository = mock(DocumentVersionRepository.class);
        categoryRepository = mock(DocumentCategoryRepository.class);
        ruleRepository = mock(DocumentAccessRuleRepository.class);
        accessRoleRepository = mock(DocumentAccessRoleRepository.class);
        departmentRepository = mock(DocumentAccessDepartmentRepository.class);
        roleRepository = mock(RoleRepository.class);
        service = new DocumentManagementQueryService(
                documentRepository, versionRepository, categoryRepository,
                ruleRepository, accessRoleRepository, departmentRepository, roleRepository);
    }

    @Test
    void returnsEmptyDocumentList() {
        when(documentRepository
                .findByDocumentStatusAndDeletedAtIsNullOrderByCreatedAtDesc("ACTIVE"))
                .thenReturn(List.of());

        assertTrue(service.getDocuments().isEmpty());
    }

    @Test
    void mapsCategoryAndSelectsLatestVersionByCreatedAt() {
        DocumentCategory category = category(10L);
        Document document = document(1L, category, "Policy", "ACTIVE", null);
        DocumentVersion older = version(11L, document, "v1", LocalDateTime.of(2026, 1, 1, 0, 0));
        DocumentVersion newer = version(12L, document, "v2", LocalDateTime.of(2026, 2, 1, 0, 0));
        stubList(document, category, List.of(older, newer));

        DocumentManagementListItemResponse result = service.getDocuments().get(0);

        assertEquals(10L, result.documentCategoryId());
        assertEquals("POLICY", result.categoryCode());
        assertEquals("Policy", result.categoryName());
        assertEquals(12L, result.latestVersionId());
        assertEquals("v2", result.latestVersionName());
    }

    @Test
    void usesLargerVersionIdWhenLatestCreatedAtTies() {
        DocumentCategory category = category(10L);
        Document document = document(1L, category, "Policy", "ACTIVE", null);
        LocalDateTime sameTime = LocalDateTime.of(2026, 2, 1, 0, 0);
        stubList(document, category, List.of(
                version(20L, document, "v20", sameTime),
                version(21L, document, "v21", sameTime)));

        assertEquals(21L, service.getDocuments().get(0).latestVersionId());
    }

    @Test
    void mapsSortedVersionsAndNullAllAndRestrictedAccessRules() {
        DocumentCategory category = category(10L);
        Document document = document(1L, category, "Policy", "ACTIVE", null);
        LocalDateTime sameTime = LocalDateTime.of(2026, 3, 1, 0, 0);
        DocumentVersion noRule = version(30L, document, "v3", sameTime);
        DocumentVersion allVersion = version(20L, document, "v2", sameTime);
        DocumentVersion restrictedVersion = version(
                10L, document, "v1", LocalDateTime.of(2026, 1, 1, 0, 0));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(categoryRepository.findAllById(List.of(10L))).thenReturn(List.of(category));
        when(versionRepository.findByDocument_DocumentIdIn(List.of(1L)))
                .thenReturn(List.of(restrictedVersion, allVersion, noRule));

        DocumentAccessRule allRule = rule(100L, allVersion, "ALL", "OR", null, false);
        DocumentAccessRule restrictedRule = rule(
                101L, restrictedVersion, "RESTRICTED", "AND", 7L, true);
        when(ruleRepository.findByDocumentVersion_DocumentVersionIdIn(List.of(30L, 20L, 10L)))
                .thenReturn(List.of(allRule, restrictedRule));
        DocumentAccessRole accessRole = mock(DocumentAccessRole.class);
        when(accessRole.getDocumentAccessRule()).thenReturn(restrictedRule);
        when(accessRole.getRoleId()).thenReturn(5L);
        when(accessRoleRepository.findByDocumentAccessRule_DocumentAccessRuleIdIn(
                List.of(100L, 101L))).thenReturn(List.of(accessRole));
        DocumentAccessDepartment department = mock(DocumentAccessDepartment.class);
        when(department.getDocumentAccessRule()).thenReturn(restrictedRule);
        when(department.getDepartmentId()).thenReturn(9L);
        when(departmentRepository.findByDocumentAccessRule_DocumentAccessRuleIdIn(
                List.of(100L, 101L))).thenReturn(List.of(department));
        Role role = mock(Role.class);
        when(role.getRoleId()).thenReturn(5L);
        when(role.getRoleCode()).thenReturn("HR_MANAGER");
        when(roleRepository.findAllById(List.of(5L))).thenReturn(List.of(role));

        DocumentManagementDetailResponse result = service.getDocument(1L);

        assertEquals(List.of(30L, 20L, 10L), result.versions().stream()
                .map(value -> value.documentVersionId()).toList());
        assertNull(result.versions().get(0).accessRule());
        assertEquals("ALL", result.versions().get(1).accessRule().accessScope().name());
        var restricted = result.versions().get(2).accessRule();
        assertEquals(List.of(RoleType.HR_MANAGER), restricted.roles());
        assertEquals(List.of(9L), restricted.departmentIds());
        assertEquals(7L, restricted.minimumJobGradeId());
        assertTrue(restricted.newEmployeeOnly());
        assertEquals("AND", restricted.conditionOperator().name());
    }

    @Test
    void rejectsMissingInactiveAndDeletedDocuments() {
        when(documentRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals("문서를 찾을 수 없습니다.",
                assertThrows(BusinessException.class, () -> service.getDocument(1L)).getMessage());

        DocumentCategory category = category(10L);
        Document inactive = document(2L, category, "Inactive", "INACTIVE", null);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(inactive));
        assertThrows(BusinessException.class, () -> service.getDocument(2L));

        Document deleted = document(
                3L, category, "Deleted", "ACTIVE", LocalDateTime.of(2026, 1, 1, 0, 0));
        when(documentRepository.findById(3L)).thenReturn(Optional.of(deleted));
        assertThrows(BusinessException.class, () -> service.getDocument(3L));
    }

    private void stubList(
            Document document,
            DocumentCategory category,
            List<DocumentVersion> versions) {
        when(documentRepository
                .findByDocumentStatusAndDeletedAtIsNullOrderByCreatedAtDesc("ACTIVE"))
                .thenReturn(List.of(document));
        when(categoryRepository.findAllById(List.of(10L))).thenReturn(List.of(category));
        when(versionRepository.findByDocument_DocumentIdIn(List.of(1L))).thenReturn(versions);
    }

    private DocumentCategory category(Long id) {
        DocumentCategory category = mock(DocumentCategory.class);
        when(category.getDocumentCategoryId()).thenReturn(id);
        when(category.getCategoryCode()).thenReturn("POLICY");
        when(category.getCategoryName()).thenReturn("Policy");
        return category;
    }

    private Document document(
            Long id,
            DocumentCategory category,
            String name,
            String status,
            LocalDateTime deletedAt) {
        Document document = mock(Document.class);
        when(document.getDocumentId()).thenReturn(id);
        when(document.getDocumentCategory()).thenReturn(category);
        when(document.getDocumentName()).thenReturn(name);
        when(document.getDocumentDescription()).thenReturn("Description");
        when(document.getDocumentStatus()).thenReturn(status);
        when(document.getDeletedAt()).thenReturn(deletedAt);
        when(document.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
        return document;
    }

    private DocumentVersion version(
            Long id,
            Document document,
            String name,
            LocalDateTime createdAt) {
        DocumentVersion version = mock(DocumentVersion.class);
        when(version.getDocumentVersionId()).thenReturn(id);
        when(version.getDocument()).thenReturn(document);
        when(version.getVersionName()).thenReturn(name);
        when(version.getPublicationStatus()).thenReturn("DRAFT");
        when(version.getOriginalFileName()).thenReturn("policy.txt");
        when(version.getEffectiveDate()).thenReturn(LocalDate.of(2026, 1, 1));
        when(version.getCreatedAt()).thenReturn(createdAt);
        return version;
    }

    private DocumentAccessRule rule(
            Long id,
            DocumentVersion version,
            String scope,
            String operator,
            Long minimumGradeId,
            boolean newEmployeeOnly) {
        DocumentAccessRule rule = mock(DocumentAccessRule.class);
        when(rule.getDocumentAccessRuleId()).thenReturn(id);
        when(rule.getDocumentVersion()).thenReturn(version);
        when(rule.getAccessScope()).thenReturn(scope);
        when(rule.getConditionOperator()).thenReturn(operator);
        when(rule.getMinimumJobGradeId()).thenReturn(minimumGradeId);
        when(rule.isNewEmployeeOnly()).thenReturn(newEmployeeOnly);
        return rule;
    }
}
