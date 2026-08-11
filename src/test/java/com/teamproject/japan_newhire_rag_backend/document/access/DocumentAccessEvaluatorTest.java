package com.teamproject.japan_newhire_rag_backend.document.access;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;

class DocumentAccessEvaluatorTest {

    private final DocumentAccessEvaluator evaluator = new DocumentAccessEvaluator();

    @Test
    void rejectsNullUserOrRule() {
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.canAccess(null, createRule(true, AccessScope.ALL, Set.of(), Set.of(), null, false,
                        ConditionOperator.AND)));
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.canAccess(createUser(Set.of(), 1L, 1, false), null));
    }

    @Test
    void deniesAccessWhenRuleIsInactive() {
        UserAccessContext user = createUser(Set.of(), 1L, 1, false);
        DocumentAccessRule rule =
                createRule(false, AccessScope.RESTRICTED, Set.of(99L), Set.of(99L), 99, true,
                        ConditionOperator.AND);

        assertFalse(evaluator.canAccess(user, rule));
    }

    @Test
    void deniesNonNewEmployeeWhenRuleIsNewEmployeeOnly() {
        UserAccessContext user = createUser(Set.of(1L), 1L, 10, false);
        DocumentAccessRule rule =
                createRule(true, AccessScope.RESTRICTED, Set.of(1L), Set.of(1L), 1, true,
                        ConditionOperator.AND);

        assertFalse(evaluator.canAccess(user, rule));
    }

    @Test
    void allowsAccessWhenAccessScopeIsAll() {
        UserAccessContext user = createUser(Set.of(), null, 1, false);
        DocumentAccessRule rule =
                createRule(true, AccessScope.ALL, Set.of(), Set.of(), null, false, ConditionOperator.AND);

        assertTrue(evaluator.canAccess(user, rule));
    }

    @Test
    void deniesAccessWhenRestrictedWithNoConditions() {
        Set<Long> allowedRoleIds = new HashSet<>(Set.of(1L));
        DocumentAccessRule rule = createRule(
                true,
                AccessScope.RESTRICTED,
                allowedRoleIds,
                Set.of(),
                null,
                false,
                ConditionOperator.AND);
        allowedRoleIds.clear();

        assertFalse(evaluator.canAccess(createUser(Set.of(), null, 1, false), rule));
    }

    @Test
    void evaluatesRoleCondition() {
        DocumentAccessRule rule =
                createRule(true, AccessScope.RESTRICTED, Set.of(2L), Set.of(), null, false,
                        ConditionOperator.AND);

        assertTrue(evaluator.canAccess(createUser(Set.of(1L, 2L), 1L, 1, false), rule));
        assertFalse(evaluator.canAccess(createUser(Set.of(1L), 1L, 1, false), rule));
    }

    @Test
    void evaluatesDepartmentCondition() {
        DocumentAccessRule rule =
                createRule(true, AccessScope.RESTRICTED, Set.of(), Set.of(2L), null, false,
                        ConditionOperator.AND);

        assertTrue(evaluator.canAccess(createUser(Set.of(), 2L, 1, false), rule));
        assertFalse(evaluator.canAccess(createUser(Set.of(), 1L, 1, false), rule));
    }

    @Test
    void deniesDepartmentConditionWithoutExceptionWhenUserDepartmentIsNull() {
        UserAccessContext user = createUser(Set.of(), null, 1, false);
        DocumentAccessRule rule =
                createRule(true, AccessScope.RESTRICTED, Set.of(), Set.of(2L), null, false,
                        ConditionOperator.AND);

        assertFalse(evaluator.canAccess(user, rule));
    }

    @Test
    void evaluatesMinimumJobGradeCondition() {
        DocumentAccessRule rule =
                createRule(true, AccessScope.RESTRICTED, Set.of(), Set.of(), 3, false,
                        ConditionOperator.AND);

        assertTrue(evaluator.canAccess(createUser(Set.of(), 1L, 3, false), rule));
        assertTrue(evaluator.canAccess(createUser(Set.of(), 1L, 4, false), rule));
        assertFalse(evaluator.canAccess(createUser(Set.of(), 1L, 2, false), rule));
    }

    @Test
    void requiresAllConfiguredConditionsForAndOperator() {
        DocumentAccessRule rule =
                createRule(true, AccessScope.RESTRICTED, Set.of(1L), Set.of(2L), null, false,
                        ConditionOperator.AND);

        assertFalse(evaluator.canAccess(createUser(Set.of(1L), 3L, 1, false), rule));
        assertTrue(evaluator.canAccess(createUser(Set.of(1L), 2L, 1, false), rule));
    }

    @Test
    void requiresOneConfiguredConditionForOrOperator() {
        DocumentAccessRule rule =
                createRule(true, AccessScope.RESTRICTED, Set.of(1L), Set.of(2L), null, false,
                        ConditionOperator.OR);

        assertTrue(evaluator.canAccess(createUser(Set.of(1L), 3L, 1, false), rule));
    }

    @Test
    void allowsNewEmployeeWhoAlsoSatisfiesRoleCondition() {
        UserAccessContext user = createUser(Set.of(1L), 1L, 1, true);
        DocumentAccessRule rule =
                createRule(true, AccessScope.RESTRICTED, Set.of(1L), Set.of(), null, true,
                        ConditionOperator.AND);

        assertTrue(evaluator.canAccess(user, rule));
    }

    @Test
    void allowsNewEmployeeConditionToSatisfyOrOperator() {
        UserAccessContext user = createUser(Set.of(), 1L, 1, true);
        DocumentAccessRule rule = createRule(
                true,
                AccessScope.RESTRICTED,
                Set.of(99L),
                Set.of(),
                null,
                true,
                ConditionOperator.OR);

        assertTrue(evaluator.canAccess(user, rule));
    }

    private UserAccessContext createUser(
            Set<Long> roleIds,
            Long departmentId,
            int jobGradeLevel,
            boolean isNewEmployee) {
        return new UserAccessContext(roleIds, departmentId, jobGradeLevel, isNewEmployee);
    }

    private DocumentAccessRule createRule(
            boolean active,
            AccessScope accessScope,
            Set<Long> allowedRoleIds,
            Set<Long> allowedDepartmentIds,
            Integer minimumJobGradeLevel,
            boolean newEmployeeOnly,
            ConditionOperator conditionOperator) {
        return new DocumentAccessRule(
                active,
                accessScope,
                allowedRoleIds,
                allowedDepartmentIds,
                minimumJobGradeLevel,
                newEmployeeOnly,
                conditionOperator);
    }
}
