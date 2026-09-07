package com.teamproject.japan_newhire_rag_backend.document.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "document.storage")
public record DocumentStorageProperties(String rootDirectory) {

    public DocumentStorageProperties {
        if (rootDirectory == null || rootDirectory.isBlank()) {
            throw new IllegalArgumentException("document.storage.root-directory must not be blank");
        }
    }
}
