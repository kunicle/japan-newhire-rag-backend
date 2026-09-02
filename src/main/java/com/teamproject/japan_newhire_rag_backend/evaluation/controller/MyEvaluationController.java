package com.teamproject.japan_newhire_rag_backend.evaluation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationResultResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.MyEvaluationSummaryResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationResultService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationSubmissionService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.SelfEvaluationService;

@RestController
@RequestMapping("/api/me/evaluations")
public class MyEvaluationController {

    private final SelfEvaluationService selfEvaluationService;
    private final EvaluationSubmissionService submissionService;
    private final EvaluationResultService resultService;

    public MyEvaluationController(
            SelfEvaluationService selfEvaluationService,
            EvaluationSubmissionService submissionService,
            EvaluationResultService resultService
    ) {
        this.selfEvaluationService = selfEvaluationService;
        this.submissionService = submissionService;
        this.resultService = resultService;
    }

    @GetMapping
    public List<MyEvaluationSummaryResponse> getMyEvaluations() {
        return selfEvaluationService.getMyEvaluations();
    }

    @GetMapping("/{evaluationId}/self")
    public SelfEvaluationResponse getSelfEvaluation(@PathVariable Long evaluationId) {
        return selfEvaluationService.getMySelfEvaluation(evaluationId);
    }

    @PutMapping("/{evaluationId}/self/draft")
    public SelfEvaluationResponse saveSelfDraft(
            @PathVariable Long evaluationId,
            @RequestBody SelfEvaluationDraftRequest request
    ) {
        return selfEvaluationService.saveDraft(evaluationId, request);
    }

    @PostMapping("/{evaluationId}/self/submission")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitSelf(@PathVariable Long evaluationId) {
        submissionService.submitSelf(evaluationId);
    }

    @GetMapping("/result")
    public EvaluationResultResponse getMyResult(@RequestParam Long cycleId) {
        return resultService.getMyResult(cycleId);
    }
}
