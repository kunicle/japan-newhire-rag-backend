package com.teamproject.japan_newhire_rag_backend.document.access.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessDepartment;

public interface DocumentAccessDepartmentRepository extends JpaRepository<DocumentAccessDepartment, Long> {

    List<DocumentAccessDepartment> findByDocumentAccessRule_DocumentAccessRuleId(Long documentAccessRuleId);

    List<DocumentAccessDepartment> findByDocumentAccessRule_DocumentAccessRuleIdIn(
            Collection<Long> documentAccessRuleIds);
}
