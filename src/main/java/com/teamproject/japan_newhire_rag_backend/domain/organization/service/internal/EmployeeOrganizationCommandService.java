package com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal;

import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationType;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.ChangeEmployeeOrganizationRequest;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.DepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.JobGradeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.ManagerRelationRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.error.OrganizationErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordService;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;

@Service
@Transactional
public class EmployeeOrganizationCommandService {
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final JobGradeRepository grades;
    private final DirectManagerCommandService managers;
    private final CurrentUserProvider currentUser;
    private final AuditLogRecordService audit;
    private final ManagerRelationRepository relations;

    public EmployeeOrganizationCommandService(EmployeeRepository employees, DepartmentRepository departments,
            JobGradeRepository grades, DirectManagerCommandService managers,
            CurrentUserProvider currentUser, AuditLogRecordService audit, ManagerRelationRepository relations) {
        this.employees = employees;
        this.departments = departments;
        this.grades = grades;
        this.managers = managers;
        this.currentUser = currentUser;
        this.audit = audit;
        this.relations = relations;
    }

    public void changeOrganization(Long employeeId, ChangeEmployeeOrganizationRequest request) {
        employees.lockOrganizationEmployees();
        Employee employee = employees.findForUpdateByEmployeeId(employeeId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.EMPLOYEE_NOT_FOUND));
        Department department = departments.findById(request.departmentId())
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.DEPARTMENT_NOT_FOUND));
        JobGrade grade = grades.findById(request.jobGradeId()).filter(JobGrade::isActive)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.JOB_GRADE_NOT_FOUND));
        Long previousDepartment = employee.getDepartment().getDepartmentId();
        Long previousGrade = employee.getJobGrade().getJobGradeId();
        if (!Objects.equals(previousGrade, grade.getJobGradeId())) {
            boolean lowerThanReport = relations
                    .findByManagerEmployee_EmployeeIdAndRelationStatusAndEndedAtIsNull(employeeId,
                            RelationStatus.ACTIVE)
                    .stream().filter(relation -> relation.getRelationType()
                            == RelationType.DIRECT)
                    .filter(relation -> relation.getEmployee().getDeletedAt() == null)
                    .anyMatch(relation -> grade.getGradeLevel() > relation.getEmployee().getJobGrade().getGradeLevel());
            if (lowerThanReport) throw new BusinessException(OrganizationErrorCode.MANAGER_GRADE_NOT_ALLOWED);
        }
        // The manager validator must see the newly selected grade; all edits share this transaction.
        employee.changeOrganization(department, grade);
        managers.changeManager(employeeId, request.managerEmployeeId());
        recordChange(employeeId, AuditActionType.EMPLOYEE_DEPARTMENT_CHANGED, "departmentId",
                previousDepartment, department.getDepartmentId());
        recordChange(employeeId, AuditActionType.EMPLOYEE_JOB_GRADE_CHANGED, "jobGradeId",
                previousGrade, grade.getJobGradeId());
    }

    private void recordChange(Long id, AuditActionType action, String key, Long before, Long after) {
        if (!Objects.equals(before, after)) {
            audit.record(new AuditLogRecordCommand(currentUser.getCurrentUser().appUserId(), action,
                    id, Map.of(key, before), Map.of(key, after), null, null));
        }
    }
}
