package com.teamproject.japan_newhire_rag_backend.rag.controller.dto;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.rag.application.RagQueryResult;

public record RagQueryResponse(
        boolean hasSufficientEvidence,
        String answer,
        List<Long> validCitedChunkIds) {

    public static RagQueryResponse from(RagQueryResult result) {
        return new RagQueryResponse(
                result.hasSufficientEvidence(),
                result.answer(),
                result.validCitedChunkIds());
    }
}
