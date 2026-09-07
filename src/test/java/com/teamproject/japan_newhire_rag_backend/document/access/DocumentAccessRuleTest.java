package com.teamproject.japan_newhire_rag_backend.document.access;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;

class DocumentAccessRuleTest {

    @Test
    void rejectsNullAllowedRoleIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentAccessRule(
                        true, AccessScope.RESTRICTED, null, Set.of(), null, false, ConditionOperator.AND));
    }

    @Test
    void rejectsNullAllowedDepartmentIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentAccessRule(
                        true, AccessScope.RESTRICTED, Set.of(1L), null, null, false, ConditionOperator.AND));
    }

    @Test
    void rejectsNullConditionOperator() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentAccessRule(
                        true, AccessScope.RESTRICTED, Set.of(1L), Set.of(), null, false, null));
    }

    @Test
    void rejectsNullAccessScope() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentAccessRule(
                        true, null, Set.of(), Set.of(), null, false, ConditionOperator.AND));
    }

    @Test
    void rejectsAllScopeWithRoleCondition() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentAccessRule(
                        true, AccessScope.ALL, Set.of(1L), Set.of(), null, false, ConditionOperator.AND));
    }

    @Test
    void rejectsAllScopeWithDepartmentCondition() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentAccessRule(
                        true, AccessScope.ALL, Set.of(), Set.of(1L), null, false, ConditionOperator.AND));
    }

    @Test
    void rejectsAllScopeWithMinimumJobGradeCondition() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentAccessRule(
                        true, AccessScope.ALL, Set.of(), Set.of(), 1, false, ConditionOperator.AND));
    }

    @Test
    void rejectsAllScopeWithNewEmployeeCondition() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentAccessRule(
                        true, AccessScope.ALL, Set.of(), Set.of(), null, true, ConditionOperator.AND));
    }

    @Test
    void rejectsRestrictedScopeWithoutConditions() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentAccessRule(
                        true, AccessScope.RESTRICTED, Set.of(), Set.of(), null, false, ConditionOperator.AND));
    }
}
