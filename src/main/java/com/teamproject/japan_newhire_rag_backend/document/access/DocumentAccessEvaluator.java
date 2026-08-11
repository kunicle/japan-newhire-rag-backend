package com.teamproject.japan_newhire_rag_backend.document.access;

import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;

public class DocumentAccessEvaluator {

    public boolean canAccess(UserAccessContext user, DocumentAccessRule rule) {
        if (user == null || rule == null) {
            throw new IllegalArgumentException("user와 rule은 null일 수 없습니다.");
        }
        if (!rule.active()) {
            return true;
        }
        if (rule.newEmployeeOnly() && !user.isNewEmployee()) {
            return false;
        }

        int evaluatedConditionCount = 0;
        int satisfiedConditionCount = 0;

        if (!rule.allowedRoleIds().isEmpty()) {
            evaluatedConditionCount++;
            boolean hasAllowedRole = user.roleIds().stream()
                    .anyMatch(rule.allowedRoleIds()::contains);
            if (hasAllowedRole) {
                satisfiedConditionCount++;
            }
        }

        if (!rule.allowedDepartmentIds().isEmpty()) {
            evaluatedConditionCount++;
            Long userDepartmentId = user.departmentId();
            if (userDepartmentId != null && rule.allowedDepartmentIds().contains(userDepartmentId)) {
                satisfiedConditionCount++;
            }
        }

        if (rule.minimumJobGradeLevel() != null) {
            evaluatedConditionCount++;
            if (user.jobGradeLevel() >= rule.minimumJobGradeLevel()) {
                satisfiedConditionCount++;
            }
        }

        if (evaluatedConditionCount == 0) {
            return true;
        }
        if (rule.conditionOperator() == ConditionOperator.AND) {
            return satisfiedConditionCount == evaluatedConditionCount;
        }
        return satisfiedConditionCount > 0;
    }
}
