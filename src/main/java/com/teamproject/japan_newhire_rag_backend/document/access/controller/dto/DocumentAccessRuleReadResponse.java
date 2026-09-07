package com.teamproject.japan_newhire_rag_backend.document.access.controller.dto;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;

public record DocumentAccessRuleReadResponse(
        AccessScope accessScope,
        ConditionOperator conditionOperator,
        List<RoleType> roles,
        List<Long> departmentIds,
        Long minimumJobGradeId,
        boolean newEmployeeOnly) {
}
