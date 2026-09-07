package com.teamproject.japan_newhire_rag_backend.rag.evidence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class EvidenceThresholdCheckerTest {

    private final EvidenceThresholdChecker checker = new EvidenceThresholdChecker();

    @Test
    void returnsFalseWhenSimilarityScoresIsNull() {
        assertFalse(checker.hasSufficientEvidence(null, 0.7));
    }

    @Test
    void returnsFalseWhenSimilarityScoresIsEmpty() {
        assertFalse(checker.hasSufficientEvidence(List.of(), 0.7));
    }

    @Test
    void returnsFalseWhenAllScoresAreBelowThreshold() {
        assertFalse(checker.hasSufficientEvidence(List.of(0.3, 0.5, 0.6), 0.7));
    }

    @Test
    void returnsTrueWhenMaximumScoreEqualsThreshold() {
        assertTrue(checker.hasSufficientEvidence(List.of(0.5, 0.7), 0.7));
    }

    @Test
    void returnsTrueWhenMaximumScoreExceedsThreshold() {
        assertTrue(checker.hasSufficientEvidence(List.of(0.9), 0.7));
    }

    @Test
    void returnsTrueWhenQualifyingScoreAppearsAmongLowerScores() {
        assertTrue(checker.hasSufficientEvidence(List.of(0.1, 0.95, 0.2), 0.7));
    }

    @Test
    void evaluatesSingleScoreAgainstThreshold() {
        assertFalse(checker.hasSufficientEvidence(List.of(0.6), 0.7));
        assertTrue(checker.hasSufficientEvidence(List.of(0.7), 0.7));
    }
}
