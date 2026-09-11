package com.teamproject.japan_newhire_rag_backend.document.access.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;
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
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordService;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;

@Service
@Transactional
public class DocumentAccessRuleManagementService {

    private static final String ACTIVE_DOCUMENT_STATUS = "ACTIVE";

    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentAccessRuleRepository documentAccessRuleRepository;
    private final DocumentAccessRoleRepository documentAccessRoleRepository;
    private final DocumentAccessDepartmentRepository documentAccessDepartmentRepository;
    private final AccessReferenceQueryService accessReferenceQueryService;
    private final AuditLogRecordService auditLogRecordService;

    public DocumentAccessRuleManagementService(
            DocumentVersionRepository documentVersionRepository,
            DocumentAccessRuleRepository documentAccessRuleRepository,
            DocumentAccessRoleRepository documentAccessRoleRepository,
            DocumentAccessDepartmentRepository documentAccessDepartmentRepository,
            AccessReferenceQueryService accessReferenceQueryService,
            AuditLogRecordService auditLogRecordService) {
        this.documentVersionRepository = documentVersionRepository;
        this.documentAccessRuleRepository = documentAccessRuleRepository;
        this.documentAccessRoleRepository = documentAccessRoleRepository;
        this.documentAccessDepartmentRepository = documentAccessDepartmentRepository;
        this.accessReferenceQueryService = accessReferenceQueryService;
        this.auditLogRecordService = auditLogRecordService;
    }

    public DocumentAccessRuleResult replace(
            Long documentId,
            Long documentVersionId,
            DocumentAccessRuleCommand command,
            Long actorAppUserId) {
        validateArguments(documentId, documentVersionId, command, actorAppUserId);
        validateCommand(command);

        List<DocumentVersion> lockedVersions =
                documentVersionRepository.findForUpdateByDocument_DocumentId(documentId);
        DocumentVersion target = lockedVersions.stream()
                .filter(version -> Objects.equals(
                        version.getDocumentVersionId(), documentVersionId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "문서 버전을 찾을 수 없습니다."));
        validateDocument(target.getDocument());

        Set<Long> roleIds = resolveRoleIds(command);
        validateMinimumJobGrade(command.minimumJobGradeId());

        DocumentAccessRule existingRule = documentAccessRuleRepository
                .findByDocumentVersion_DocumentVersionId(documentVersionId)
                .orElse(null);
        List<DocumentAccessRole> existingRoles = existingRule == null
                ? List.of()
                : documentAccessRoleRepository.findByDocumentAccessRule_DocumentAccessRuleId(
                        existingRule.getDocumentAccessRuleId());
        List<DocumentAccessDepartment> existingDepartments = existingRule == null
                ? List.of()
                : documentAccessDepartmentRepository.findByDocumentAccessRule_DocumentAccessRuleId(
                        existingRule.getDocumentAccessRuleId());
        Map<String, Object> previousValue = existingRule == null
                ? null
                : auditSnapshot(existingRule, roleIds(existingRoles), departmentIds(existingDepartments));
        DocumentAccessRule rule;
        if (existingRule == null) {
            rule = documentAccessRuleRepository.save(DocumentAccessRule.create(
                    target,
                    command.accessScope(),
                    command.conditionOperator(),
                    command.minimumJobGradeId(),
                    command.newEmployeeOnly(),
                    actorAppUserId));
        } else {
            existingRule.reconfigure(
                    command.accessScope(),
                    command.conditionOperator(),
                    command.minimumJobGradeId(),
                    command.newEmployeeOnly());
            rule = existingRule;
        }

        documentAccessRoleRepository.deleteAll(existingRoles);
        documentAccessDepartmentRepository.deleteAll(existingDepartments);

        List<DocumentAccessRole> newRoles = roleIds.stream()
                .map(roleId -> DocumentAccessRole.create(rule, roleId))
                .toList();
        List<DocumentAccessDepartment> newDepartments = command.departmentIds().stream()
                .map(departmentId -> DocumentAccessDepartment.create(rule, departmentId))
                .toList();
        documentAccessRoleRepository.saveAll(newRoles);
        documentAccessDepartmentRepository.saveAll(newDepartments);

        auditLogRecordService.record(new AuditLogRecordCommand(
                actorAppUserId,
                AuditActionType.DOCUMENT_ACCESS_RULE_CHANGED,
                documentVersionId,
                previousValue,
                auditSnapshot(rule, roleIds, command.departmentIds()),
                null,
                null));

        return result(documentId, documentVersionId, rule, roleIds, command.departmentIds());
    }

    private void validateArguments(
            Long documentId,
            Long documentVersionId,
            DocumentAccessRuleCommand command,
            Long actorAppUserId) {
        if (documentId == null
                || documentVersionId == null
                || command == null
                || actorAppUserId == null) {
            throw new IllegalArgumentException("필수 인자는 null일 수 없습니다.");
        }
    }

    private void validateCommand(DocumentAccessRuleCommand command) {
        boolean hasCondition = !command.roles().isEmpty()
                || !command.departmentIds().isEmpty()
                || command.minimumJobGradeId() != null
                || command.newEmployeeOnly();
        if (command.accessScope() == AccessScope.ALL && hasCondition) {
            throw new IllegalArgumentException("ALL 범위에는 접근 조건을 설정할 수 없습니다.");
        }
        if (command.accessScope() == AccessScope.RESTRICTED) {
            if (command.conditionOperator() == null) {
                throw new IllegalArgumentException("RESTRICTED 범위에는 conditionOperator가 필요합니다.");
            }
            if (!hasCondition) {
                throw new IllegalArgumentException("RESTRICTED 범위에는 접근 조건이 필요합니다.");
            }
        }
    }

    private void validateDocument(Document document) {
        if (!ACTIVE_DOCUMENT_STATUS.equals(document.getDocumentStatus())
                || document.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "문서를 찾을 수 없습니다.");
        }
    }

    private Set<Long> resolveRoleIds(DocumentAccessRuleCommand command) {
        if (command.roles().isEmpty()) {
            return Set.of();
        }
        try {
            return accessReferenceQueryService.findRoleIdsByRoleTypes(command.roles());
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 역할입니다.");
        }
    }

    private void validateMinimumJobGrade(Long minimumJobGradeId) {
        if (minimumJobGradeId == null) {
            return;
        }
        try {
            accessReferenceQueryService.findJobGradeLevel(minimumJobGradeId);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 직급입니다.");
        }
    }

    private DocumentAccessRuleResult result(
            Long documentId,
            Long documentVersionId,
            DocumentAccessRule rule,
            Set<Long> roleIds,
            Set<Long> departmentIds) {
        List<Long> sortedRoleIds = new ArrayList<>(roleIds);
        List<Long> sortedDepartmentIds = new ArrayList<>(departmentIds);
        sortedRoleIds.sort(Long::compareTo);
        sortedDepartmentIds.sort(Long::compareTo);
        return new DocumentAccessRuleResult(
                documentId,
                documentVersionId,
                rule.getDocumentAccessRuleId(),
                AccessScope.valueOf(rule.getAccessScope()),
                ConditionOperator.valueOf(rule.getConditionOperator()),
                List.copyOf(sortedRoleIds),
                List.copyOf(sortedDepartmentIds),
                rule.getMinimumJobGradeId(),
                rule.isNewEmployeeOnly(),
                rule.isActive(),
                rule.getCreatedBy());
    }

    private Map<String, Object> auditSnapshot(
            DocumentAccessRule rule,
            Set<Long> roleIds,
            Set<Long> departmentIds) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("accessScope", rule.getAccessScope());
        snapshot.put("conditionOperator", rule.getConditionOperator());
        snapshot.put("roleIds", sortedIds(roleIds));
        snapshot.put("departmentIds", sortedIds(departmentIds));
        snapshot.put("minimumJobGradeId", rule.getMinimumJobGradeId());
        snapshot.put("newEmployeeOnly", rule.isNewEmployeeOnly());
        snapshot.put("isActive", rule.isActive());
        return snapshot;
    }

    private Set<Long> roleIds(List<DocumentAccessRole> rows) {
        return rows.stream().map(DocumentAccessRole::getRoleId).collect(java.util.stream.Collectors.toSet());
    }

    private Set<Long> departmentIds(List<DocumentAccessDepartment> rows) {
        return rows.stream().map(DocumentAccessDepartment::getDepartmentId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private List<Long> sortedIds(Set<Long> ids) {
        List<Long> sorted = new ArrayList<>(ids);
        sorted.sort(Long::compareTo);
        return List.copyOf(sorted);
    }
}
