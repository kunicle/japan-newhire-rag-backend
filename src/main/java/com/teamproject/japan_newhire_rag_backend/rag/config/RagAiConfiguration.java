package com.teamproject.japan_newhire_rag_backend.rag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentSearchScopeService;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiEmbeddingClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.PythonAiEmbeddingClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.PythonAiRagClient;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQueryService;
import com.teamproject.japan_newhire_rag_backend.rag.citation.CitationValidator;
import com.teamproject.japan_newhire_rag_backend.rag.evidence.EvidenceThresholdChecker;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagOrchestrator;
import com.teamproject.japan_newhire_rag_backend.rag.search.SearchResultVerifier;

@Configuration
public class RagAiConfiguration {

    @Bean
    public AiRagClient aiRagClient(
            RestClient.Builder restClientBuilder,
            @Value("${rag.ai-server.base-url:http://127.0.0.1:5001}") String baseUrl) {
        return new PythonAiRagClient(restClientBuilder, baseUrl);
    }

    @Bean
    public AiEmbeddingClient aiEmbeddingClient(
            RestClient.Builder restClientBuilder,
            @Value("${rag.ai-server.base-url:http://127.0.0.1:5001}") String baseUrl) {
        RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
        return new PythonAiEmbeddingClient(restClient);
    }

    @Bean
    public SearchResultVerifier searchResultVerifier() {
        return new SearchResultVerifier();
    }

    @Bean
    public EvidenceThresholdChecker evidenceThresholdChecker() {
        return new EvidenceThresholdChecker();
    }

    @Bean
    public CitationValidator citationValidator() {
        return new CitationValidator();
    }

    @Bean
    public RagOrchestrator ragOrchestrator(
            AiRagClient aiRagClient,
            SearchResultVerifier searchResultVerifier,
            EvidenceThresholdChecker evidenceThresholdChecker,
            CitationValidator citationValidator,
            @Value("${rag.evidence-threshold}") double evidenceThreshold) {
        return new RagOrchestrator(
                aiRagClient,
                searchResultVerifier,
                evidenceThresholdChecker,
                citationValidator,
                evidenceThreshold);
    }

    @Bean
    public RagQueryService ragQueryService(
            DocumentSearchScopeService documentSearchScopeService,
            RagOrchestrator ragOrchestrator) {
        return new RagQueryService(documentSearchScopeService, ragOrchestrator);
    }
}
