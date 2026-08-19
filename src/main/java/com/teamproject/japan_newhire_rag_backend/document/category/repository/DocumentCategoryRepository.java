package com.teamproject.japan_newhire_rag_backend.document.category.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.document.category.entity.DocumentCategory;

public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, Long> {

    Optional<DocumentCategory> findByCategoryCode(String categoryCode);

    List<DocumentCategory> findByIsActiveTrue();
}
