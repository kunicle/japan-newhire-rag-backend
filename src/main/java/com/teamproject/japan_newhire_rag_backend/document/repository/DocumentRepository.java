package com.teamproject.japan_newhire_rag_backend.document.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teamproject.japan_newhire_rag_backend.document.entity.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByDocumentCategory_DocumentCategoryIdAndDocumentStatusAndDeletedAtIsNull(
            Long documentCategoryId,
            String documentStatus);

    List<Document> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT d FROM Document d
            WHERE d.documentStatus = :documentStatus
              AND d.deletedAt IS NULL
              AND (:keyword IS NULL OR d.documentName LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<Document> findAllByDocumentStatusAndKeyword(
            @Param("documentStatus") String documentStatus,
            @Param("keyword") String keyword,
            Pageable pageable);
}
