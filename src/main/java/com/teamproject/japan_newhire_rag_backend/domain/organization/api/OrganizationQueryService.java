package com.teamproject.japan_newhire_rag_backend.domain.organization.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface OrganizationQueryService {

    boolean isValidEmployee(Long employeeId);

    List<Long> findValidEmployeeIdsByDepartmentIds(Collection<Long> departmentIds);

    List<Long> findValidEmployeeIdsByJobGradeIds(Collection<Long> jobGradeIds);

    List<Long> findValidNewHireEmployeeIds();

    List<Long> findManagedEmployeeIds(Long managerEmployeeId);

    boolean isManagedEmployee(Long managerEmployeeId, Long employeeId);

    Long findDirectManagerEmployeeId(Long employeeId);

    List<EmployeeSummary> findEmployeeSummaries(Collection<Long> employeeIds);

    Map<Long, Long> findAppUserIdsByEmployeeIds(Collection<Long> employeeIds);
}
