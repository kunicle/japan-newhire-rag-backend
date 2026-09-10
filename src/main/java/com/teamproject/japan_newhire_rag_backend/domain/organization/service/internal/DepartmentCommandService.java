package com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.CreateDepartmentRequest;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.UpdateDepartmentRequest;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.OrganizationDepartmentResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.DepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.error.OrganizationErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordService;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;

@Service
@Transactional
public class DepartmentCommandService {
    private final DepartmentRepository departments;
    private final CurrentUserProvider currentUser;
    private final AuditLogRecordService audit;

    public DepartmentCommandService(DepartmentRepository departments, CurrentUserProvider currentUser,
            AuditLogRecordService audit) {
        this.departments = departments;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    public OrganizationDepartmentResponse create(CreateDepartmentRequest request) {
        departments.lockDepartments();
        String code = request.departmentCode().trim();
        if (departments.findByDepartmentCode(code).isPresent()) {
            throw new BusinessException(OrganizationErrorCode.DEPARTMENT_CODE_CONFLICT);
        }
        Department parent = parent(request.parentDepartmentId(), null);
        Department department = Department.create(code, request.departmentName().trim(), parent);
        try {
            departments.saveAndFlush(department);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(OrganizationErrorCode.DEPARTMENT_CODE_CONFLICT);
        }
        record(AuditActionType.DEPARTMENT_CREATED, department, null);
        return response(department);
    }

    public OrganizationDepartmentResponse update(Long id, UpdateDepartmentRequest request) {
        departments.lockDepartments();
        Department department = find(id);
        Department parent = parent(request.parentDepartmentId(), id);
        Map<String, Object> before = values(department);
        department.update(request.departmentName().trim(), parent);
        if (!before.equals(values(department))) record(AuditActionType.DEPARTMENT_UPDATED, department, before);
        return response(department);
    }

    private Department find(Long id) {
        return departments.findById(id).filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.DEPARTMENT_NOT_FOUND));
    }

    private Department parent(Long parentId, Long id) {
        if (parentId == null) return null;
        Department parent = find(parentId);
        Set<Long> visited = new HashSet<>();
        for (Department cursor = parent; cursor != null; cursor = cursor.getParentDepartment()) {
            if (Objects.equals(id, cursor.getDepartmentId()) || !visited.add(cursor.getDepartmentId())) {
                throw new BusinessException(OrganizationErrorCode.DEPARTMENT_CYCLE_NOT_ALLOWED);
            }
        }
        return parent;
    }

    private Map<String, Object> values(Department department) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("departmentCode", department.getDepartmentCode());
        values.put("departmentName", department.getDepartmentName());
        values.put("parentDepartmentId", department.getParentDepartment() == null
                ? null : department.getParentDepartment().getDepartmentId());
        return values;
    }

    private void record(AuditActionType action, Department department, Map<String, Object> before) {
        audit.record(new AuditLogRecordCommand(currentUser.getCurrentUser().appUserId(), action,
                department.getDepartmentId(), before, values(department), null, null));
    }

    private OrganizationDepartmentResponse response(Department department) {
        return new OrganizationDepartmentResponse(department.getDepartmentId(), department.getDepartmentCode(),
                department.getDepartmentName(), department.getParentDepartment() == null
                        ? null : department.getParentDepartment().getDepartmentId(),
                department.getDisplayOrder(), List.of(), List.of());
    }
}
