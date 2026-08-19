package com.teamproject.japan_newhire_rag_backend.rag.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RagQueryRequest(
        @NotBlank @Size(min = 2, max = 500) String question) {
}
