package com.teamproject.japan_newhire_rag_backend.document.access.service;

import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;

public record DocumentAccessRuleCommand(
        AccessScope accessScope,
        ConditionOperator conditionOperator,
        Set<RoleType> roles,
        Set<Long> departmentIds,
        Long minimumJobGradeId,
        boolean newEmployeeOnly) {

    public DocumentAccessRuleCommand {
        if (accessScope == null) {
            throw new IllegalArgumentException("accessScope는 null일 수 없습니다.");
        }
        if (roles == null) {
            throw new IllegalArgumentException("roles는 null일 수 없습니다.");
        }
        if (departmentIds == null) {
            throw new IllegalArgumentException("departmentIds는 null일 수 없습니다.");
        }
        if (roles.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("roles에는 null을 포함할 수 없습니다.");
        }
        if (departmentIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("departmentIds에는 null을 포함할 수 없습니다.");
        }
        roles = Set.copyOf(roles);
        departmentIds = Set.copyOf(departmentIds);
    }
}
