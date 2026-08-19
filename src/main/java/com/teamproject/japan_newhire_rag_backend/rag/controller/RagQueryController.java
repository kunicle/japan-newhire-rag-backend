package com.teamproject.japan_newhire_rag_backend.rag.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.rag.application.RagQueryExecutionService;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQueryResult;
import com.teamproject.japan_newhire_rag_backend.rag.controller.dto.RagQueryRequest;
import com.teamproject.japan_newhire_rag_backend.rag.controller.dto.RagQueryResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rag/questions")
public class RagQueryController {

    private final RagQueryExecutionService ragQueryExecutionService;

    public RagQueryController(RagQueryExecutionService ragQueryExecutionService) {
        this.ragQueryExecutionService = ragQueryExecutionService;
    }

    @PostMapping
    public RagQueryResponse createQuestion(
            @Valid @RequestBody RagQueryRequest request) {
        RagQueryResult result =
                ragQueryExecutionService.execute(request.question());

        return RagQueryResponse.from(result);
    }
}
