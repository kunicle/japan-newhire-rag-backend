package com.teamproject.japan_newhire_rag_backend.document.access.controller.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;
import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentAccessRuleCommand;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;

public record DocumentAccessRuleReplaceRequest(
        AccessScope accessScope,
        ConditionOperator conditionOperator,
        Set<RoleType> roles,
        Set<Long> departmentIds,
        Long minimumJobGradeId,
        boolean newEmployeeOnly) {

    public DocumentAccessRuleReplaceRequest {
        if (accessScope == null) {
            throw new IllegalArgumentException("accessScope는 null일 수 없습니다.");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        departmentIds = departmentIds == null ? Set.of() : Set.copyOf(departmentIds);
    }

    @JsonCreator
    public static DocumentAccessRuleReplaceRequest fromJson(
            @JsonProperty("accessScope") AccessScope accessScope,
            @JsonProperty("conditionOperator") ConditionOperator conditionOperator,
            @JsonProperty("roles") Set<RoleType> roles,
            @JsonProperty("departmentIds") Set<Long> departmentIds,
            @JsonProperty("minimumJobGradeId") Long minimumJobGradeId,
            @JsonProperty("newEmployeeOnly") Boolean newEmployeeOnly) {
        return new DocumentAccessRuleReplaceRequest(
                accessScope,
                conditionOperator,
                roles,
                departmentIds,
                minimumJobGradeId,
                Boolean.TRUE.equals(newEmployeeOnly));
    }

    public DocumentAccessRuleCommand toCommand() {
        return new DocumentAccessRuleCommand(
                accessScope,
                conditionOperator,
                roles,
                departmentIds,
                minimumJobGradeId,
                newEmployeeOnly);
    }
}
