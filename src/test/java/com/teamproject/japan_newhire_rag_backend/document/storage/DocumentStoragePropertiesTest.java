package com.teamproject.japan_newhire_rag_backend.document.storage;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DocumentStoragePropertiesTest {

    @Test
    void rejectsNullRootDirectory() {
        assertThrows(IllegalArgumentException.class, () -> new DocumentStorageProperties(null));
    }

    @Test
    void rejectsBlankRootDirectory() {
        assertThrows(IllegalArgumentException.class, () -> new DocumentStorageProperties("   "));
    }
}
