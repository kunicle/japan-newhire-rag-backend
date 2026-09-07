package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.auth.config.AuthBootstrapProperties;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.Role;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.UserRole;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.RoleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.UserRoleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.DepartmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.DepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.JobGradeRepository;

@Service
public class SystemAdminBootstrapService {

    private static final String SYSTEM_ADMIN = RoleType.SYSTEM_ADMIN.name();

    private final AppUserRepository appUserRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final JobGradeRepository jobGradeRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public SystemAdminBootstrapService(
            AppUserRepository appUserRepository,
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            JobGradeRepository jobGradeRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.jobGradeRepository = jobGradeRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void bootstrap(AuthBootstrapProperties properties) {
        if (userRoleRepository.existsByRole_RoleCodeAndRole_IsActiveTrueAndRevokedAtIsNull(
                SYSTEM_ADMIN)) {
            return;
        }
        if (appUserRepository.existsByEmail(properties.adminEmail())) {
            throw new IllegalStateException("Bootstrap admin email is already in use");
        }
        if (employeeRepository.existsByEmployeeNumber(properties.employeeNumber())) {
            throw new IllegalStateException("Bootstrap employee number is already in use");
        }

        Role role = roleRepository.findByRoleCode(SYSTEM_ADMIN)
                .filter(Role::isActive)
                .orElseThrow(() -> new IllegalStateException(
                        "Active SYSTEM_ADMIN role is required for bootstrap"));
        Department department = departmentRepository.findById(properties.departmentId())
                .filter(value -> value.getDeletedAt() == null)
                .filter(value -> value.getDepartmentStatus() == DepartmentStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException(
                        "Active bootstrap department was not found"));
        JobGrade jobGrade = jobGradeRepository.findById(properties.jobGradeId())
                .filter(JobGrade::isActive)
                .orElseThrow(() -> new IllegalStateException(
                        "Active bootstrap job grade was not found"));

        AppUser appUser = appUserRepository.save(AppUser.createActive(
                properties.adminEmail(), passwordEncoder.encode(properties.adminPassword())));
        employeeRepository.save(Employee.createEmployed(
                appUser,
                department,
                jobGrade,
                properties.employeeNumber(),
                properties.employeeName(),
                EmployeeType.GENERAL,
                LocalDate.now()));
        userRoleRepository.save(UserRole.grant(appUser, role, appUser, LocalDateTime.now()));
        userRoleRepository.flush();
    }
}
