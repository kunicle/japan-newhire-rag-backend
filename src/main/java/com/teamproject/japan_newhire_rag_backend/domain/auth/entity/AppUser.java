package com.teamproject.japan_newhire_rag_backend.domain.auth.entity;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "app_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends BaseEntity {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_user_id")
    private Long appUserId;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void recordLoginFailure(LocalDateTime attemptedAt) {
        failedLoginCount++;

        if (failedLoginCount >= MAX_FAILED_LOGIN_ATTEMPTS) {
            accountStatus = AccountStatus.LOCKED;
            lockedUntil = attemptedAt.plusMinutes(LOCK_DURATION_MINUTES);
        }
    }

    public void recordLoginSuccess(LocalDateTime loggedInAt) {
        failedLoginCount = 0;
        lockedUntil = null;
        lastLoginAt = loggedInAt;
    }

    public boolean unlockIfExpired(LocalDateTime currentTime) {
        if (accountStatus != AccountStatus.LOCKED
                || lockedUntil == null
                || lockedUntil.isAfter(currentTime)) {
            return false;
        }

        accountStatus = AccountStatus.ACTIVE;
        failedLoginCount = 0;
        lockedUntil = null;
        return true;
    }
}
