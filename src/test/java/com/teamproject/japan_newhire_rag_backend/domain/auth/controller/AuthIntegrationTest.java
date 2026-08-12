package com.teamproject.japan_newhire_rag_backend.domain.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.RefreshTokenGenerator;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthIntegrationTest {

    private static final String RAW_PASSWORD = "Integration-password-123!";
    private static final String DEVICE_INFO = "auth-integration-test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessTokenService accessTokenService;

    @Autowired
    private RefreshTokenGenerator refreshTokenGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<TestUser> testUsers = new ArrayList<>();
    private Long employeeRoleId;
    private boolean employeeRoleCreated;

    @BeforeAll
    void prepareEmployeeRole() {
        List<Long> roleIds = jdbcTemplate.queryForList(
                "select role_id from role where role_code = 'EMPLOYEE'",
                Long.class);
        if (roleIds.isEmpty()) {
            jdbcTemplate.update("""
                    insert into role (role_code, role_name, role_description, is_active)
                    values ('EMPLOYEE', 'Employee', 'Auth integration test role', true)
                    """);
            employeeRoleCreated = true;
            roleIds = jdbcTemplate.queryForList(
                    "select role_id from role where role_code = 'EMPLOYEE'",
                    Long.class);
        }

        employeeRoleId = roleIds.get(0);
        Boolean active = jdbcTemplate.queryForObject(
                "select is_active from role where role_id = ?",
                Boolean.class,
                employeeRoleId);
        assertTrue(Boolean.TRUE.equals(active), "EMPLOYEE role must be active");
    }

    @AfterEach
    void cleanTestUsers() {
        for (int index = testUsers.size() - 1; index >= 0; index--) {
            TestUser user = testUsers.get(index);
            jdbcTemplate.update("delete from refresh_token where app_user_id = ?", user.appUserId());
            jdbcTemplate.update("delete from login_attempt where app_user_id = ?", user.appUserId());
            jdbcTemplate.update("delete from user_role where app_user_id = ?", user.appUserId());
            jdbcTemplate.update("delete from employee where app_user_id = ?", user.appUserId());
            jdbcTemplate.update("delete from app_user where app_user_id = ?", user.appUserId());
            jdbcTemplate.update("delete from department where department_id = ?", user.departmentId());
            jdbcTemplate.update("delete from job_grade where job_grade_id = ?", user.jobGradeId());
        }
        testUsers.clear();
    }

    @AfterAll
    void cleanCreatedRole() {
        if (employeeRoleCreated && employeeRoleId != null) {
            jdbcTemplate.update("delete from role where role_id = ?", employeeRoleId);
        }
    }

    @Test
    void loginRefreshRotationAndLogoutWorkThroughMySql() throws Exception {
        TestUser user = createActiveUser("lifecycle");

        TokenPair loginTokens = login(user.email(), RAW_PASSWORD, DEVICE_INFO);
        assertEquals(user.appUserId(), accessTokenService.validateAndExtractAppUserId(
                loginTokens.accessToken()));

        RefreshRow initialRefresh = findRefreshByRawToken(loginTokens.refreshToken());
        assertEquals(user.appUserId(), initialRefresh.appUserId());
        assertEquals(refreshTokenGenerator.hash(loginTokens.refreshToken()), initialRefresh.tokenHash());
        assertNotEquals(loginTokens.refreshToken(), initialRefresh.tokenHash());
        assertTrue(initialRefresh.expiresAt().isAfter(LocalDateTime.now().plusDays(13)));
        assertTrue(initialRefresh.expiresAt().isBefore(LocalDateTime.now().plusDays(15)));
        assertNull(initialRefresh.revokedAt());
        assertEquals(DEVICE_INFO, initialRefresh.deviceInfo());
        assertEquals(0, countRawRefreshTokenInDatabase(loginTokens.refreshToken()));

        assertEquals("SUCCESS", latestLoginResult(user.appUserId()));
        assertNotNull(jdbcTemplate.queryForObject(
                "select last_login_at from app_user where app_user_id = ?",
                LocalDateTime.class,
                user.appUserId()));

        TokenPair rotatedTokens = refresh(loginTokens.refreshToken());
        assertNotEquals(loginTokens.accessToken(), rotatedTokens.accessToken());
        assertNotEquals(loginTokens.refreshToken(), rotatedTokens.refreshToken());
        assertEquals(user.appUserId(), accessTokenService.validateAndExtractAppUserId(
                rotatedTokens.accessToken()));

        RefreshRow revokedInitial = findRefreshByRawToken(loginTokens.refreshToken());
        RefreshRow rotatedRefresh = findRefreshByRawToken(rotatedTokens.refreshToken());
        assertNotNull(revokedInitial.revokedAt());
        assertNull(rotatedRefresh.revokedAt());
        assertEquals(user.appUserId(), rotatedRefresh.appUserId());
        assertEquals(DEVICE_INFO, rotatedRefresh.deviceInfo());
        assertEquals(0, countRawRefreshTokenInDatabase(rotatedTokens.refreshToken()));

        int tokenCountAfterRotation = countRefreshTokens(user.appUserId());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(loginTokens.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        assertEquals(tokenCountAfterRotation, countRefreshTokens(user.appUserId()));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + rotatedTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(rotatedTokens.refreshToken())))
                .andExpect(status().isNoContent());
        assertNotNull(findRefreshByRawToken(rotatedTokens.refreshToken()).revokedAt());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(rotatedTokens.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void wrongPasswordPersistsFailureWithoutIssuingRefreshToken() throws Exception {
        TestUser user = createActiveUser("failure");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(user.email(), "wrong-password", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));

        assertEquals(1, jdbcTemplate.queryForObject(
                "select failed_login_count from app_user where app_user_id = ?",
                Integer.class,
                user.appUserId()));
        assertEquals("FAILURE", latestLoginResult(user.appUserId()));
        assertEquals("INVALID_CREDENTIALS", jdbcTemplate.queryForObject("""
                select failure_reason from login_attempt
                where app_user_id = ? order by login_attempt_id desc limit 1
                """, String.class, user.appUserId()));
        assertEquals(0, countRefreshTokens(user.appUserId()));
    }

    @Test
    void logoutRejectsRefreshTokenOwnedByAnotherUser() throws Exception {
        TestUser userA = createActiveUser("owner-a");
        TestUser userB = createActiveUser("owner-b");
        TokenPair tokensA = login(userA.email(), RAW_PASSWORD, "device-a");
        TokenPair tokensB = login(userB.email(), RAW_PASSWORD, "device-b");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokensA.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(tokensB.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        assertNull(findRefreshByRawToken(tokensB.refreshToken()).revokedAt());
    }

    private TestUser createActiveUser(String label) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String email = "auth-it-" + label + "-" + suffix + "@example.com";
        String departmentCode = "AIT-D-" + suffix;
        String gradeCode = "AIT-G-" + suffix;
        int gradeLevel = 100_000 + Math.abs(suffix.hashCode() % 900_000);

        jdbcTemplate.update("""
                insert into department
                    (department_code, department_name, department_status, display_order)
                values (?, ?, 'ACTIVE', 0)
                """, departmentCode, "Auth Integration Department");
        Long departmentId = jdbcTemplate.queryForObject(
                "select department_id from department where department_code = ?",
                Long.class,
                departmentCode);

        jdbcTemplate.update("""
                insert into job_grade
                    (grade_code, grade_name, grade_level, is_active)
                values (?, ?, ?, true)
                """, gradeCode, "Auth Integration Grade", gradeLevel);
        Long jobGradeId = jdbcTemplate.queryForObject(
                "select job_grade_id from job_grade where grade_code = ?",
                Long.class,
                gradeCode);

        jdbcTemplate.update("""
                insert into app_user
                    (email, password_hash, account_status, failed_login_count)
                values (?, ?, 'ACTIVE', 0)
                """, email, passwordEncoder.encode(RAW_PASSWORD));
        Long appUserId = jdbcTemplate.queryForObject(
                "select app_user_id from app_user where email = ?",
                Long.class,
                email);

        TestUser user = new TestUser(appUserId, email, departmentId, jobGradeId);
        testUsers.add(user);

        jdbcTemplate.update("""
                insert into employee
                    (app_user_id, department_id, job_grade_id, employee_number,
                     employee_name, employee_type, hire_date, employment_status)
                values (?, ?, ?, ?, ?, 'GENERAL', current_date, 'EMPLOYED')
                """,
                appUserId,
                departmentId,
                jobGradeId,
                "AIT-E-" + suffix,
                "Auth Integration User");
        jdbcTemplate.update("""
                insert into user_role (app_user_id, role_id, granted_by)
                values (?, ?, ?)
                """, appUserId, employeeRoleId, appUserId);

        return user;
    }

    private TokenPair login(String email, String password, String deviceInfo) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password, deviceInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();
        return tokenPair(result);
    }

    private TokenPair refresh(String rawRefreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(rawRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();
        return tokenPair(result);
    }

    private TokenPair tokenPair(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return new TokenPair(
                body.get("accessToken").asText(),
                body.get("refreshToken").asText());
    }

    private RefreshRow findRefreshByRawToken(String rawRefreshToken) {
        String tokenHash = refreshTokenGenerator.hash(rawRefreshToken);
        return jdbcTemplate.queryForObject("""
                select refresh_token_id, app_user_id, token_hash, expires_at,
                       revoked_at, device_info
                from refresh_token where token_hash = ?
                """, (resultSet, rowNumber) -> new RefreshRow(
                resultSet.getLong("refresh_token_id"),
                resultSet.getLong("app_user_id"),
                resultSet.getString("token_hash"),
                resultSet.getObject("expires_at", LocalDateTime.class),
                resultSet.getObject("revoked_at", LocalDateTime.class),
                resultSet.getString("device_info")), tokenHash);
    }

    private int countRawRefreshTokenInDatabase(String rawRefreshToken) {
        return jdbcTemplate.queryForObject(
                "select count(*) from refresh_token where token_hash = ?",
                Integer.class,
                rawRefreshToken);
    }

    private int countRefreshTokens(Long appUserId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from refresh_token where app_user_id = ?",
                Integer.class,
                appUserId);
    }

    private String latestLoginResult(Long appUserId) {
        return jdbcTemplate.queryForObject("""
                select login_result from login_attempt
                where app_user_id = ? order by login_attempt_id desc limit 1
                """, String.class, appUserId);
    }

    private String loginBody(String email, String password, String deviceInfo) {
        String device = deviceInfo == null ? "null" : "\"" + deviceInfo + "\"";
        return "{\"email\":\"" + email + "\",\"password\":\"" + password
                + "\",\"deviceInfo\":" + device + "}";
    }

    private String refreshBody(String refreshToken) {
        return "{\"refreshToken\":\"" + refreshToken + "\"}";
    }

    private record TestUser(
            Long appUserId,
            String email,
            Long departmentId,
            Long jobGradeId
    ) {
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }

    private record RefreshRow(
            Long refreshTokenId,
            Long appUserId,
            String tokenHash,
            LocalDateTime expiresAt,
            LocalDateTime revokedAt,
            String deviceInfo
    ) {
    }
}
