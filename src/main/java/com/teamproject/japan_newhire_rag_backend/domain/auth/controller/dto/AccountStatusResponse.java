package com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;

public record AccountStatusResponse(Long appUserId, AccountStatus accountStatus) {
}
