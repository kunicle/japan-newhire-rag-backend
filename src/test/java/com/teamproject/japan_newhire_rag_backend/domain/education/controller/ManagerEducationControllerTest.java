package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.ManagerEducationItemResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.ManagerEducationPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.ManagerEducationQueryService;

@SpringJUnitConfig(
        ManagerEducationControllerTest.TestConfiguration.class)
@WebAppConfiguration
class ManagerEducationControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ManagerEducationQueryService managerEducationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(managerEducationQueryService);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .build();
    }

    @Test
    void getsTeamEducationWithDefaultPagination()
            throws Exception {
        ManagerEducationItemResponse item =
                educationItem(
                        10L,
                        "Employee A",
                        EnrollmentStatus.IN_PROGRESS,
                        false);

        when(managerEducationQueryService
                .getTeamEducation(0, 20))
                .thenReturn(pageResponse(
                        List.of(item),
                        0,
                        20,
                        1));

        mockMvc.perform(get(
                        "/api/manager/team-education"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeId")
                        .value(10))
                .andExpect(jsonPath("$.content[0].employeeName")
                        .value("Employee A"))
                .andExpect(jsonPath("$.content[0].departmentId")
                        .value(100))
                .andExpect(jsonPath("$.content[0].departmentName")
                        .value("Development"))
                .andExpect(jsonPath("$.content[0].enrollmentId")
                        .value(1000))
                .andExpect(jsonPath("$.content[0].courseId")
                        .value(2000))
                .andExpect(jsonPath("$.content[0].courseName")
                        .value("Security basics"))
                .andExpect(jsonPath("$.content[0].progressRate")
                        .value(50.00))
                .andExpect(jsonPath("$.content[0].status")
                        .value("IN_PROGRESS"))
                .andExpect(jsonPath("$.content[0].dueDate")
                        .value("2026-09-30"))
                .andExpect(jsonPath("$.content[0].overdue")
                        .value(false))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements")
                        .value(1));

        verify(managerEducationQueryService)
                .getTeamEducation(0, 20);
    }

    @Test
    void getsTeamEducationWithRequestedPagination()
            throws Exception {
        when(managerEducationQueryService
                .getTeamEducation(2, 10))
                .thenReturn(pageResponse(
                        List.of(),
                        2,
                        10,
                        25));

        mockMvc.perform(get(
                        "/api/manager/team-education")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements")
                        .value(25))
                .andExpect(jsonPath("$.totalPages")
                        .value(3));

        verify(managerEducationQueryService)
                .getTeamEducation(2, 10);
    }

    @Test
    void getsManagedEmployeesCourses()
            throws Exception {
        ManagerEducationItemResponse item =
                educationItem(
                        10L,
                        "Employee A",
                        EnrollmentStatus.OVERDUE,
                        true);

        when(managerEducationQueryService
                .getEmployeeCourses(10L, 0, 20))
                .thenReturn(pageResponse(
                        List.of(item),
                        0,
                        20,
                        1));

        mockMvc.perform(get(
                        "/api/manager/employees/10/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeId")
                        .value(10))
                .andExpect(jsonPath("$.content[0].status")
                        .value("OVERDUE"))
                .andExpect(jsonPath("$.content[0].overdue")
                        .value(true));

        verify(managerEducationQueryService)
                .getEmployeeCourses(10L, 0, 20);
    }

    @Test
    void invalidEmployeeIdReturnsBadRequest()
            throws Exception {
        mockMvc.perform(get(
                        "/api/manager/employees/not-a-number/courses"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Employee ID must be a number"));

        verify(managerEducationQueryService, never())
                .getEmployeeCourses(any(), any(Integer.class),
                        any(Integer.class));
    }

    @Test
    void employeeOutsideRelationshipReturnsForbidden()
            throws Exception {
        when(managerEducationQueryService
                .getEmployeeCourses(999L, 0, 20))
                .thenThrow(new BusinessException(
                        ErrorCode.FORBIDDEN,
                        "Employee is outside the manager's scope"));

        mockMvc.perform(get(
                        "/api/manager/employees/999/courses"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code")
                        .value("FORBIDDEN"));
    }

    private ManagerEducationItemResponse educationItem(
            Long employeeId,
            String employeeName,
            EnrollmentStatus status,
            boolean overdue
    ) {
        return new ManagerEducationItemResponse(
                employeeId,
                employeeName,
                100L,
                "Development",
                1000L,
                2000L,
                "Security basics",
                new BigDecimal("50.00"),
                status,
                LocalDate.of(2026, 9, 30),
                overdue);
    }

    private ManagerEducationPageResponse pageResponse(
            List<ManagerEducationItemResponse> content,
            int page,
            int size,
            long totalElements
    ) {
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil(
                        (double) totalElements / size);

        return new ManagerEducationPageResponse(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                totalPages == 0 || page >= totalPages - 1);
    }

    @Configuration
    @EnableWebMvc
    @Import({
            ManagerEducationController.class,
            GlobalExceptionHandler.class
    })
    static class TestConfiguration {

        @Bean
        ManagerEducationQueryService managerEducationQueryService() {
            return mock(ManagerEducationQueryService.class);
        }
    }
}