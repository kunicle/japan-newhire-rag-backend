package com.teamproject.japan_newhire_rag_backend.document.access.adapter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessDepartment;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRole;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.AccessReferenceQueryService;

public class AccessRuleAssembler {

    private final AccessReferenceQueryService accessReferenceQueryService;

    public AccessRuleAssembler(AccessReferenceQueryService accessReferenceQueryService) {
        this.accessReferenceQueryService = accessReferenceQueryService;
    }

    public DocumentAccessRule assemble(
            com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule entity,
            List<DocumentAccessRole> roleRows,
            List<DocumentAccessDepartment> departmentRows) {
        if (entity == null) {
            throw new IllegalArgumentException("DocumentAccessRule Entity는 null일 수 없습니다.");
        }
        if (roleRows == null) {
            throw new IllegalArgumentException("roleRows는 null일 수 없습니다.");
        }
        if (departmentRows == null) {
            throw new IllegalArgumentException("departmentRows는 null일 수 없습니다.");
        }

        Set<Long> allowedRoleIds = roleRows.stream()
                .map(DocumentAccessRole::getRoleId)
                .collect(Collectors.toUnmodifiableSet());
        Set<Long> allowedDepartmentIds = departmentRows.stream()
                .map(DocumentAccessDepartment::getDepartmentId)
                .collect(Collectors.toUnmodifiableSet());
        Integer minimumJobGradeLevel = accessReferenceQueryService.findJobGradeLevel(
                entity.getMinimumJobGradeId());
        AccessScope accessScope = AccessScope.valueOf(entity.getAccessScope());
        ConditionOperator conditionOperator = ConditionOperator.valueOf(entity.getConditionOperator());

        return new DocumentAccessRule(
                entity.isActive(),
                accessScope,
                allowedRoleIds,
                allowedDepartmentIds,
                minimumJobGradeLevel,
                entity.isNewEmployeeOnly(),
                conditionOperator);
    }
}
