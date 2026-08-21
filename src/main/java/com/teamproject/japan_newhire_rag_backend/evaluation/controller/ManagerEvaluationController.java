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

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationProgressResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationSummaryResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationSubmissionService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.ManagerEvaluationProgressService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.ManagerEvaluationService;

@RestController
@RequestMapping("/api/manager/evaluations")
public class ManagerEvaluationController {

    private final ManagerEvaluationService managerEvaluationService;
    private final EvaluationSubmissionService submissionService;
    private final ManagerEvaluationProgressService progressService;

    public ManagerEvaluationController(
            ManagerEvaluationService managerEvaluationService,
            EvaluationSubmissionService submissionService,
            ManagerEvaluationProgressService progressService
    ) {
        this.managerEvaluationService = managerEvaluationService;
        this.submissionService = submissionService;
        this.progressService = progressService;
    }

    @GetMapping
    public List<ManagerEvaluationSummaryResponse> getAssignedEvaluations() {
        return managerEvaluationService.getMyAssignedEvaluations();
    }

    @GetMapping("/{evaluationId}")
    public ManagerEvaluationResponse getManagerEvaluation(
            @PathVariable Long evaluationId
    ) {
        return managerEvaluationService.getMyManagerEvaluation(evaluationId);
    }

    @PutMapping("/{evaluationId}/draft")
    public ManagerEvaluationResponse saveManagerDraft(
            @PathVariable Long evaluationId,
            @RequestBody ManagerEvaluationDraftRequest request
    ) {
        return managerEvaluationService.saveDraft(evaluationId, request);
    }

    @PostMapping("/{evaluationId}/submission")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitManager(@PathVariable Long evaluationId) {
        submissionService.submitManager(evaluationId);
    }

    @GetMapping("/progress")
    public ManagerEvaluationProgressResponse getManagedProgress(
            @RequestParam Long cycleId
    ) {
        return progressService.getManagedProgress(cycleId);
    }
}
