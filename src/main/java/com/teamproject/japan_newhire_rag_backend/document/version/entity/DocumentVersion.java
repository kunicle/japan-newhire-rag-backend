package com.teamproject.japan_newhire_rag_backend.document.version.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;

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
@Table(name = "document_version")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_version_id")
    private Long documentVersionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "version_name", nullable = false, length = 20)
    private String versionName;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "stored_file_path", nullable = false, length = 500)
    private String storedFilePath;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "publication_status", nullable = false, length = 20)
    private String publicationStatus;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "published_by")
    private Long publishedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    private DocumentVersion(
            Document document,
            String versionName,
            LocalDate effectiveDate,
            LocalDate expirationDate,
            String originalFileName,
            String storedFilePath,
            long fileSize) {
        this.document = document;
        this.versionName = versionName;
        this.effectiveDate = effectiveDate;
        this.expirationDate = expirationDate;
        this.originalFileName = originalFileName;
        this.storedFilePath = storedFilePath;
        this.fileSize = fileSize;
        this.mimeType = "text/plain";
        this.publicationStatus = "DRAFT";
        this.isActive = false;
        this.publishedBy = null;
        this.publishedAt = null;
    }

    public static DocumentVersion create(
            Document document,
            String versionName,
            LocalDate effectiveDate,
            LocalDate expirationDate,
            String originalFileName,
            String storedFilePath,
            long fileSize) {
        return new DocumentVersion(
                document,
                versionName,
                effectiveDate,
                expirationDate,
                originalFileName,
                storedFilePath,
                fileSize);
    }
}
