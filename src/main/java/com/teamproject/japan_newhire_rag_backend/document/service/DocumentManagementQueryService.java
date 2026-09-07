package com.teamproject.japan_newhire_rag_backend.document.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;
import com.teamproject.japan_newhire_rag_backend.document.access.controller.dto.DocumentAccessRuleReadResponse;
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
import com.teamproject.japan_newhire_rag_backend.document.controller.dto.DocumentManagementVersionResponse;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.repository.DocumentRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.Role;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.RoleRepository;

@Service
@Transactional(readOnly = true)
public class DocumentManagementQueryService {

    private static final String ACTIVE = "ACTIVE";
    private static final Comparator<DocumentVersion> VERSION_ORDER = Comparator
            .comparing(DocumentVersion::getCreatedAt, Comparator.reverseOrder())
            .thenComparing(DocumentVersion::getDocumentVersionId, Comparator.reverseOrder());

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentCategoryRepository documentCategoryRepository;
    private final DocumentAccessRuleRepository documentAccessRuleRepository;
    private final DocumentAccessRoleRepository documentAccessRoleRepository;
    private final DocumentAccessDepartmentRepository documentAccessDepartmentRepository;
    private final RoleRepository roleRepository;

    public DocumentManagementQueryService(
            DocumentRepository documentRepository,
            DocumentVersionRepository documentVersionRepository,
            DocumentCategoryRepository documentCategoryRepository,
            DocumentAccessRuleRepository documentAccessRuleRepository,
            DocumentAccessRoleRepository documentAccessRoleRepository,
            DocumentAccessDepartmentRepository documentAccessDepartmentRepository,
            RoleRepository roleRepository
    ) {
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.documentCategoryRepository = documentCategoryRepository;
        this.documentAccessRuleRepository = documentAccessRuleRepository;
        this.documentAccessRoleRepository = documentAccessRoleRepository;
        this.documentAccessDepartmentRepository = documentAccessDepartmentRepository;
        this.roleRepository = roleRepository;
    }

    public List<DocumentManagementListItemResponse> getDocuments() {
        List<Document> documents = documentRepository
                .findByDocumentStatusAndDeletedAtIsNullOrderByCreatedAtDesc(ACTIVE);
        if (documents.isEmpty()) return List.of();

        Map<Long, DocumentCategory> categories = categories(documents);
        List<Long> documentIds = documents.stream().map(Document::getDocumentId).toList();
        Map<Long, List<DocumentVersion>> versionsByDocument = documentVersionRepository
                .findByDocument_DocumentIdIn(documentIds).stream()
                .collect(Collectors.groupingBy(version -> version.getDocument().getDocumentId()));

        return documents.stream().map(document -> {
            DocumentCategory category = categories.get(
                    document.getDocumentCategory().getDocumentCategoryId());
            DocumentVersion latest = versionsByDocument
                    .getOrDefault(document.getDocumentId(), List.of()).stream()
                    .sorted(VERSION_ORDER).findFirst().orElse(null);
            return new DocumentManagementListItemResponse(
                    document.getDocumentId(),
                    document.getDocumentName(),
                    category.getDocumentCategoryId(),
                    category.getCategoryCode(),
                    category.getCategoryName(),
                    document.getDocumentStatus(),
                    latest == null ? null : latest.getDocumentVersionId(),
                    latest == null ? null : latest.getVersionName(),
                    latest == null ? null : latest.getPublicationStatus(),
                    latest != null && latest.isActive(),
                    document.getCreatedAt());
        }).toList();
    }

    public DocumentManagementDetailResponse getDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .filter(value -> ACTIVE.equals(value.getDocumentStatus()))
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(this::notFound);
        DocumentCategory category = categories(List.of(document)).get(
                document.getDocumentCategory().getDocumentCategoryId());
        List<DocumentVersion> versions = new ArrayList<>(documentVersionRepository
                .findByDocument_DocumentIdIn(List.of(documentId)));
        versions.sort(VERSION_ORDER);

        AccessData accessData = loadAccessData(
                versions.stream().map(DocumentVersion::getDocumentVersionId).toList());
        List<DocumentManagementVersionResponse> responses = versions.stream()
                .map(version -> versionResponse(version, accessData))
                .toList();
        return new DocumentManagementDetailResponse(
                document.getDocumentId(),
                document.getDocumentName(),
                document.getDocumentDescription(),
                category.getDocumentCategoryId(),
                category.getCategoryCode(),
                category.getCategoryName(),
                document.getDocumentStatus(),
                document.getCreatedAt(),
                responses);
    }

    private Map<Long, DocumentCategory> categories(List<Document> documents) {
        List<Long> ids = documents.stream()
                .map(document -> document.getDocumentCategory().getDocumentCategoryId())
                .distinct().toList();
        return documentCategoryRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(DocumentCategory::getDocumentCategoryId, Function.identity()));
    }

    private AccessData loadAccessData(List<Long> versionIds) {
        if (versionIds.isEmpty()) return AccessData.empty();
        List<DocumentAccessRule> rules = documentAccessRuleRepository
                .findByDocumentVersion_DocumentVersionIdIn(versionIds);
        if (rules.isEmpty()) return AccessData.empty();

        Map<Long, DocumentAccessRule> rulesByVersion = rules.stream().collect(Collectors.toMap(
                rule -> rule.getDocumentVersion().getDocumentVersionId(), Function.identity()));
        List<Long> ruleIds = rules.stream().map(DocumentAccessRule::getDocumentAccessRuleId).toList();
        List<DocumentAccessRole> accessRoles = documentAccessRoleRepository
                .findByDocumentAccessRule_DocumentAccessRuleIdIn(ruleIds);
        List<DocumentAccessDepartment> departments = documentAccessDepartmentRepository
                .findByDocumentAccessRule_DocumentAccessRuleIdIn(ruleIds);

        Map<Long, List<DocumentAccessRole>> rolesByRule = accessRoles.stream().collect(
                Collectors.groupingBy(role -> role.getDocumentAccessRule().getDocumentAccessRuleId()));
        Map<Long, List<Long>> departmentsByRule = departments.stream().collect(
                Collectors.groupingBy(
                        department -> department.getDocumentAccessRule().getDocumentAccessRuleId(),
                        Collectors.mapping(DocumentAccessDepartment::getDepartmentId, Collectors.toList())));
        List<Long> roleIds = accessRoles.stream().map(DocumentAccessRole::getRoleId).distinct().toList();
        Map<Long, RoleType> roleTypesById = roleIds.isEmpty()
                ? Map.of()
                : roleRepository.findAllById(roleIds).stream().collect(Collectors.toMap(
                        Role::getRoleId,
                        role -> RoleType.valueOf(role.getRoleCode())));
        return new AccessData(rulesByVersion, rolesByRule, departmentsByRule, roleTypesById);
    }

    private DocumentManagementVersionResponse versionResponse(
            DocumentVersion version,
            AccessData data) {
        DocumentAccessRule rule = data.rulesByVersion().get(version.getDocumentVersionId());
        DocumentAccessRuleReadResponse accessRule = null;
        if (rule != null) {
            Long ruleId = rule.getDocumentAccessRuleId();
            List<RoleType> roles = data.rolesByRule().getOrDefault(ruleId, List.of()).stream()
                    .map(DocumentAccessRole::getRoleId)
                    .map(data.roleTypesById()::get)
                    .sorted(Comparator.comparing(RoleType::name))
                    .toList();
            List<Long> departmentIds = data.departmentsByRule()
                    .getOrDefault(ruleId, List.of()).stream().sorted().toList();
            accessRule = new DocumentAccessRuleReadResponse(
                    AccessScope.valueOf(rule.getAccessScope()),
                    ConditionOperator.valueOf(rule.getConditionOperator()),
                    roles,
                    departmentIds,
                    rule.getMinimumJobGradeId(),
                    rule.isNewEmployeeOnly());
        }
        return new DocumentManagementVersionResponse(
                version.getDocumentVersionId(), version.getVersionName(),
                version.getPublicationStatus(), version.isActive(),
                version.getOriginalFileName(), version.getEffectiveDate(),
                version.getExpirationDate(), version.getPublishedAt(),
                version.getCreatedAt(), accessRule);
    }

    private BusinessException notFound() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "문서를 찾을 수 없습니다.");
    }

    private record AccessData(
            Map<Long, DocumentAccessRule> rulesByVersion,
            Map<Long, List<DocumentAccessRole>> rolesByRule,
            Map<Long, List<Long>> departmentsByRule,
            Map<Long, RoleType> roleTypesById) {
        private static AccessData empty() {
            return new AccessData(Map.of(), Map.of(), Map.of(), Map.of());
        }
    }
}
