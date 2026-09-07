package com.teamproject.japan_newhire_rag_backend.document.access.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "document_access_department")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentAccessDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_access_department_id")
    private Long documentAccessDepartmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_access_rule_id", nullable = false)
    private DocumentAccessRule documentAccessRule;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private DocumentAccessDepartment(DocumentAccessRule documentAccessRule, Long departmentId) {
        this.documentAccessRule = documentAccessRule;
        this.departmentId = departmentId;
    }

    public static DocumentAccessDepartment create(
            DocumentAccessRule documentAccessRule,
            Long departmentId) {
        return new DocumentAccessDepartment(documentAccessRule, departmentId);
    }
}
