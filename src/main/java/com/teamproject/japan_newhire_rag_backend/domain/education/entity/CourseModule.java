package com.teamproject.japan_newhire_rag_backend.domain.education.entity;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course_module", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_module_order", columnNames = { "course_id", "module_order" })
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseModule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_module_id")
    private Long courseModuleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "module_title", nullable = false, length = 200)
    private String moduleTitle;

    @Column(name = "module_content", columnDefinition = "TEXT")
    private String moduleContent;

    @Column(name = "reference_url", length = 500)
    private String referenceUrl;

    @Column(name = "module_order", nullable = false)
    private int moduleOrder;

    @Column(name = "is_required", nullable = false)
    private boolean required = true;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public static CourseModule create(
            Course course,
            String moduleTitle,
            String moduleContent,
            String referenceUrl,
            int moduleOrder,
            boolean required
    ) {
        CourseModule module = new CourseModule();
        module.course = course;
        module.moduleTitle = moduleTitle;
        module.moduleContent = moduleContent;
        module.referenceUrl = referenceUrl;
        module.moduleOrder = moduleOrder;
        module.required = required;
        module.active = true;
        return module;
    }

    public void updateBasicInformation(
            String moduleTitle,
            String moduleContent,
            String referenceUrl,
            int moduleOrder,
            boolean required
    ) {
        this.moduleTitle = moduleTitle;
        this.moduleContent = moduleContent;
        this.referenceUrl = referenceUrl;
        this.moduleOrder = moduleOrder;
        this.required = required;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }
}
