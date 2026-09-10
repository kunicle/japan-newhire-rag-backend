package com.teamproject.japan_newhire_rag_backend.document.storage;

public interface DocumentStorageService {

    String store(String originalFileName, byte[] content);

    byte[] load(String storedFilePath);

    void delete(String storedFilePath);

}
