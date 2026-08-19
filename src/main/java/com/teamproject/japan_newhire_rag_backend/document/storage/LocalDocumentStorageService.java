package com.teamproject.japan_newhire_rag_backend.document.storage;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class LocalDocumentStorageService implements DocumentStorageService {

    private final Path rootPath;

    public LocalDocumentStorageService(DocumentStorageProperties properties) {
        this.rootPath = Path.of(properties.rootDirectory()).toAbsolutePath().normalize();
    }

    @Override
    public String store(String originalFileName, byte[] content) {
        Path temporaryFile = null;

        try {
            Files.createDirectories(rootPath);

            String storedFileName = UUID.randomUUID() + ".txt";
            Path targetFile = resolveWithinRoot(storedFileName);
            temporaryFile = Files.createTempFile(rootPath, ".document-", ".tmp");
            Files.write(temporaryFile, content);

            moveToTarget(temporaryFile, targetFile);
            temporaryFile = null;
            return storedFileName;
        } catch (IOException exception) {
            deleteTemporaryFile(temporaryFile, exception);
            throw new IllegalStateException("문서 파일을 저장할 수 없습니다.", exception);
        }
    }

    @Override
    public void delete(String storedFilePath) {
        if (storedFilePath == null || storedFilePath.isBlank()) {
            throw new IllegalArgumentException("저장된 파일 경로가 비어 있습니다.");
        }

        Path targetFile = resolveWithinRoot(storedFilePath);
        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException exception) {
            throw new IllegalStateException("문서 파일을 삭제할 수 없습니다.", exception);
        }
    }

    private void moveToTarget(Path temporaryFile, Path targetFile) throws IOException {
        try {
            Files.move(
                    temporaryFile,
                    targetFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path resolveWithinRoot(String storedFilePath) {
        Path resolvedPath = rootPath.resolve(storedFilePath).normalize();
        if (resolvedPath.equals(rootPath) || !resolvedPath.startsWith(rootPath)) {
            throw new IllegalArgumentException("저장소 루트 밖의 경로는 사용할 수 없습니다.");
        }
        return resolvedPath;
    }

    private void deleteTemporaryFile(Path temporaryFile, IOException originalException) {
        if (temporaryFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }
}
