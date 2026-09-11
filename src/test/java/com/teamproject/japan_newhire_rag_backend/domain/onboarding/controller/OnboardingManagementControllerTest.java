package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingAssignmentCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingAssignmentCreateResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.OnboardingAssignmentService;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.OnboardingManagementService;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.OnboardingTaskService;

@ExtendWith(MockitoExtension.class)
class OnboardingManagementControllerTest {

    @Mock
    private OnboardingManagementService managementService;

    @Mock
    private OnboardingAssignmentService assignmentService;

    @Mock
    private OnboardingTaskService taskService;

    private OnboardingManagementController controller;

    @BeforeEach
    void setUp() {
        controller = new OnboardingManagementController(
                managementService,
                assignmentService,
                taskService);
    }

    @Test
    void getManagedTasksReturnsActiveTaskPage() {
        OnboardingTaskPageResponse expected =
                new OnboardingTaskPageResponse(
                        List.of(),
                        0,
                        20,
                        0,
                        0,
                        true,
                        true);

        when(taskService.getManagedTasks(0, 20))
                .thenReturn(expected);

        assertSame(expected, controller.getManagedTasks(0, 20));
        verify(taskService).getManagedTasks(0, 20);
    }

    @Test
    void assignManagedReturnsCreatedResponse() {
        OnboardingAssignmentCreateRequest request =
                new OnboardingAssignmentCreateRequest(
                        List.of(101L, 102L));
        OnboardingAssignmentCreateResponse expected =
                new OnboardingAssignmentCreateResponse(
                        10L,
                        2,
                        2,
                        0);

        when(assignmentService.assignManaged(10L, request))
                .thenReturn(expected);

        ResponseEntity<OnboardingAssignmentCreateResponse>
                response = controller.assignManaged(
                        10L,
                        request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(assignmentService).assignManaged(10L, request);
    }
}
