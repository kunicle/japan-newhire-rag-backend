package com.teamproject.japan_newhire_rag_backend.evaluation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.ManagerEvaluationDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationSubmissionService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.ManagerEvaluationProgressService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.ManagerEvaluationService;

class ManagerEvaluationControllerTest {

    private ManagerEvaluationService managerService;
    private EvaluationSubmissionService submissionService;
    private ManagerEvaluationProgressService progressService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        managerService = mock(ManagerEvaluationService.class);
        submissionService = mock(EvaluationSubmissionService.class);
        progressService = mock(ManagerEvaluationProgressService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ManagerEvaluationController(
                managerService, submissionService, progressService)).build();
    }

    @Test
    void mapsManagerEvaluationEndpointsAndDraftBody() throws Exception {
        mockMvc.perform(get("/api/manager/evaluations"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/manager/evaluations/10"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/manager/evaluations/10/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"evaluationItemId":20,"score":4.0,
                                "itemFeedback":"good"}],"overallFeedback":"overall"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/manager/evaluations/10/submission"))
                .andExpect(status().isNoContent());

        verify(managerService).getMyAssignedEvaluations();
        verify(managerService).getMyManagerEvaluation(10L);
        ArgumentCaptor<ManagerEvaluationDraftRequest> request =
                ArgumentCaptor.forClass(ManagerEvaluationDraftRequest.class);
        verify(managerService).saveDraft(
                org.mockito.ArgumentMatchers.eq(10L), request.capture());
        assertEquals("overall", request.getValue().overallFeedback());
        verify(submissionService).submitManager(10L);
    }

    @Test
    void passesProgressCycleQueryParameter() throws Exception {
        mockMvc.perform(get("/api/manager/evaluations/progress").param("cycleId", "7"))
                .andExpect(status().isOk());

        verify(progressService).getManagedProgress(7L);
    }
}
