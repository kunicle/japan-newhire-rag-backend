package com.teamproject.japan_newhire_rag_backend.evaluation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

import com.teamproject.japan_newhire_rag_backend.evaluation.dto.SelfEvaluationDraftRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationResultService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationSubmissionService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.SelfEvaluationService;

class MyEvaluationControllerTest {

    private SelfEvaluationService selfService;
    private EvaluationSubmissionService submissionService;
    private EvaluationResultService resultService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        selfService = mock(SelfEvaluationService.class);
        submissionService = mock(EvaluationSubmissionService.class);
        resultService = mock(EvaluationResultService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MyEvaluationController(
                selfService, submissionService, resultService)).build();
    }

    @Test
    void mapsSelfEvaluationEndpointsAndDraftBody() throws Exception {
        mockMvc.perform(get("/api/me/evaluations/10/self"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/me/evaluations/10/self/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"evaluationItemId":20,"score":4.5,
                                "itemFeedback":"good"}],"overallFeedback":"overall"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/me/evaluations/10/self/submission"))
                .andExpect(status().isNoContent());

        verify(selfService).getMySelfEvaluation(10L);
        ArgumentCaptor<SelfEvaluationDraftRequest> request =
                ArgumentCaptor.forClass(SelfEvaluationDraftRequest.class);
        verify(selfService).saveDraft(org.mockito.ArgumentMatchers.eq(10L), request.capture());
        assertEquals("overall", request.getValue().overallFeedback());
        assertEquals(20L, request.getValue().items().get(0).evaluationItemId());
        verify(submissionService).submitSelf(10L);
    }

    @Test
    void passesResultCycleQueryParameter() throws Exception {
        mockMvc.perform(get("/api/me/evaluations/result").param("cycleId", "7"))
                .andExpect(status().isOk());

        verify(resultService).getMyResult(7L);
    }
}
