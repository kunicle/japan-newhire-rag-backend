package com.teamproject.japan_newhire_rag_backend.document.access.adapter;

import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.document.access.UserAccessContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.AccessReferenceQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

public class CurrentUserAccessContextAssembler {

    private final AccessReferenceQueryService accessReferenceQueryService;

    public CurrentUserAccessContextAssembler(AccessReferenceQueryService accessReferenceQueryService) {
        this.accessReferenceQueryService = accessReferenceQueryService;
    }

    public UserAccessContext assemble(CurrentUserContext context) {
        if (context == null) {
            throw new IllegalArgumentException("CurrentUserContext는 null일 수 없습니다.");
        }

        Set<Long> roleIds = accessReferenceQueryService.findRoleIdsByRoleTypes(context.roles());
        Long departmentId = context.departmentId();
        Integer jobGradeLevel = context.jobGradeLevel();
        if (jobGradeLevel == null) {
            throw new IllegalStateException("CurrentUserContext에 jobGradeLevel이 없습니다.");
        }
        boolean isNewEmployee = context.employeeType() == EmployeeType.NEW_HIRE;

        return new UserAccessContext(roleIds, departmentId, jobGradeLevel, isNewEmployee);
    }
}
