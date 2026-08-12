package com.teamproject.japan_newhire_rag_backend.domain.auth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    @EntityGraph(attributePaths = "role")
    List<UserRole> findByAppUser_AppUserIdAndRevokedAtIsNull(Long appUserId);
}
