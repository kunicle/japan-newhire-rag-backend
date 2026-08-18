package com.teamproject.japan_newhire_rag_backend.document.access.controller.dto;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;
import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentAccessRuleResult;

public record DocumentAccessRuleResponse(
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

    public static DocumentAccessRuleResponse from(DocumentAccessRuleResult result) {
        return new DocumentAccessRuleResponse(
                result.documentId(),
                result.documentVersionId(),
                result.accessRuleId(),
                result.accessScope(),
                result.conditionOperator(),
                result.roleIds(),
                result.departmentIds(),
                result.minimumJobGradeId(),
                result.newEmployeeOnly(),
                result.active(),
                result.createdBy());
    }
}
