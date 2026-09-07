package com.teamproject.japan_newhire_rag_backend.rag.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQuestionHistoryDetail;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQuestionHistoryItem;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQuestionHistoryService;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQueryExecutionService;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQueryResult;
import com.teamproject.japan_newhire_rag_backend.rag.controller.dto.RagQuestionHistoryDetailResponse;
import com.teamproject.japan_newhire_rag_backend.rag.controller.dto.RagQuestionHistoryItemResponse;
import com.teamproject.japan_newhire_rag_backend.rag.controller.dto.RagQueryRequest;
import com.teamproject.japan_newhire_rag_backend.rag.controller.dto.RagQueryResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rag/questions")
public class RagQueryController {

    private final RagQueryExecutionService ragQueryExecutionService;
    private final CurrentUserProvider currentUserProvider;
    private final RagQuestionHistoryService ragQuestionHistoryService;

    public RagQueryController(
            RagQueryExecutionService ragQueryExecutionService,
            CurrentUserProvider currentUserProvider,
            RagQuestionHistoryService ragQuestionHistoryService) {
        this.ragQueryExecutionService = ragQueryExecutionService;
        this.currentUserProvider = currentUserProvider;
        this.ragQuestionHistoryService = ragQuestionHistoryService;
    }

    @PostMapping
    public RagQueryResponse createQuestion(
            @Valid @RequestBody RagQueryRequest request) {
        RagQueryResult result =
                ragQueryExecutionService.execute(request.question());

        return RagQueryResponse.from(result);
    }

    @GetMapping("/me")
    public List<RagQuestionHistoryItemResponse> getMyQuestionHistory() {
        CurrentUserContext currentUser = currentUserProvider.getCurrentUser();
        List<RagQuestionHistoryItem> history =
                ragQuestionHistoryService.getQuestionHistory(currentUser);
        return history.stream()
                .map(RagQuestionHistoryItemResponse::from)
                .toList();
    }

    @GetMapping("/{questionId}")
    public RagQuestionHistoryDetailResponse getQuestionDetail(
            @PathVariable Long questionId) {
        CurrentUserContext currentUser = currentUserProvider.getCurrentUser();
        RagQuestionHistoryDetail detail =
                ragQuestionHistoryService.getQuestionDetail(currentUser, questionId);
        return RagQuestionHistoryDetailResponse.from(detail);
    }
}
