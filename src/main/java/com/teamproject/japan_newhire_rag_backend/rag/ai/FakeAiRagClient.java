package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.util.HashMap;
import java.util.Map;

public class FakeAiRagClient implements AiRagClient {

    private final Map<String, AiRagSearchResponse> searchResponsesByQuestion = new HashMap<>();
    private AiRagSearchResponse defaultSearchResponse;
    private AiRagSearchRequest lastSearchRequest;
    private final Map<String, AiRagGenerateResponse> generateResponsesByQuestion = new HashMap<>();
    private AiRagGenerateResponse defaultGenerateResponse;
    private int generateCallCount;
    private AiRagGenerateRequest lastGenerateRequest;

    public void registerSearchResponse(String question, AiRagSearchResponse response) {
        if (question == null || response == null) {
            throw new IllegalArgumentException("question과 response는 null일 수 없습니다.");
        }
        searchResponsesByQuestion.put(question, response);
    }

    public void setDefaultSearchResponse(AiRagSearchResponse response) {
        defaultSearchResponse = response;
    }

    public void registerGenerateResponse(String question, AiRagGenerateResponse response) {
        if (question == null || response == null) {
            throw new IllegalArgumentException("question과 response는 null일 수 없습니다.");
        }
        generateResponsesByQuestion.put(question, response);
    }

    public void setDefaultGenerateResponse(AiRagGenerateResponse response) {
        defaultGenerateResponse = response;
    }

    @Override
    public AiRagSearchResponse search(AiRagSearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request는 null일 수 없습니다.");
        }
        lastSearchRequest = request;

        AiRagSearchResponse registeredResponse = searchResponsesByQuestion.get(request.question());
        if (registeredResponse != null) {
            return registeredResponse;
        }
        if (defaultSearchResponse != null) {
            return defaultSearchResponse;
        }
        throw new IllegalStateException("등록된 검색 응답이 없습니다: " + request.question());
    }

    @Override
    public AiRagGenerateResponse generate(AiRagGenerateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request는 null일 수 없습니다.");
        }

        generateCallCount++;
        lastGenerateRequest = request;

        AiRagGenerateResponse registeredResponse =
                generateResponsesByQuestion.get(request.question());
        if (registeredResponse != null) {
            return registeredResponse;
        }
        if (defaultGenerateResponse != null) {
            return defaultGenerateResponse;
        }
        throw new IllegalStateException("등록된 생성 응답이 없습니다: " + request.question());
    }

    public int getGenerateCallCount() {
        return generateCallCount;
    }

    public AiRagSearchRequest getLastSearchRequest() {
        return lastSearchRequest;
    }

    public AiRagGenerateRequest getLastGenerateRequest() {
        return lastGenerateRequest;
    }
}
