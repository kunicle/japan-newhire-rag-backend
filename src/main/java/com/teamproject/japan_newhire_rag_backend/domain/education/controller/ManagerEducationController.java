package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.ManagerEducationPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.ManagerEducationQueryService;

@RestController
@RequestMapping("/api/manager")
public class ManagerEducationController {

    private final ManagerEducationQueryService managerEducationQueryService;

    public ManagerEducationController(
            ManagerEducationQueryService managerEducationQueryService
    ) {
        this.managerEducationQueryService =
                managerEducationQueryService;
    }

    @GetMapping("/team-education")
    public ManagerEducationPageResponse getTeamEducation(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return managerEducationQueryService.getTeamEducation(
                page,
                size);
    }

    @GetMapping("/employees/{employeeId}/courses")
    public ManagerEducationPageResponse getEmployeeCourses(
            @PathVariable String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return managerEducationQueryService.getEmployeeCourses(
                parseEmployeeId(employeeId),
                page,
                size);
    }

    private Long parseEmployeeId(String employeeId) {
        try {
            return Long.valueOf(employeeId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Employee ID must be a number");
        }
    }
}