package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.rag.RagAnswerStatus;

public record AiRagGenerateResponse(
        RagAnswerStatus status,
        String answer,
        List<Long> citedChunkIds) {

    public AiRagGenerateResponse {
        if (status == null) {
            throw new IllegalArgumentException("생성 상태가 없습니다.");
        }
        if (citedChunkIds == null) {
            throw new IllegalArgumentException("인용된 chunk 목록이 없습니다.");
        }
        if (status == RagAnswerStatus.ANSWERED
                && (answer == null || answer.isBlank())) {
            throw new IllegalArgumentException("답변 완료 응답에는 답변이 필요합니다.");
        }
    }
}
