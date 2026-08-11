package com.teamproject.japan_newhire_rag_backend.document.access.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessDepartment;

public interface DocumentAccessDepartmentRepository extends JpaRepository<DocumentAccessDepartment, Long> {
}
