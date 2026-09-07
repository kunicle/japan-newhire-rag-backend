package com.teamproject.japan_newhire_rag_backend.domain.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.bootstrap")
public record AuthBootstrapProperties(
        boolean enabled,
        String adminEmail,
        String adminPassword,
        String employeeNumber,
        String employeeName,
        Long departmentId,
        Long jobGradeId
) {

    public AuthBootstrapProperties {
        if (enabled) {
            requireText(adminEmail, "admin-email");
            requireText(adminPassword, "admin-password");
            requireText(employeeNumber, "employee-number");
            requireText(employeeName, "employee-name");
            requirePositive(departmentId, "department-id");
            requirePositive(jobGradeId, "job-grade-id");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("auth.bootstrap." + name + " must be set when enabled");
        }
    }

    private static void requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("auth.bootstrap." + name + " must be positive when enabled");
        }
    }

    @Override
    public String toString() {
        return "AuthBootstrapProperties[enabled=" + enabled
                + ", adminEmail=" + adminEmail
                + ", adminPassword=<redacted>"
                + ", employeeNumber=" + employeeNumber
                + ", employeeName=" + employeeName
                + ", departmentId=" + departmentId
                + ", jobGradeId=" + jobGradeId + "]";
    }
}
