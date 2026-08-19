package com.teamproject.japan_newhire_rag_backend.document.category.entity;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "document_category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_category_id")
    private Long documentCategoryId;

    @Column(name = "category_code", nullable = false, unique = true, length = 30)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, unique = true, length = 100)
    private String categoryName;

    @Column(name = "category_description", length = 500)
    private String categoryDescription;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    private DocumentCategory(String categoryCode, String categoryName, String categoryDescription) {
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.categoryDescription = categoryDescription;
        this.isActive = true;
    }

    public static DocumentCategory create(
            String categoryCode,
            String categoryName,
            String categoryDescription) {
        return new DocumentCategory(categoryCode, categoryName, categoryDescription);
    }
}
