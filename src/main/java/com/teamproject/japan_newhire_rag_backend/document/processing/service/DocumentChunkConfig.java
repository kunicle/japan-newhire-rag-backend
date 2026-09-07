package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DocumentChunkProperties.class)
public class DocumentChunkConfig {
}
