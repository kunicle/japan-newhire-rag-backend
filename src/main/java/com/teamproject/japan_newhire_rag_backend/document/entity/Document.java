package com.teamproject.japan_newhire_rag_backend.document.entity;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.document.category.entity.DocumentCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "document")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_category_id", nullable = false)
    private DocumentCategory documentCategory;

    @Column(name = "document_name", nullable = false, length = 200)
    private String documentName;

    @Column(name = "document_description", length = 1000)
    private String documentDescription;

    @Column(name = "document_status", nullable = false, length = 20)
    private String documentStatus;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Document(
            DocumentCategory documentCategory,
            String documentName,
            String documentDescription,
            Long createdBy) {
        this.documentCategory = documentCategory;
        this.documentName = documentName;
        this.documentDescription = documentDescription;
        this.documentStatus = "ACTIVE";
        this.createdBy = createdBy;
        this.deletedAt = null;
    }

    public static Document create(
            DocumentCategory documentCategory,
            String documentName,
            String documentDescription,
            Long createdBy) {
        return new Document(documentCategory, documentName, documentDescription, createdBy);
    }
}
