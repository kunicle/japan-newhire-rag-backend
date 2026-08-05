package com.teamproject.japan_newhire_rag_backend.domain.regulation.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "regulation_documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegulationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private RegulationDocument(
            String title,
            String originalFileName,
            DocumentStatus status
    ) {
        this.title = title;
        this.originalFileName = originalFileName;
        this.status = status;
    }

    public static RegulationDocument create(
            String title,
            String originalFileName
    ) {
        return new RegulationDocument(
                title,
                originalFileName,
                DocumentStatus.UPLOADED
        );
    }

    public void changeStatus(DocumentStatus status) {
        this.status = status;
    }
}
