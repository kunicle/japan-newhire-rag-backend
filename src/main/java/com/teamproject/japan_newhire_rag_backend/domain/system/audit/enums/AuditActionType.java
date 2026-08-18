package com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums;

import java.util.Set;

public enum AuditActionType {
    USER_CREATED(AuditTargetType.APP_USER, Set.of("accountStatus", "employeeId")),
    ACCOUNT_ACTIVATED(AuditTargetType.APP_USER, Set.of("accountStatus")),
    ACCOUNT_DEACTIVATED(AuditTargetType.APP_USER, Set.of("accountStatus")),
    ROLE_GRANTED(AuditTargetType.USER_ROLE, Set.of("roleId", "roleType")),
    ROLE_REVOKED(AuditTargetType.USER_ROLE, Set.of("roleId", "roleType")),
    DIRECT_MANAGER_CHANGED(
            AuditTargetType.EMPLOYEE,
            Set.of("managerEmployeeId")),
    EVALUATION_RESULT_PUBLISHED(
            AuditTargetType.EVALUATION,
            Set.of(
                    "cycleId",
                    "targetEmployeeId",
                    "selfEvaluationId",
                    "managerEvaluationId",
                    "visibleManagerFeedbackIds"));

    private final AuditTargetType targetType;
    private final Set<String> allowedValueKeys;

    AuditActionType(AuditTargetType targetType, Set<String> allowedValueKeys) {
        this.targetType = targetType;
        this.allowedValueKeys = Set.copyOf(allowedValueKeys);
    }

    public AuditTargetType targetType() {
        return targetType;
    }

    public Set<String> allowedValueKeys() {
        return allowedValueKeys;
    }
}
