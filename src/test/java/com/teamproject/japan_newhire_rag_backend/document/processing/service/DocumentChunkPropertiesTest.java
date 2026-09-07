package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DocumentChunkPropertiesTest {

    @Test
    void validConfigurationIsAccepted() {
        assertDoesNotThrow(() -> new DocumentChunkProperties(1000, 100));
    }

    @Test
    void rejectsNonPositiveMaxChunkSize() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new DocumentChunkProperties(0, 0)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new DocumentChunkProperties(-1, 0)));
    }

    @Test
    void rejectsNegativeOverlapSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentChunkProperties(1000, -1));
    }

    @Test
    void rejectsOverlapSizeEqualToOrLargerThanMaxChunkSize() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new DocumentChunkProperties(1000, 1000)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new DocumentChunkProperties(1000, 1001)));
    }
}
