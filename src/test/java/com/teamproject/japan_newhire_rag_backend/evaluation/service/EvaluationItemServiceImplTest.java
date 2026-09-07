package com.teamproject.japan_newhire_rag_backend.evaluation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationItem;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationItemRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationTemplate;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationTemplateRepository;
import com.teamproject.japan_newhire_rag_backend.evaluation.EvaluationType;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemCreateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemResponse;
import com.teamproject.japan_newhire_rag_backend.evaluation.dto.EvaluationItemUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

@ExtendWith(MockitoExtension.class)
class EvaluationItemServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 12);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private EvaluationItemRepository itemRepository;
    @Mock
    private EvaluationTemplateRepository templateRepository;
    @Mock
    private EvaluationCycleRepository cycleRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private EvaluationItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EvaluationItemServiceImpl(
                itemRepository, templateRepository, cycleRepository,
                evaluationRepository, currentUserProvider, FIXED_CLOCK);
    }

    @Test
    void hrManagerCreatesItemInPlannedCycle() {
        givenHrManagerAndTemplateWith(plannedCycle());
        saveReturnsArgument();

        service.create(createRequest(1, "40.00", true));

        verify(itemRepository).save(any(EvaluationItem.class));
    }

    @Test
    void systemAdminAloneCannotCreateItem() {
        givenRoles(RoleType.SYSTEM_ADMIN);

        assertError(EvaluationErrorCode.EVALUATION_ACCESS_DENIED,
                () -> service.create(createRequest(1, "40.00", true)));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void missingTemplateIsReported() {
        givenRoles(RoleType.HR_MANAGER);
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        EvaluationItemCreateRequest request = new EvaluationItemCreateRequest(
                99L, "Item", null, 1, new BigDecimal("40.00"), true, 1, 5);
        assertError(EvaluationErrorCode.EVALUATION_TEMPLATE_NOT_FOUND,
                () -> service.create(request));
    }

    @Test
    void openCycleRejectsCreate() {
        givenHrManagerAndTemplateWith(openCycle());

        assertError(EvaluationErrorCode.EVALUATION_ITEM_NOT_EDITABLE,
                () -> service.create(createRequest(1, "40.00", true)));
    }

    @Test
    void closedCycleRejectsCreate() {
        givenHrManagerAndTemplateWith(closedCycle());

        assertError(EvaluationErrorCode.EVALUATION_ITEM_NOT_EDITABLE,
                () -> service.create(createRequest(1, "40.00", true)));
    }

    @Test
    void duplicateOrderInTemplateIsRejected() {
        givenHrManagerAndTemplateWith(plannedCycle());
        when(itemRepository.existsByEvaluationTemplateIdAndItemOrder(5L, 1)).thenReturn(true);

        assertError(EvaluationErrorCode.EVALUATION_ITEM_DUPLICATE_ORDER,
                () -> service.create(createRequest(1, "40.00", true)));
    }

    @Test
    void itemIsUpdatedInPlannedCycle() {
        EvaluationItem item = givenItemForUpdate(plannedCycle());

        service.update(7L, updateRequest(2, "55.50", false));

        assertEquals("Changed", item.getItemName());
        assertEquals(2, item.getItemOrder());
        assertEquals(new BigDecimal("55.50"), item.getWeight());
        assertFalse(item.getIsRequired());
    }

    @Test
    void openCycleRejectsUpdate() {
        givenItemForUpdate(openCycle());

        assertError(EvaluationErrorCode.EVALUATION_ITEM_NOT_EDITABLE,
                () -> service.update(7L, updateRequest(2, "50.00", true)));
    }

    @Test
    void closedCycleRejectsUpdate() {
        givenItemForUpdate(closedCycle());

        assertError(EvaluationErrorCode.EVALUATION_ITEM_NOT_EDITABLE,
                () -> service.update(7L, updateRequest(2, "50.00", true)));
    }

    @Test
    void evaluationPreventsItemCreation() {
        givenHrManagerAndTemplateWith(plannedCycle());
        when(evaluationRepository.existsByEvaluationTemplateId(5L)).thenReturn(true);

        assertError(EvaluationErrorCode.EVALUATION_ITEM_NOT_EDITABLE,
                () -> service.create(createRequest(1, "40.00", true)));
    }

    @Test
    void evaluationPreventsItemUpdate() {
        givenItemForUpdate(plannedCycle());
        when(evaluationRepository.existsByEvaluationTemplateId(5L)).thenReturn(true);

        assertError(EvaluationErrorCode.EVALUATION_ITEM_NOT_EDITABLE,
                () -> service.update(7L, updateRequest(2, "50.00", true)));
    }

    @Test
    void requiredTrueIsStored() {
        givenHrManagerAndTemplateWith(plannedCycle());
        saveReturnsArgument();

        service.create(createRequest(1, "40.00", true));

        assertTrue(capturedSavedItem().getIsRequired());
    }

    @Test
    void requiredFalseIsStored() {
        givenHrManagerAndTemplateWith(plannedCycle());
        saveReturnsArgument();

        service.create(createRequest(1, "40.00", false));

        assertFalse(capturedSavedItem().getIsRequired());
    }

    @Test
    void validWeightIsStored() {
        givenHrManagerAndTemplateWith(plannedCycle());
        saveReturnsArgument();

        service.create(createRequest(1, "12.34", true));

        assertEquals(new BigDecimal("12.34"), capturedSavedItem().getWeight());
    }

    @Test
    void totalWeightOfOneHundredIsNotRequired() {
        givenHrManagerAndTemplateWith(plannedCycle());
        saveReturnsArgument();

        service.create(createRequest(1, "10.00", true));
        service.create(createRequest(2, "20.00", true));

        verify(itemRepository, org.mockito.Mockito.times(2)).save(any(EvaluationItem.class));
    }

    @Test
    void itemsAreReadInRepositoryOrder() {
        givenRoles(RoleType.HR_MANAGER);
        givenTemplate();
        EvaluationItem first = item(1);
        EvaluationItem second = item(2);
        when(itemRepository.findByEvaluationTemplateIdOrderByItemOrderAsc(5L))
                .thenReturn(List.of(first, second));

        List<EvaluationItemResponse> responses = service.getByTemplateId(5L);

        assertEquals(List.of(1, 2), responses.stream().map(EvaluationItemResponse::itemOrder).toList());
    }

    @Test
    void missingItemIsReportedForGet() {
        givenRoles(RoleType.HR_MANAGER);
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertError(EvaluationErrorCode.EVALUATION_ITEM_NOT_FOUND,
                () -> service.getById(99L));
    }

    private void givenRoles(RoleType... roles) {
        when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserContext(
                10L, 20L, Set.of(roles), 30L, 1, null));
    }

    private void givenHrManagerAndTemplateWith(EvaluationCycle cycle) {
        givenRoles(RoleType.HR_MANAGER);
        givenTemplate();
        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
    }

    private void givenTemplate() {
        when(templateRepository.findById(5L)).thenReturn(Optional.of(template()));
    }

    private EvaluationItem givenItemForUpdate(EvaluationCycle cycle) {
        givenRoles(RoleType.HR_MANAGER);
        EvaluationItem item = item(1);
        when(itemRepository.findById(7L)).thenReturn(Optional.of(item));
        givenTemplate();
        when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
        return item;
    }

    private void saveReturnsArgument() {
        when(itemRepository.save(any(EvaluationItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private EvaluationItem capturedSavedItem() {
        ArgumentCaptor<EvaluationItem> captor = ArgumentCaptor.forClass(EvaluationItem.class);
        verify(itemRepository).save(captor.capture());
        return captor.getValue();
    }

    private EvaluationItemCreateRequest createRequest(
            int order, String weight, boolean required) {
        return new EvaluationItemCreateRequest(
                5L, "Item", "Description", order, new BigDecimal(weight), required, 1, 5);
    }

    private EvaluationItemUpdateRequest updateRequest(
            int order, String weight, boolean required) {
        return new EvaluationItemUpdateRequest(
                "Changed", "Changed description", order,
                new BigDecimal(weight), required, 1, 5);
    }

    private EvaluationItem item(int order) {
        return new EvaluationItem(
                5L, "Item " + order, null, order,
                new BigDecimal("50.00"), true, 1, 5);
    }

    private EvaluationTemplate template() {
        return new EvaluationTemplate(
                1L, "Template", EvaluationType.SELF, null, true, 10L);
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
