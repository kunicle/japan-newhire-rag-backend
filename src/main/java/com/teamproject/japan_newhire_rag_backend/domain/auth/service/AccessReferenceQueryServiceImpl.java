package com.teamproject.japan_newhire_rag_backend.domain.auth.service;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.AccessReferenceQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.Role;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.RoleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.JobGradeRepository;

@Service
@Transactional(readOnly = true)
public class AccessReferenceQueryServiceImpl implements AccessReferenceQueryService {

    private final RoleRepository roleRepository;
    private final JobGradeRepository jobGradeRepository;

    public AccessReferenceQueryServiceImpl(
            RoleRepository roleRepository,
            JobGradeRepository jobGradeRepository
    ) {
        this.roleRepository = roleRepository;
        this.jobGradeRepository = jobGradeRepository;
    }

    @Override
    public Set<Long> findRoleIdsByRoleTypes(Collection<RoleType> roleTypes) {
        Set<RoleType> normalizedRoleTypes = normalizeRoleTypes(roleTypes);
        if (normalizedRoleTypes.isEmpty()) {
            return Set.of();
        }

        Set<String> requestedRoleCodes = normalizedRoleTypes.stream()
                .map(RoleType::name)
                .collect(Collectors.toUnmodifiableSet());
        List<Role> roles = roleRepository.findByRoleCodeIn(requestedRoleCodes);
        Set<String> foundRoleCodes = roles.stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toUnmodifiableSet());

        Set<String> missingRoleCodes = requestedRoleCodes.stream()
                .filter(roleCode -> !foundRoleCodes.contains(roleCode))
                .collect(Collectors.toUnmodifiableSet());
        if (!missingRoleCodes.isEmpty()) {
            throw new IllegalStateException(
                    "Role definitions not found for roleCodes: " + missingRoleCodes);
        }

        return roles.stream()
                .map(Role::getRoleId)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Integer findJobGradeLevel(Long jobGradeId) {
        if (jobGradeId == null) {
            return null;
        }

        return jobGradeRepository.findById(jobGradeId)
                .map(JobGrade::getGradeLevel)
                .orElseThrow(() -> new IllegalStateException(
                        "JobGrade not found for jobGradeId: " + jobGradeId));
    }

    private Set<RoleType> normalizeRoleTypes(Collection<RoleType> roleTypes) {
        if (roleTypes == null || roleTypes.isEmpty()) {
            return Set.of();
        }

        EnumSet<RoleType> normalized = EnumSet.noneOf(RoleType.class);
        roleTypes.stream()
                .filter(roleType -> roleType != null)
                .forEach(normalized::add);
        return Set.copyOf(normalized);
    }
}
