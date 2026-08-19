package com.teamproject.japan_newhire_rag_backend.evaluation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.FeedbackType;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationAssignmentRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationCycleCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationCycleUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishPreviewFeedbackResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishPreviewResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationPublishRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationTemplateCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationTemplateUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationAssignmentService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationCycleService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationItemService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationProgressService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationPublishService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationTemplateService;

class HrEvaluationControllerTest {

    private EvaluationCycleService cycleService;
    private EvaluationTemplateService templateService;
    private EvaluationItemService itemService;
    private EvaluationAssignmentService assignmentService;
    private EvaluationProgressService progressService;
    private EvaluationPublishService publishService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cycleService = mock(EvaluationCycleService.class);
        templateService = mock(EvaluationTemplateService.class);
        itemService = mock(EvaluationItemService.class);
        assignmentService = mock(EvaluationAssignmentService.class);
        progressService = mock(EvaluationProgressService.class);
        publishService = mock(EvaluationPublishService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new HrEvaluationController(
                cycleService, templateService, itemService, assignmentService,
                progressService, publishService)).build();
    }

    @Test
    void mapsCycleEndpointsAndDeserializesRequests() throws Exception {
        when(cycleService.getCurrentStatus(7L)).thenReturn(EvaluationCycleStatus.OPEN);

        mockMvc.perform(post("/api/hr/evaluation-cycles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cycleName":"Review","startDate":"2026-01-01",
                                "endDate":"2026-06-30","plannedPublishDate":"2026-07-10"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/hr/evaluation-cycles/7"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/hr/evaluation-cycles/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cycleName":"Updated","startDate":"2026-01-01",
                                "endDate":"2026-06-30","plannedPublishDate":"2026-07-10"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/hr/evaluation-cycles/7/status"))
                .andExpect(status().isOk())
                .andExpect(content().json("\"OPEN\""));

        ArgumentCaptor<EvaluationCycleCreateRequest> create =
                ArgumentCaptor.forClass(EvaluationCycleCreateRequest.class);
        verify(cycleService).create(create.capture());
        assertEquals("Review", create.getValue().cycleName());
        verify(cycleService).getById(7L);
        verify(cycleService).update(
                org.mockito.ArgumentMatchers.eq(7L), any(EvaluationCycleUpdateRequest.class));
    }

    @Test
    void mapsTemplateAndItemEndpoints() throws Exception {
        mockMvc.perform(post("/api/hr/evaluation-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evaluationCycleId":7,"templateName":"Self",
                                "evaluationType":"SELF","templateDescription":"desc",
                                "isActive":true}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/hr/evaluation-templates/8"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/hr/evaluation-cycles/7/templates"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/hr/evaluation-templates/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateName":"Updated","templateDescription":"desc",
                                "isActive":true}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/hr/evaluation-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evaluationTemplateId":8,"itemName":"Quality",
                                "itemDescription":"desc","itemOrder":1,"weight":100,
                                "isRequired":true,"minimumScore":1,"maximumScore":5}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/hr/evaluation-items/9"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/hr/evaluation-templates/8/items"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/hr/evaluation-items/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemName":"Updated","itemDescription":"desc","itemOrder":1,
                                "weight":100,"isRequired":true,"minimumScore":1,"maximumScore":5}
                                """))
                .andExpect(status().isOk());

        verify(templateService).create(any(EvaluationTemplateCreateRequest.class));
        verify(templateService).getById(8L);
        verify(templateService).getByCycleId(7L);
        verify(templateService).update(
                org.mockito.ArgumentMatchers.eq(8L), any(EvaluationTemplateUpdateRequest.class));
        verify(itemService).create(any(EvaluationItemCreateRequest.class));
        verify(itemService).getById(9L);
        verify(itemService).getByTemplateId(8L);
        verify(itemService).update(
                org.mockito.ArgumentMatchers.eq(9L), any(EvaluationItemUpdateRequest.class));
    }

    @Test
    void mapsAssignmentProgressAndOfficialPublishEndpoint() throws Exception {
        when(publishService.getPublishPreview(30L)).thenReturn(
                new EvaluationPublishPreviewResponse(7L, 20L, 30L, 31L, List.of(
                        new EvaluationPublishPreviewFeedbackResponse(
                                11L, 9L, FeedbackType.ITEM, "feedback", false),
                        new EvaluationPublishPreviewFeedbackResponse(
                                12L, null, FeedbackType.OVERALL, "overall", true))));
        mockMvc.perform(post("/api/hr/evaluation-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationCycleId\":7,\"targetEmployeeId\":20}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/hr/evaluations/progress").param("cycleId", "7"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/hr/evaluations/30/publish-preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selfEvaluationId").value(30))
                .andExpect(jsonPath("$.managerEvaluationId").value(31))
                .andExpect(jsonPath("$.managerFeedbacks[0].evaluationFeedbackId").value(11))
                .andExpect(jsonPath("$.managerFeedbacks[0].evaluationItemId").value(9))
                .andExpect(jsonPath("$.managerFeedbacks[1].evaluationItemId").doesNotExist());
        mockMvc.perform(patch("/api/hr/evaluations/30/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"publishReason":"final","visibleManagerFeedbackIds":[11,12]}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<EvaluationAssignmentRequest> assignment =
                ArgumentCaptor.forClass(EvaluationAssignmentRequest.class);
        verify(assignmentService).assign(assignment.capture());
        assertEquals(20L, assignment.getValue().targetEmployeeId());
        verify(progressService).getCycleProgress(7L);
        verify(publishService).getPublishPreview(30L);
        ArgumentCaptor<EvaluationPublishRequest> publish =
                ArgumentCaptor.forClass(EvaluationPublishRequest.class);
        verify(publishService).publish(org.mockito.ArgumentMatchers.eq(30L), publish.capture());
        assertEquals(List.of(11L, 12L), publish.getValue().visibleManagerFeedbackIds());
    }
}
