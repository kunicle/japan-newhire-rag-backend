package com.teamproject.japan_newhire_rag_backend.document.access.service;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;

public record DocumentAccessRuleResult(
        Long documentId,
        Long documentVersionId,
        Long accessRuleId,
        AccessScope accessScope,
        ConditionOperator conditionOperator,
        List<Long> roleIds,
        List<Long> departmentIds,
        Long minimumJobGradeId,
        boolean newEmployeeOnly,
        boolean active,
        Long createdBy) {
}
