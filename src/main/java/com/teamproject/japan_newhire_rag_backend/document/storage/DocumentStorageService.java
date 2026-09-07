package com.teamproject.japan_newhire_rag_backend.document.storage;

public interface DocumentStorageService {

    String store(String originalFileName, byte[] content);

    void delete(String storedFilePath);
}
