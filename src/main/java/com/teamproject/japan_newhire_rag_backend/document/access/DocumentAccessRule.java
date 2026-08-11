package com.teamproject.japan_newhire_rag_backend.document.access;

import java.util.Set;

public record DocumentAccessRule(
        boolean active,
        AccessScope accessScope,
        Set<Long> allowedRoleIds,
        Set<Long> allowedDepartmentIds,
        Integer minimumJobGradeLevel,
        boolean newEmployeeOnly,
        ConditionOperator conditionOperator) {

    public DocumentAccessRule {
        if (accessScope == null) {
            throw new IllegalArgumentException("accessScope는 null일 수 없습니다.");
        }
        if (allowedRoleIds == null) {
            throw new IllegalArgumentException("allowedRoleIds는 null일 수 없습니다.");
        }
        if (allowedDepartmentIds == null) {
            throw new IllegalArgumentException("allowedDepartmentIds는 null일 수 없습니다.");
        }
        if (conditionOperator == null) {
            throw new IllegalArgumentException("conditionOperator는 null일 수 없습니다.");
        }
        boolean hasAccessCondition = !allowedRoleIds.isEmpty()
                || !allowedDepartmentIds.isEmpty()
                || minimumJobGradeLevel != null
                || newEmployeeOnly;
        if (accessScope == AccessScope.ALL && hasAccessCondition) {
            throw new IllegalArgumentException("ALL 범위에는 접근 조건을 설정할 수 없습니다.");
        }
        if (accessScope == AccessScope.RESTRICTED && !hasAccessCondition) {
            throw new IllegalArgumentException("RESTRICTED 범위에는 접근 조건이 필요합니다.");
        }
    }

    public enum AccessScope {
        ALL,
        RESTRICTED
    }

    public enum ConditionOperator {
        AND,
        OR
    }
}
