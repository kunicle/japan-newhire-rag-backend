package com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.ChangeDirectManagerRequest;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.DirectManagerResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.ManagerRelation;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.error.OrganizationErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.ManagerRelationRepository;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordService;

@Service
@Transactional
public class DirectManagerCommandService {

    private final EmployeeRepository employeeRepository;
    private final ManagerRelationRepository managerRelationRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogRecordService auditLogRecordService;
    private final Clock clock;

    public DirectManagerCommandService(
            EmployeeRepository employeeRepository,
            ManagerRelationRepository managerRelationRepository,
            AppUserRepository appUserRepository,
            CurrentUserProvider currentUserProvider,
            AuditLogRecordService auditLogRecordService,
            Clock clock
    ) {
        this.employeeRepository = employeeRepository;
        this.managerRelationRepository = managerRelationRepository;
        this.appUserRepository = appUserRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditLogRecordService = auditLogRecordService;
        this.clock = clock;
    }

    public DirectManagerResponse changeDirectManager(
            Long employeeId,
            ChangeDirectManagerRequest request
    ) {
        return changeManager(employeeId, request.managerEmployeeId());
    }

    /** Null ends the current relationship while retaining its history. */
    public DirectManagerResponse changeManager(Long employeeId, Long managerEmployeeId) {
        if (employeeId.equals(managerEmployeeId)) {
            throw new BusinessException(OrganizationErrorCode.SELF_MANAGER_NOT_ALLOWED);
        }

        employeeRepository.lockOrganizationEmployees();
        Employee employee = employeeRepository.findForUpdateByEmployeeId(employeeId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(
                        OrganizationErrorCode.EMPLOYEE_NOT_FOUND));
        Employee manager = managerEmployeeId == null ? null : employeeRepository.findById(managerEmployeeId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(
                        OrganizationErrorCode.MANAGER_NOT_FOUND));

        List<ManagerRelation> activeRelations = managerRelationRepository
                .findByEmployee_EmployeeIdAndRelationTypeAndRelationStatusAndEndedAtIsNull(
                        employeeId,
                        RelationType.DIRECT,
                        RelationStatus.ACTIVE);
        if (activeRelations.size() > 1) {
            throw new BusinessException(
                    OrganizationErrorCode.MANAGER_RELATION_DATA_CONFLICT);
        }

        ManagerRelation current = activeRelations.isEmpty() ? null : activeRelations.get(0);
        if (manager != null && manager.getJobGrade().getGradeLevel() > employee.getJobGrade().getGradeLevel()) {
            throw new BusinessException(OrganizationErrorCode.MANAGER_GRADE_NOT_ALLOWED);
        }
        validateNoCycle(employeeId, managerEmployeeId);
        if (current != null
                && current.getManagerEmployee().getEmployeeId().equals(managerEmployeeId)) {
            return new DirectManagerResponse(employeeId, managerEmployeeId);
        }

        if (current == null && managerEmployeeId == null) {
            return new DirectManagerResponse(employeeId, null);
        }
        Long actorAppUserId = currentUserProvider.getCurrentUser().appUserId();
        AppUser actor = appUserRepository.findById(actorAppUserId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(
                        OrganizationErrorCode.ORGANIZATION_DATA_CONFLICT));
        LocalDateTime changedAt = LocalDateTime.now(clock);
        Long previousManagerEmployeeId = current == null
                ? null
                : current.getManagerEmployee().getEmployeeId();

        try {
            if (current != null) {
                current.end(changedAt);
            }
            managerRelationRepository.flush();
            if (manager != null) {
                managerRelationRepository.saveAndFlush(
                        ManagerRelation.createDirect(employee, manager, actor, changedAt));
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(
                    OrganizationErrorCode.MANAGER_RELATION_DATA_CONFLICT);
        }

        auditLogRecordService.record(new AuditLogRecordCommand(
                actorAppUserId,
                AuditActionType.DIRECT_MANAGER_CHANGED,
                employeeId,
                Collections.singletonMap("managerEmployeeId", previousManagerEmployeeId),
                Collections.singletonMap("managerEmployeeId", managerEmployeeId),
                null,
                null));
        return new DirectManagerResponse(employeeId, managerEmployeeId);
    }

    private void validateNoCycle(Long employeeId, Long managerEmployeeId) {
        java.util.Set<Long> visited = new java.util.HashSet<>();
        Long cursor = managerEmployeeId;
        while (cursor != null) {
            if (cursor.equals(employeeId) || !visited.add(cursor)) {
                throw new BusinessException(OrganizationErrorCode.MANAGER_CYCLE_NOT_ALLOWED);
            }
            List<ManagerRelation> relations = managerRelationRepository
                    .findByEmployee_EmployeeIdAndRelationTypeAndRelationStatusAndEndedAtIsNull(
                            cursor, RelationType.DIRECT, RelationStatus.ACTIVE);
            if (relations.size() > 1) {
                throw new BusinessException(OrganizationErrorCode.MANAGER_RELATION_DATA_CONFLICT);
            }
            cursor = relations.isEmpty() ? null : relations.get(0).getManagerEmployee().getEmployeeId();
        }
    }
}
