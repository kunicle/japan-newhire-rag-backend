package com.teamproject.japan_newhire_rag_backend.rag.evidence;

import java.util.List;

public class EvidenceThresholdChecker {

    public boolean hasSufficientEvidence(List<Double> similarityScores, double threshold) {
        if (similarityScores == null || similarityScores.isEmpty()) {
            return false;
        }

        double maxScore = similarityScores.stream()
                .max(Double::compare)
                .orElseThrow();

        return maxScore >= threshold;
    }
}
