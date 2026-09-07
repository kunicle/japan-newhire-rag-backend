package com.teamproject.japan_newhire_rag_backend.document.access;

import java.util.Set;

// A 실제 Entity 연동 전까지 사용하는 최소 입력 형태
public record UserAccessContext(
        Set<Long> roleIds,
        Long departmentId,
        int jobGradeLevel,
        boolean isNewEmployee) {

    public UserAccessContext {
        if (roleIds == null) {
            throw new IllegalArgumentException("roleIds는 null일 수 없습니다.");
        }
    }
}
