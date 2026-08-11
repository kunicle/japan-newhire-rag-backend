package com.teamproject.japan_newhire_rag_backend.document.access.entity;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "document_access_rule")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentAccessRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_access_rule_id")
    private Long documentAccessRuleId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false, unique = true)
    private DocumentVersion documentVersion;

    @Column(name = "minimum_job_grade_id")
    private Long minimumJobGradeId;

    @Column(name = "access_scope", nullable = false, length = 20)
    private String accessScope;

    @Column(name = "condition_operator", nullable = false, length = 10)
    private String conditionOperator;

    @Column(name = "is_new_employee_only", nullable = false)
    private boolean isNewEmployeeOnly;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    private DocumentAccessRule(
            DocumentVersion documentVersion,
            Long minimumJobGradeId,
            Long createdBy) {
        this.documentVersion = documentVersion;
        this.minimumJobGradeId = minimumJobGradeId;
        this.accessScope = "ALL";
        this.conditionOperator = "OR";
        this.isNewEmployeeOnly = false;
        this.isActive = true;
        this.createdBy = createdBy;
    }

    public static DocumentAccessRule create(
            DocumentVersion documentVersion,
            Long minimumJobGradeId,
            Long createdBy) {
        return new DocumentAccessRule(documentVersion, minimumJobGradeId, createdBy);
    }
}
