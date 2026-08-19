package com.teamproject.japan_newhire_rag_backend.document.access.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule;

public interface DocumentAccessRuleRepository extends JpaRepository<DocumentAccessRule, Long> {

    Optional<DocumentAccessRule> findByDocumentVersion_DocumentVersionId(Long documentVersionId);

    List<DocumentAccessRule> findByDocumentVersion_DocumentVersionIdIn(
            Collection<Long> documentVersionIds);
}
