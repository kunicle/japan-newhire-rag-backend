package com.teamproject.japan_newhire_rag_backend.document.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.document.entity.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByDocumentCategory_DocumentCategoryIdAndDocumentStatusAndDeletedAtIsNull(
            Long documentCategoryId,
            String documentStatus);

    List<Document> findAllByOrderByCreatedAtDesc();
}
