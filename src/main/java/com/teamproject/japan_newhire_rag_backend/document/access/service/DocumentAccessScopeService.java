package com.teamproject.japan_newhire_rag_backend.document.access.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessEvaluator;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule;
import com.teamproject.japan_newhire_rag_backend.document.access.UserAccessContext;
import com.teamproject.japan_newhire_rag_backend.document.access.adapter.AccessRuleAssembler;
import com.teamproject.japan_newhire_rag_backend.document.access.adapter.CurrentUserAccessContextAssembler;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessDepartment;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRole;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessDepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessRoleRepository;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessRuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.AccessReferenceQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;

@Service
@Transactional(readOnly = true)
public class DocumentAccessScopeService {

    private final CurrentUserProvider currentUserProvider;
    private final DocumentAccessRuleRepository documentAccessRuleRepository;
    private final DocumentAccessRoleRepository documentAccessRoleRepository;
    private final DocumentAccessDepartmentRepository documentAccessDepartmentRepository;
    private final AccessReferenceQueryService accessReferenceQueryService;
    private final CurrentUserAccessContextAssembler currentUserAccessContextAssembler;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    public DocumentAccessScopeService(
            CurrentUserProvider currentUserProvider,
            DocumentAccessRuleRepository documentAccessRuleRepository,
            DocumentAccessRoleRepository documentAccessRoleRepository,
            DocumentAccessDepartmentRepository documentAccessDepartmentRepository,
            AccessReferenceQueryService accessReferenceQueryService) {
        this.currentUserProvider = currentUserProvider;
        this.documentAccessRuleRepository = documentAccessRuleRepository;
        this.documentAccessRoleRepository = documentAccessRoleRepository;
        this.documentAccessDepartmentRepository = documentAccessDepartmentRepository;
        this.accessReferenceQueryService = accessReferenceQueryService;
        this.currentUserAccessContextAssembler =
                new CurrentUserAccessContextAssembler(accessReferenceQueryService);
        this.documentAccessEvaluator = new DocumentAccessEvaluator();
    }

    public Set<Long> filterAccessibleDocumentVersionIds(Collection<Long> candidateVersionIds) {
        if (candidateVersionIds == null) {
            throw new IllegalArgumentException("candidateVersionIds는 null일 수 없습니다.");
        }
        if (candidateVersionIds.isEmpty()) {
            return Set.of();
        }

        List<com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule> ruleEntities =
                documentAccessRuleRepository
                        .findByDocumentVersion_DocumentVersionIdIn(candidateVersionIds);
        if (ruleEntities.isEmpty()) {
            return Set.of();
        }

        Set<Long> ruleIds = ruleEntities.stream()
                .map(com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule
                        ::getDocumentAccessRuleId)
                .collect(Collectors.toSet());
        List<DocumentAccessRole> roleRows = documentAccessRoleRepository
                .findByDocumentAccessRule_DocumentAccessRuleIdIn(ruleIds);
        List<DocumentAccessDepartment> departmentRows = documentAccessDepartmentRepository
                .findByDocumentAccessRule_DocumentAccessRuleIdIn(ruleIds);

        Map<Long, List<DocumentAccessRole>> roleRowsByRuleId = roleRows.stream()
                .collect(Collectors.groupingBy(
                        row -> row.getDocumentAccessRule().getDocumentAccessRuleId()));
        Map<Long, List<DocumentAccessDepartment>> departmentRowsByRuleId = departmentRows.stream()
                .collect(Collectors.groupingBy(
                        row -> row.getDocumentAccessRule().getDocumentAccessRuleId()));
        List<Long> minimumJobGradeIds = ruleEntities.stream()
                .map(com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule
                        ::getMinimumJobGradeId)
                .filter(jobGradeId -> jobGradeId != null)
                .distinct()
                .toList();
        Map<Long, Integer> jobGradeLevelsById = new HashMap<>();
        for (Long jobGradeId : minimumJobGradeIds) {
            jobGradeLevelsById.put(
                    jobGradeId,
                    accessReferenceQueryService.findJobGradeLevel(jobGradeId));
        }

        AccessRuleAssembler accessRuleAssembler = new AccessRuleAssembler(
                cachedAccessReferenceQueryService(jobGradeLevelsById));
        CurrentUserContext currentUser = currentUserProvider.getCurrentUser();
        UserAccessContext userAccessContext = currentUserAccessContextAssembler.assemble(currentUser);

        Set<Long> accessibleVersionIds = new LinkedHashSet<>();
        for (com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule ruleEntity
                : ruleEntities) {
            Long ruleId = ruleEntity.getDocumentAccessRuleId();
            DocumentAccessRule rule = accessRuleAssembler.assemble(
                    ruleEntity,
                    roleRowsByRuleId.getOrDefault(ruleId, List.of()),
                    departmentRowsByRuleId.getOrDefault(ruleId, List.of()));
            if (documentAccessEvaluator.canAccess(userAccessContext, rule)) {
                accessibleVersionIds.add(ruleEntity.getDocumentVersion().getDocumentVersionId());
            }
        }
        return accessibleVersionIds;
    }

    private AccessReferenceQueryService cachedAccessReferenceQueryService(
            Map<Long, Integer> jobGradeLevelsById) {
        return new AccessReferenceQueryService() {
            @Override
            public Set<Long> findRoleIdsByRoleTypes(Collection<RoleType> roleTypes) {
                return accessReferenceQueryService.findRoleIdsByRoleTypes(roleTypes);
            }

            @Override
            public Integer findJobGradeLevel(Long jobGradeId) {
                return jobGradeId == null ? null : jobGradeLevelsById.get(jobGradeId);
            }
        };
    }
}
