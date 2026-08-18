package com.teamproject.japan_newhire_rag_backend.document.access.entity;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;
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
            AccessScope accessScope,
            ConditionOperator conditionOperator,
            Long minimumJobGradeId,
            boolean newEmployeeOnly,
            Long createdBy) {
        validateRequiredFields(documentVersion, accessScope, conditionOperator, createdBy);
        this.documentVersion = documentVersion;
        this.createdBy = createdBy;
        applyConfiguration(accessScope, conditionOperator, minimumJobGradeId, newEmployeeOnly);
    }

    public static DocumentAccessRule create(
            DocumentVersion documentVersion,
            AccessScope accessScope,
            ConditionOperator conditionOperator,
            Long minimumJobGradeId,
            boolean newEmployeeOnly,
            Long createdBy) {
        return new DocumentAccessRule(
                documentVersion,
                accessScope,
                conditionOperator,
                minimumJobGradeId,
                newEmployeeOnly,
                createdBy);
    }

    public void reconfigure(
            AccessScope accessScope,
            ConditionOperator conditionOperator,
            Long minimumJobGradeId,
            boolean newEmployeeOnly) {
        if (accessScope == null) {
            throw new IllegalArgumentException("accessScope는 null일 수 없습니다.");
        }
        if (accessScope == AccessScope.RESTRICTED && conditionOperator == null) {
            throw new IllegalArgumentException("RESTRICTED 범위에는 conditionOperator가 필요합니다.");
        }
        applyConfiguration(accessScope, conditionOperator, minimumJobGradeId, newEmployeeOnly);
    }

    private static void validateRequiredFields(
            DocumentVersion documentVersion,
            AccessScope accessScope,
            ConditionOperator conditionOperator,
            Long createdBy) {
        if (documentVersion == null) {
            throw new IllegalArgumentException("documentVersion은 null일 수 없습니다.");
        }
        if (accessScope == null) {
            throw new IllegalArgumentException("accessScope는 null일 수 없습니다.");
        }
        if (accessScope == AccessScope.RESTRICTED && conditionOperator == null) {
            throw new IllegalArgumentException("RESTRICTED 범위에는 conditionOperator가 필요합니다.");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("createdBy는 null일 수 없습니다.");
        }
    }

    private void applyConfiguration(
            AccessScope accessScope,
            ConditionOperator conditionOperator,
            Long minimumJobGradeId,
            boolean newEmployeeOnly) {
        if (accessScope == AccessScope.ALL
                && (minimumJobGradeId != null || newEmployeeOnly)) {
            throw new IllegalArgumentException("ALL 범위에는 접근 조건을 설정할 수 없습니다.");
        }

        this.accessScope = accessScope.name();
        this.conditionOperator = accessScope == AccessScope.ALL
                ? ConditionOperator.OR.name()
                : conditionOperator.name();
        this.minimumJobGradeId = minimumJobGradeId;
        this.isNewEmployeeOnly = newEmployeeOnly;
        this.isActive = true;
    }
}
