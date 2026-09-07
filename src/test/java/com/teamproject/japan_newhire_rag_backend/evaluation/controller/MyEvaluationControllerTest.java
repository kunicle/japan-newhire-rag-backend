package com.teamproject.japan_newhire_rag_backend.evaluation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.MyEvaluationSummaryResponse;
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
    void listsMyEvaluationSummariesWithoutDetailFields() throws Exception {
        when(selfService.getMyEvaluations()).thenReturn(List.of(
                new MyEvaluationSummaryResponse(
                        20L, 200L, "2026 하반기",
                        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31),
                        EvaluationStatus.DRAFT, EvaluationCycleStatus.OPEN),
                new MyEvaluationSummaryResponse(
                        10L, 100L, "2026 상반기",
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                        EvaluationStatus.SUBMITTED, EvaluationCycleStatus.CLOSED)));

        mockMvc.perform(get("/api/me/evaluations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evaluationId").value(20))
                .andExpect(jsonPath("$[0].evaluationCycleId").value(200))
                .andExpect(jsonPath("$[0].cycleName").value("2026 하반기"))
                .andExpect(jsonPath("$[0].cycleStartDate").value("2026-07-01"))
                .andExpect(jsonPath("$[0].cycleEndDate").value("2026-12-31"))
                .andExpect(jsonPath("$[0].evaluationStatus").value("DRAFT"))
                .andExpect(jsonPath("$[0].currentCycleStatus").value("OPEN"))
                .andExpect(jsonPath("$[1].evaluationId").value(10))
                .andExpect(jsonPath("$[0].items").doesNotExist())
                .andExpect(jsonPath("$[0].overallFeedback").doesNotExist())
                .andExpect(jsonPath("$[0].score").doesNotExist())
                .andExpect(jsonPath("$[0].itemFeedback").doesNotExist())
                .andExpect(jsonPath("$[0].manager").doesNotExist())
                .andExpect(jsonPath("$[0].managerFeedback").doesNotExist())
                .andExpect(jsonPath("$[0].managerScore").doesNotExist());

        verify(selfService).getMyEvaluations();
    }

    @Test
    void returnsEmptyEvaluationSummaryList() throws Exception {
        when(selfService.getMyEvaluations()).thenReturn(List.of());

        mockMvc.perform(get("/api/me/evaluations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
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
