package com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.ChangeDirectManagerRequest;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.ManagerRelation;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.error.OrganizationErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.ManagerRelationRepository;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.internal.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.internal.AuditLogRecordService;

@ExtendWith(MockitoExtension.class)
class DirectManagerCommandServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 13, 5, 0);

    @Mock EmployeeRepository employeeRepository;
    @Mock ManagerRelationRepository managerRelationRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock AuditLogRecordService auditLogRecordService;
    DirectManagerCommandService service;

    @BeforeEach
    void setUp() {
        service = new DirectManagerCommandService(
                employeeRepository,
                managerRelationRepository,
                appUserRepository,
                currentUserProvider,
                auditLogRecordService,
                Clock.fixed(Instant.parse("2026-08-13T05:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsFirstDirectManagerRelationAndAudit() {
        Employee employee = employee(10L, null);
        Employee manager = employee(20L, null);
        AppUser actor = mock(AppUser.class);
        stubEmployeesAndActor(employee, manager, actor);
        when(managerRelationRepository
                .findByEmployee_EmployeeIdAndRelationTypeAndRelationStatusAndEndedAtIsNull(
                        10L, RelationType.DIRECT, RelationStatus.ACTIVE))
                .thenReturn(List.of());
        when(managerRelationRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.changeDirectManager(10L, new ChangeDirectManagerRequest(20L));

        assertEquals(10L, response.employeeId());
        assertEquals(20L, response.managerEmployeeId());
        ArgumentCaptor<ManagerRelation> relation = ArgumentCaptor.forClass(ManagerRelation.class);
        verify(managerRelationRepository).saveAndFlush(relation.capture());
        assertEquals(employee, relation.getValue().getEmployee());
        assertEquals(manager, relation.getValue().getManagerEmployee());
        assertEquals(actor, relation.getValue().getCreatedBy());
        assertEquals(RelationType.DIRECT, relation.getValue().getRelationType());
        assertEquals(RelationStatus.ACTIVE, relation.getValue().getRelationStatus());
        assertEquals(NOW, relation.getValue().getStartedAt());
        assertNull(relation.getValue().getEndedAt());
        ArgumentCaptor<AuditLogRecordCommand> audit = ArgumentCaptor.forClass(AuditLogRecordCommand.class);
        verify(auditLogRecordService).record(audit.capture());
        assertNull(audit.getValue().previousValue().get("managerEmployeeId"));
        assertEquals(20L, audit.getValue().changedValue().get("managerEmployeeId"));
        assertEquals(10L, audit.getValue().targetId());
    }

    @Test
    void endsOldRelationAndCreatesNewRelationWithoutOverwritingHistory() {
        Employee employee = employee(10L, null);
        Employee oldManager = employee(15L, null);
        Employee newManager = employee(20L, null);
        AppUser actor = mock(AppUser.class);
        ManagerRelation current = ManagerRelation.createDirect(
                employee, oldManager, actor, NOW.minusDays(10));
        stubEmployeesAndActor(employee, newManager, actor);
        when(managerRelationRepository
                .findByEmployee_EmployeeIdAndRelationTypeAndRelationStatusAndEndedAtIsNull(
                        10L, RelationType.DIRECT, RelationStatus.ACTIVE))
                .thenReturn(List.of(current));
        when(managerRelationRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.changeDirectManager(10L, new ChangeDirectManagerRequest(20L));

        assertEquals(RelationStatus.ENDED, current.getRelationStatus());
        assertEquals(NOW, current.getEndedAt());
        assertEquals(oldManager, current.getManagerEmployee());
        ArgumentCaptor<AuditLogRecordCommand> audit = ArgumentCaptor.forClass(AuditLogRecordCommand.class);
        verify(auditLogRecordService).record(audit.capture());
        assertEquals(15L, audit.getValue().previousValue().get("managerEmployeeId"));
        assertEquals(20L, audit.getValue().changedValue().get("managerEmployeeId"));
    }

    @Test
    void sameManagerIsNoOp() {
        Employee employee = employee(10L, null);
        Employee manager = employee(20L, null);
        when(employeeRepository.findForUpdateByEmployeeId(10L)).thenReturn(Optional.of(employee));
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(manager));
        ManagerRelation current = mock(ManagerRelation.class);
        when(current.getManagerEmployee()).thenReturn(manager);
        when(managerRelationRepository
                .findByEmployee_EmployeeIdAndRelationTypeAndRelationStatusAndEndedAtIsNull(
                        10L, RelationType.DIRECT, RelationStatus.ACTIVE))
                .thenReturn(List.of(current));

        service.changeDirectManager(10L, new ChangeDirectManagerRequest(20L));

        verify(managerRelationRepository, never()).saveAndFlush(any());
        verifyNoInteractions(currentUserProvider, appUserRepository, auditLogRecordService);
    }

    @Test
    void rejectsMissingOrDeletedEmployeesSelfAssignmentAndDuplicateActiveData() {
        assertCode(OrganizationErrorCode.SELF_MANAGER_NOT_ALLOWED,
                () -> service.changeDirectManager(10L, new ChangeDirectManagerRequest(10L)));

        when(employeeRepository.findForUpdateByEmployeeId(10L)).thenReturn(Optional.empty());
        assertCode(OrganizationErrorCode.EMPLOYEE_NOT_FOUND,
                () -> service.changeDirectManager(10L, new ChangeDirectManagerRequest(20L)));

        Employee deleted = employee(10L, NOW);
        when(employeeRepository.findForUpdateByEmployeeId(10L)).thenReturn(Optional.of(deleted));
        assertCode(OrganizationErrorCode.EMPLOYEE_NOT_FOUND,
                () -> service.changeDirectManager(10L, new ChangeDirectManagerRequest(20L)));

        Employee employee = employee(10L, null);
        when(employeeRepository.findForUpdateByEmployeeId(10L)).thenReturn(Optional.of(employee));
        when(employeeRepository.findById(20L)).thenReturn(Optional.empty());
        assertCode(OrganizationErrorCode.MANAGER_NOT_FOUND,
                () -> service.changeDirectManager(10L, new ChangeDirectManagerRequest(20L)));

        Employee managerDeleted = employee(20L, NOW);
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(managerDeleted));
        assertCode(OrganizationErrorCode.MANAGER_NOT_FOUND,
                () -> service.changeDirectManager(10L, new ChangeDirectManagerRequest(20L)));

        Employee manager = employee(20L, null);
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(manager));
        when(managerRelationRepository
                .findByEmployee_EmployeeIdAndRelationTypeAndRelationStatusAndEndedAtIsNull(
                        10L, RelationType.DIRECT, RelationStatus.ACTIVE))
                .thenReturn(List.of(mock(ManagerRelation.class), mock(ManagerRelation.class)));
        assertCode(OrganizationErrorCode.MANAGER_RELATION_DATA_CONFLICT,
                () -> service.changeDirectManager(10L, new ChangeDirectManagerRequest(20L)));
    }

    private void stubEmployeesAndActor(Employee employee, Employee manager, AppUser actor) {
        when(employeeRepository.findForUpdateByEmployeeId(10L)).thenReturn(Optional.of(employee));
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(manager));
        when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserContext(
                100L, 1L, java.util.Set.of(RoleType.HR_MANAGER), null, null, null));
        when(appUserRepository.findById(100L)).thenReturn(Optional.of(actor));
    }

    private Employee employee(Long id, LocalDateTime deletedAt) {
        Employee employee = mock(Employee.class);
        lenient().when(employee.getEmployeeId()).thenReturn(id);
        lenient().when(employee.getDeletedAt()).thenReturn(deletedAt);
        return employee;
    }

    private void assertCode(OrganizationErrorCode code, org.junit.jupiter.api.function.Executable action) {
        BusinessException exception = assertThrows(BusinessException.class, action);
        assertEquals(code, exception.getErrorCode());
    }
}
