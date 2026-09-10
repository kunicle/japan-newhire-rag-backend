package com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.*;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.*;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.DepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.error.OrganizationErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.*;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import org.mockito.ArgumentCaptor;

class DepartmentCommandServiceTest {
    DepartmentRepository repository = mock(DepartmentRepository.class);
    CurrentUserProvider user = mock(CurrentUserProvider.class);
    AuditLogRecordService audit = mock(AuditLogRecordService.class);
    DepartmentCommandService service = new DepartmentCommandService(repository, user, audit);
    Department department(Long id, Department parent) {
        Department department = Department.create("D" + id, "Department", parent);
        ReflectionTestUtils.setField(department, "departmentId", id);
        when(repository.findById(id)).thenReturn(Optional.of(department));
        return department;
    }
    void actor() { when(user.getCurrentUser()).thenReturn(new CurrentUserContext(1L, 1L, Set.of(RoleType.HR_MANAGER), null, null, null)); }

    @Test void createsDepartmentAndAudits() {
        actor();
        department(1L, null);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            Department value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "departmentId", 2L);
            return value;
        });
        var result = service.create(new CreateDepartmentRequest("NEW", "New department", 1L));
        assertEquals(2L, result.departmentId());
        assertEquals(1L, result.parentDepartmentId());
        var captor = ArgumentCaptor.forClass(AuditLogRecordCommand.class);
        verify(audit).record(captor.capture());
        assertEquals(AuditActionType.DEPARTMENT_CREATED, captor.getValue().actionType());
    }
    @Test void updatesNameAndRemovesParent() {
        actor();
        Department parent = department(1L, null), child = department(2L, parent);
        var response = service.update(2L, new UpdateDepartmentRequest("Updated", null));
        assertEquals("Updated", child.getDepartmentName());
        assertNull(response.parentDepartmentId());
        verify(audit).record(any());
    }
    @Test void rejectsSelfAndDescendantParent() {
        Department parent = department(1L, null);
        department(2L, parent);
        for (Long id : List.of(1L, 2L)) assertEquals(OrganizationErrorCode.DEPARTMENT_CYCLE_NOT_ALLOWED,
                assertThrows(BusinessException.class, () -> service.update(1L, new UpdateDepartmentRequest("X", id))).getErrorCode());
        verifyNoInteractions(audit);
    }
    @Test void rejectsDuplicateCodeAndMissingParent() {
        Department existing = department(1L, null);
        when(repository.findByDepartmentCode("DUP")).thenReturn(Optional.of(existing));
        assertEquals(OrganizationErrorCode.DEPARTMENT_CODE_CONFLICT, assertThrows(BusinessException.class,
                () -> service.create(new CreateDepartmentRequest("DUP", "Name", null))).getErrorCode());
        assertEquals(OrganizationErrorCode.DEPARTMENT_NOT_FOUND, assertThrows(BusinessException.class,
                () -> service.create(new CreateDepartmentRequest("NEW", "Name", 999L))).getErrorCode());
    }
}
