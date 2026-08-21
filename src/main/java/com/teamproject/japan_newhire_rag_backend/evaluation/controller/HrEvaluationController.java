package com.teamproject.japan_newhire_rag_backend.evaluation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationAssignmentRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationAssignmentResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationCycleCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationCycleResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationCycleUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationProgressResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationTemplateCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationTemplateResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationTemplateUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationAssignmentService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationCycleService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationItemService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationProgressService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationPublishService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationTemplateService;

@RestController
@RequestMapping("/api/hr")
public class HrEvaluationController {

    private final EvaluationCycleService cycleService;
    private final EvaluationTemplateService templateService;
    private final EvaluationItemService itemService;
    private final EvaluationAssignmentService assignmentService;
    private final EvaluationProgressService progressService;
    private final EvaluationPublishService publishService;

    public HrEvaluationController(
            EvaluationCycleService cycleService,
            EvaluationTemplateService templateService,
            EvaluationItemService itemService,
            EvaluationAssignmentService assignmentService,
            EvaluationProgressService progressService,
            EvaluationPublishService publishService
    ) {
        this.cycleService = cycleService;
        this.templateService = templateService;
        this.itemService = itemService;
        this.assignmentService = assignmentService;
        this.progressService = progressService;
        this.publishService = publishService;
    }

    @PostMapping("/evaluation-cycles")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationCycleResponse createCycle(
            @RequestBody EvaluationCycleCreateRequest request
    ) {
        return cycleService.create(request);
    }

    @GetMapping("/evaluation-cycles/{cycleId}")
    public EvaluationCycleResponse getCycle(@PathVariable Long cycleId) {
        return cycleService.getById(cycleId);
    }

    @PatchMapping("/evaluation-cycles/{cycleId}")
    public EvaluationCycleResponse updateCycle(
            @PathVariable Long cycleId,
            @RequestBody EvaluationCycleUpdateRequest request
    ) {
        return cycleService.update(cycleId, request);
    }

    @GetMapping("/evaluation-cycles/{cycleId}/status")
    public EvaluationCycleStatus getCycleStatus(@PathVariable Long cycleId) {
        return cycleService.getCurrentStatus(cycleId);
    }

    @PostMapping("/evaluation-templates")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationTemplateResponse createTemplate(
            @RequestBody EvaluationTemplateCreateRequest request
    ) {
        return templateService.create(request);
    }

    @GetMapping("/evaluation-templates/{templateId}")
    public EvaluationTemplateResponse getTemplate(@PathVariable Long templateId) {
        return templateService.getById(templateId);
    }

    @GetMapping("/evaluation-cycles/{cycleId}/templates")
    public List<EvaluationTemplateResponse> getTemplates(@PathVariable Long cycleId) {
        return templateService.getByCycleId(cycleId);
    }

    @PatchMapping("/evaluation-templates/{templateId}")
    public EvaluationTemplateResponse updateTemplate(
            @PathVariable Long templateId,
            @RequestBody EvaluationTemplateUpdateRequest request
    ) {
        return templateService.update(templateId, request);
    }

    @PostMapping("/evaluation-items")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationItemResponse createItem(
            @RequestBody EvaluationItemCreateRequest request
    ) {
        return itemService.create(request);
    }

    @GetMapping("/evaluation-items/{itemId}")
    public EvaluationItemResponse getItem(@PathVariable Long itemId) {
        return itemService.getById(itemId);
    }

    @GetMapping("/evaluation-templates/{templateId}/items")
    public List<EvaluationItemResponse> getItems(@PathVariable Long templateId) {
        return itemService.getByTemplateId(templateId);
    }

    @PatchMapping("/evaluation-items/{itemId}")
    public EvaluationItemResponse updateItem(
            @PathVariable Long itemId,
            @RequestBody EvaluationItemUpdateRequest request
    ) {
        return itemService.update(itemId, request);
    }

    @PostMapping("/evaluation-assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationAssignmentResponse assign(
            @RequestBody EvaluationAssignmentRequest request
    ) {
        return assignmentService.assign(request);
    }

    @GetMapping("/evaluations/progress")
    public EvaluationProgressResponse getProgress(@RequestParam Long cycleId) {
        return progressService.getCycleProgress(cycleId);
    }

    @PatchMapping("/evaluations/{id}/publish")
    public EvaluationPublishResponse publish(
            @PathVariable Long id,
            @RequestBody(required = false) EvaluationPublishRequest request
    ) {
        return publishService.publish(id, request);
    }
}
