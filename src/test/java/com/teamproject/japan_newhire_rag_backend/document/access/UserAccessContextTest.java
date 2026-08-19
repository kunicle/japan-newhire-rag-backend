package com.teamproject.japan_newhire_rag_backend.document.access;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UserAccessContextTest {

    @Test
    void rejectsNullRoleIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserAccessContext(null, 1L, 1, true));
    }
}
