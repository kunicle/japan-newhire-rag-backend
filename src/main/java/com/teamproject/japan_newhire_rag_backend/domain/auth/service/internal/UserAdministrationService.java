package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.AccountStatusResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.CreateUserRequest;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.CreateUserResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.auth.error.UserAdministrationErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.DepartmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.DepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.JobGradeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.internal.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.internal.AuditLogRecordService;

@Service
@Transactional
public class UserAdministrationService {

    private final AppUserRepository appUserRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final JobGradeRepository jobGradeRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogRecordService auditLogRecordService;

    public UserAdministrationService(
            AppUserRepository appUserRepository,
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            JobGradeRepository jobGradeRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserProvider currentUserProvider,
            AuditLogRecordService auditLogRecordService
    ) {
        this.appUserRepository = appUserRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.jobGradeRepository = jobGradeRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
        this.auditLogRecordService = auditLogRecordService;
    }

    public CreateUserResponse createUser(CreateUserRequest request) {
        Long actorAppUserId = currentUserProvider.getCurrentUser().appUserId();
        if (appUserRepository.existsByEmail(request.email())) {
            throw new BusinessException(UserAdministrationErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (employeeRepository.existsByEmployeeNumber(request.employeeNumber())) {
            throw new BusinessException(
                    UserAdministrationErrorCode.EMPLOYEE_NUMBER_ALREADY_EXISTS);
        }

        Department department = departmentRepository.findById(request.departmentId())
                .filter(value -> value.getDeletedAt() == null)
                .filter(value -> value.getDepartmentStatus() == DepartmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        UserAdministrationErrorCode.DEPARTMENT_NOT_AVAILABLE));
        JobGrade jobGrade = jobGradeRepository.findById(request.jobGradeId())
                .filter(JobGrade::isActive)
                .orElseThrow(() -> new BusinessException(
                        UserAdministrationErrorCode.JOB_GRADE_NOT_AVAILABLE));

        try {
            AppUser appUser = appUserRepository.save(AppUser.createActive(
                    request.email(), passwordEncoder.encode(request.password())));
            Employee employee = employeeRepository.save(Employee.createEmployed(
                    appUser,
                    department,
                    jobGrade,
                    request.employeeNumber(),
                    request.employeeName(),
                    request.employeeType(),
                    request.hireDate()));
            employeeRepository.flush();

            auditLogRecordService.record(new AuditLogRecordCommand(
                    actorAppUserId,
                    AuditActionType.USER_CREATED,
                    appUser.getAppUserId(),
                    null,
                    Map.of(
                            "accountStatus", AccountStatus.ACTIVE,
                            "employeeId", employee.getEmployeeId()),
                    null,
                    null));

            return new CreateUserResponse(
                    appUser.getAppUserId(),
                    employee.getEmployeeId(),
                    appUser.getAccountStatus(),
                    employee.getEmploymentStatus());
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(UserAdministrationErrorCode.USER_DATA_CONFLICT);
        }
    }

    public AccountStatusResponse activate(Long appUserId) {
        return changeStatus(appUserId, AccountStatus.INACTIVE, true);
    }

    public AccountStatusResponse deactivate(Long appUserId) {
        return changeStatus(appUserId, AccountStatus.ACTIVE, false);
    }

    private AccountStatusResponse changeStatus(
            Long appUserId,
            AccountStatus requiredCurrentStatus,
            boolean activate
    ) {
        Long actorAppUserId = currentUserProvider.getCurrentUser().appUserId();
        AppUser appUser = appUserRepository.findById(appUserId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(
                        UserAdministrationErrorCode.APP_USER_NOT_FOUND));

        if (appUser.getAccountStatus() != requiredCurrentStatus) {
            throw new BusinessException(
                    UserAdministrationErrorCode.INVALID_ACCOUNT_STATUS_TRANSITION);
        }

        AccountStatus changedStatus;
        AuditActionType actionType;
        if (activate) {
            appUser.activate();
            changedStatus = AccountStatus.ACTIVE;
            actionType = AuditActionType.ACCOUNT_ACTIVATED;
        } else {
            appUser.deactivate();
            changedStatus = AccountStatus.INACTIVE;
            actionType = AuditActionType.ACCOUNT_DEACTIVATED;
        }

        auditLogRecordService.record(new AuditLogRecordCommand(
                actorAppUserId,
                actionType,
                appUserId,
                Map.of("accountStatus", requiredCurrentStatus),
                Map.of("accountStatus", changedStatus),
                null,
                null));
        return new AccountStatusResponse(appUserId, changedStatus);
    }
}
