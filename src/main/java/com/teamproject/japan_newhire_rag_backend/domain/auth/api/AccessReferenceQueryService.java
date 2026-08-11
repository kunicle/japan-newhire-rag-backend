package com.teamproject.japan_newhire_rag_backend.domain.auth.api;

import java.util.Collection;
import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;

public interface AccessReferenceQueryService {

    Set<Long> findRoleIdsByRoleTypes(Collection<RoleType> roleTypes);

    Integer findJobGradeLevel(Long jobGradeId);
}
