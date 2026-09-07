package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycle;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationCycleStatus;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationTemplate;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationTemplateRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationTemplateCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationTemplateUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@ExtendWith(MockitoExtension.class)
class EvaluationTemplateServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 12);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private EvaluationTemplateRepository templateRepository;
    @Mock
    private EvaluationCycleRepository cycleRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private EvaluationTemplateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EvaluationTemplateServiceImpl(
                templateRepository,
                cycleRepository,
                evaluationRepository,
                currentUserProvider,
                FIXED_CLOCK);
    }

    @Test
    void hrManagerCreatesTemplateInPlannedCycle() {
        givenRoles(RoleType.HR_MANAGER);
        givenCycle(1L, plannedCycle());
        when(templateRepository.save(any(EvaluationTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(createRequest(EvaluationType.SELF));

        ArgumentCaptor<EvaluationTemplate> captor =
                ArgumentCaptor.forClass(EvaluationTemplate.class);
        verify(templateRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getCreatedBy());
        assertEquals(EvaluationType.SELF, captor.getValue().getEvaluationType());
    }

    @Test
    void systemAdminAloneCannotCreateTemplate() {
        givenRoles(RoleType.SYSTEM_ADMIN);

        assertError(EvaluationErrorCode.EVALUATION_ACCESS_DENIED,
                () -> service.create(createRequest(EvaluationType.SELF)));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void missingCycleIsReported() {
        givenRoles(RoleType.HR_MANAGER);
        when(cycleRepository.findById(99L)).thenReturn(Optional.empty());

        EvaluationTemplateCreateRequest request = new EvaluationTemplateCreateRequest(
                99L, "Self", EvaluationType.SELF, null, true);
        assertError(EvaluationErrorCode.EVALUATION_CYCLE_NOT_FOUND,
                () -> service.create(request));
    }

    @Test
    void openCycleRejectsCreate() {
        givenRoles(RoleType.HR_MANAGER);
        givenCycle(1L, openCycle());

        assertError(EvaluationErrorCode.EVALUATION_CYCLE_NOT_EDITABLE,
                () -> service.create(createRequest(EvaluationType.SELF)));
    }

    @Test
    void closedCycleRejectsCreate() {
        givenRoles(RoleType.HR_MANAGER);
        givenCycle(1L, closedCycle());

        assertError(EvaluationErrorCode.EVALUATION_CYCLE_NOT_EDITABLE,
                () -> service.create(createRequest(EvaluationType.SELF)));
    }

    @Test
    void duplicateTypeInCycleIsRejected() {
        givenRoles(RoleType.HR_MANAGER);
        givenCycle(1L, plannedCycle());
        when(templateRepository.existsByEvaluationCycleIdAndEvaluationType(
                1L, EvaluationType.SELF)).thenReturn(true);

        assertError(EvaluationErrorCode.EVALUATION_TEMPLATE_DUPLICATE_TYPE,
                () -> service.create(createRequest(EvaluationType.SELF)));
    }

    @Test
    void selfAndManagerTemplatesCanBeCreatedIndependently() {
        givenRoles(RoleType.HR_MANAGER);
        givenCycle(1L, plannedCycle());
        when(templateRepository.save(any(EvaluationTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(createRequest(EvaluationType.SELF));
        service.create(createRequest(EvaluationType.MANAGER));

        verify(templateRepository).existsByEvaluationCycleIdAndEvaluationType(
                1L, EvaluationType.SELF);
        verify(templateRepository).existsByEvaluationCycleIdAndEvaluationType(
                1L, EvaluationType.MANAGER);
    }

    @Test
    void plannedCycleAllowsTemplateUpdate() {
        givenRoles(RoleType.HR_MANAGER);
        EvaluationTemplate template = template(EvaluationType.SELF);
        givenTemplate(5L, template);
        givenCycle(1L, plannedCycle());

        service.update(5L, updateRequest(EvaluationType.MANAGER));

        assertEquals("Changed", template.getTemplateName());
        assertEquals(EvaluationType.MANAGER, template.getEvaluationType());
        assertEquals(false, template.getIsActive());
    }

    @Test
    void openCycleRejectsTemplateUpdate() {
        givenRoles(RoleType.HR_MANAGER);
        givenTemplate(5L, template(EvaluationType.SELF));
        givenCycle(1L, openCycle());

        assertError(EvaluationErrorCode.EVALUATION_CYCLE_NOT_EDITABLE,
                () -> service.update(5L, updateRequest(EvaluationType.SELF)));
    }

    @Test
    void closedCycleRejectsTemplateUpdate() {
        givenRoles(RoleType.HR_MANAGER);
        givenTemplate(5L, template(EvaluationType.SELF));
        givenCycle(1L, closedCycle());

        assertError(EvaluationErrorCode.EVALUATION_CYCLE_NOT_EDITABLE,
                () -> service.update(5L, updateRequest(EvaluationType.SELF)));
    }

    @Test
    void templateReferencedByEvaluationCannotBeUpdated() {
        givenRoles(RoleType.HR_MANAGER);
        givenTemplate(5L, template(EvaluationType.SELF));
        givenCycle(1L, plannedCycle());
        when(evaluationRepository.existsByEvaluationTemplateId(5L)).thenReturn(true);

        assertError(EvaluationErrorCode.EVALUATION_TEMPLATE_NOT_EDITABLE,
                () -> service.update(5L, updateRequest(EvaluationType.SELF)));
    }

    @Test
    void missingTemplateIsReportedForGetAndUpdate() {
        givenRoles(RoleType.HR_MANAGER);
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        assertError(EvaluationErrorCode.EVALUATION_TEMPLATE_NOT_FOUND,
                () -> service.getById(99L));
        assertError(EvaluationErrorCode.EVALUATION_TEMPLATE_NOT_FOUND,
                () -> service.update(99L, updateRequest(EvaluationType.SELF)));
    }

    private void givenRoles(RoleType... roles) {
        when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserContext(
                10L, 20L, Set.of(roles), 30L, 1, null));
    }

    private void givenCycle(Long id, EvaluationCycle cycle) {
        when(cycleRepository.findById(id)).thenReturn(Optional.of(cycle));
    }

    private void givenTemplate(Long id, EvaluationTemplate template) {
        when(templateRepository.findById(id)).thenReturn(Optional.of(template));
    }

    private EvaluationTemplateCreateRequest createRequest(EvaluationType type) {
        return new EvaluationTemplateCreateRequest(1L, "Template", type, "Description", true);
    }

    private EvaluationTemplateUpdateRequest updateRequest(EvaluationType type) {
        return new EvaluationTemplateUpdateRequest("Changed", type, "Changed description", false);
    }

    private EvaluationTemplate template(EvaluationType type) {
        return new EvaluationTemplate(1L, "Template", type, "Description", true, 10L);
    }

    private EvaluationCycle plannedCycle() {
        return cycle(TODAY.plusDays(1), TODAY.plusDays(10));
    }

    private EvaluationCycle openCycle() {
        return cycle(TODAY.minusDays(1), TODAY.plusDays(10));
    }

    private EvaluationCycle closedCycle() {
        return cycle(TODAY.minusDays(10), TODAY.minusDays(1));
    }

    private EvaluationCycle cycle(LocalDate startDate, LocalDate endDate) {
        return new EvaluationCycle(
                "Cycle", startDate, endDate, endDate.plusDays(1),
                EvaluationCycleStatus.PLANNED, 10L);
    }

    private void assertError(EvaluationErrorCode expected, Runnable action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertSame(expected, exception.getErrorCode());
    }
}
