package com.teamproject.japan_newhire_rag_backend.document.access;

import java.util.Set;

public record DocumentAccessRule(
        boolean active,
        Set<Long> allowedRoleIds,
        Set<Long> allowedDepartmentIds,
        Integer minimumJobGradeLevel,
        boolean newEmployeeOnly,
        ConditionOperator conditionOperator) {

    public DocumentAccessRule {
        if (allowedRoleIds == null) {
            throw new IllegalArgumentException("allowedRoleIds는 null일 수 없습니다.");
        }
        if (allowedDepartmentIds == null) {
            throw new IllegalArgumentException("allowedDepartmentIds는 null일 수 없습니다.");
        }
        if (conditionOperator == null) {
            throw new IllegalArgumentException("conditionOperator는 null일 수 없습니다.");
        }
    }

    public enum ConditionOperator {
        AND,
        OR
    }
}
