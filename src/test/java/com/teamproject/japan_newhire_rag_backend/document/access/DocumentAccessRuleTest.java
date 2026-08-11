package com.teamproject.japan_newhire_rag_backend.document.access;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;

class DocumentAccessRuleTest {

    @Test
    void rejectsNullAllowedRoleIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentAccessRule(true, null, Set.of(), null, false, ConditionOperator.AND));
    }

    @Test
    void rejectsNullAllowedDepartmentIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentAccessRule(true, Set.of(), null, null, false, ConditionOperator.AND));
    }

    @Test
    void rejectsNullConditionOperator() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentAccessRule(true, Set.of(), Set.of(), null, false, null));
    }
}
