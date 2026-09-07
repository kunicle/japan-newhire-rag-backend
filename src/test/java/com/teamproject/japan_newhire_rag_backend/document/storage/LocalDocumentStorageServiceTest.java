package com.teamproject.japan_newhire_rag_backend.document.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalDocumentStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesContentAndReturnsReference() throws Exception {
        LocalDocumentStorageService service = createService(tempDirectory);

        String reference = service.store("규정.txt", "content".getBytes(StandardCharsets.UTF_8));

        assertTrue(Files.exists(tempDirectory.resolve(reference)));
    }

    @Test
    void storedBytesMatchOriginal() throws Exception {
        LocalDocumentStorageService service = createService(tempDirectory);
        byte[] content = "신입사원 문서 저장 테스트입니다.".getBytes(StandardCharsets.UTF_8);

        String reference = service.store("규정.txt", content);

        assertArrayEquals(content, Files.readAllBytes(tempDirectory.resolve(reference)));
    }

    @Test
    void generatesUniqueNames() {
        LocalDocumentStorageService service = createService(tempDirectory);
        byte[] content = "content".getBytes(StandardCharsets.UTF_8);

        String firstReference = service.store("규정.txt", content);
        String secondReference = service.store("규정.txt", content);

        assertNotEquals(firstReference, secondReference);
        assertTrue(Files.exists(tempDirectory.resolve(firstReference)));
        assertTrue(Files.exists(tempDirectory.resolve(secondReference)));
    }

    @Test
    void preservesTxtExtension() {
        LocalDocumentStorageService service = createService(tempDirectory);

        String reference = service.store("규정.txt", "content".getBytes(StandardCharsets.UTF_8));

        assertTrue(reference.endsWith(".txt"));
    }

    @Test
    void doesNotUseOriginalFilenameAsPath() {
        LocalDocumentStorageService service = createService(tempDirectory);

        String reference = service.store("규정.txt", "content".getBytes(StandardCharsets.UTF_8));

        assertFalse(reference.contains("규정"));
        assertTrue(Files.exists(tempDirectory.resolve(reference)));
    }

    @Test
    void createsDirectoryIfMissing() {
        Path missingDirectory = tempDirectory.resolve("nested").resolve("documents");
        LocalDocumentStorageService service = createService(missingDirectory);

        String reference = service.store("규정.txt", "content".getBytes(StandardCharsets.UTF_8));

        assertTrue(Files.exists(missingDirectory.resolve(reference)));
    }

    @Test
    void deleteRemovesStoredFile() {
        LocalDocumentStorageService service = createService(tempDirectory);
        String reference = service.store("규정.txt", "content".getBytes(StandardCharsets.UTF_8));

        service.delete(reference);

        assertFalse(Files.exists(tempDirectory.resolve(reference)));
    }

    @Test
    void deleteMissingFileIsIdempotent() {
        LocalDocumentStorageService service = createService(tempDirectory);

        assertDoesNotThrow(() -> service.delete("550e8400-e29b-41d4-a716-446655440000.txt"));
    }

    @Test
    void pathTraversalFilenameCannotEscapeRoot() {
        Path storageRoot = tempDirectory.resolve("nested").resolve("documents");
        LocalDocumentStorageService service = createService(storageRoot);
        Path outsideFile = tempDirectory.resolve("secret.txt");

        String reference = service.store("../../secret.txt", "content".getBytes(StandardCharsets.UTF_8));

        assertTrue(Files.exists(storageRoot.resolve(reference)));
        assertFalse(Files.exists(outsideFile));
    }

    @Test
    void deleteRejectsPathTraversal() {
        LocalDocumentStorageService service = createService(tempDirectory);

        assertThrows(IllegalArgumentException.class, () -> service.delete("../outside.txt"));
    }

    private LocalDocumentStorageService createService(Path rootDirectory) {
        return new LocalDocumentStorageService(new DocumentStorageProperties(rootDirectory.toString()));
    }
}
