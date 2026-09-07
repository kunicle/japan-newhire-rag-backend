package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.MyProfileResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.error.ProfileErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.ManagerRelation;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.ManagerRelationRepository;

@Service
@Transactional(readOnly = true)
public class MyProfileQueryService {

    private final EmployeeRepository employeeRepository;
    private final ManagerRelationRepository managerRelationRepository;

    public MyProfileQueryService(
            EmployeeRepository employeeRepository,
            ManagerRelationRepository managerRelationRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.managerRelationRepository = managerRelationRepository;
    }

    public MyProfileResponse getMyProfile(CurrentUserContext currentUser) {
        Employee employee = employeeRepository.findByAppUser_AppUserId(currentUser.appUserId())
                .filter(found -> found.getEmployeeId().equals(currentUser.employeeId()))
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ProfileErrorCode.PROFILE_NOT_FOUND));

        List<ManagerRelation> directManagers = managerRelationRepository
                .findByEmployee_EmployeeIdAndRelationTypeAndRelationStatusAndEndedAtIsNull(
                        employee.getEmployeeId(),
                        RelationType.DIRECT,
                        RelationStatus.ACTIVE);

        if (directManagers.size() > 1) {
            throw new BusinessException(ProfileErrorCode.PROFILE_DATA_CONFLICT);
        }

        Employee manager = directManagers.isEmpty()
                ? null
                : directManagers.get(0).getManagerEmployee();
        if (manager != null && manager.getDeletedAt() != null) {
            manager = null;
        }

        return new MyProfileResponse(
                currentUser.appUserId(),
                employee.getEmployeeId(),
                employee.getEmployeeNumber(),
                employee.getEmployeeName(),
                employee.getAppUser().getEmail(),
                employee.getDepartment().getDepartmentId(),
                employee.getDepartment().getDepartmentName(),
                employee.getJobGrade().getJobGradeId(),
                employee.getJobGrade().getGradeName(),
                employee.getJobGrade().getGradeLevel(),
                currentUser.roles(),
                employee.getHireDate(),
                manager == null ? null : manager.getEmployeeId(),
                manager == null ? null : manager.getEmployeeName());
    }
}
