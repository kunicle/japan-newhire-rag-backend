package com.teamproject.japan_newhire_rag_backend.domain.auth.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.LoginFailureReason;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.LoginResult;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "login_attempt")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_attempt_id")
    private Long loginAttemptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id")
    private AppUser appUser;

    @Column(name = "input_email", nullable = false, length = 100)
    private String inputEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_result", nullable = false, length = 20)
    private LoginResult loginResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 30)
    private LoginFailureReason failureReason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static LoginAttempt success(
            AppUser appUser,
            String inputEmail,
            LocalDateTime attemptedAt
    ) {
        LoginAttempt loginAttempt = new LoginAttempt();
        loginAttempt.appUser = appUser;
        loginAttempt.inputEmail = inputEmail;
        loginAttempt.loginResult = LoginResult.SUCCESS;
        loginAttempt.attemptedAt = attemptedAt;
        return loginAttempt;
    }

    public static LoginAttempt failure(
            AppUser appUser,
            String inputEmail,
            LoginFailureReason failureReason,
            LocalDateTime attemptedAt
    ) {
        LoginAttempt loginAttempt = new LoginAttempt();
        loginAttempt.appUser = appUser;
        loginAttempt.inputEmail = inputEmail;
        loginAttempt.loginResult = LoginResult.FAILURE;
        loginAttempt.failureReason = failureReason;
        loginAttempt.attemptedAt = attemptedAt;
        return loginAttempt;
    }
}
