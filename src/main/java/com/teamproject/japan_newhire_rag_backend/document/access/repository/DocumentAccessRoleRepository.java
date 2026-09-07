package com.teamproject.japan_newhire_rag_backend.document.access.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRole;

public interface DocumentAccessRoleRepository extends JpaRepository<DocumentAccessRole, Long> {

    List<DocumentAccessRole> findByDocumentAccessRule_DocumentAccessRuleId(Long documentAccessRuleId);

    List<DocumentAccessRole> findByDocumentAccessRule_DocumentAccessRuleIdIn(
            Collection<Long> documentAccessRuleIds);
}
