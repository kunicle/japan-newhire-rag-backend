package com.teamproject.japan_newhire_rag_backend.rag.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record RagQueryRequest(
        @NotBlank String question) {
}
