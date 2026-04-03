package com.nexusrag.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetrievalService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ScoringModel scoringModel;
    private final ChatLanguageModel chatModel;

    @org.springframework.beans.factory.annotation.Value("${nexus.rag.query-expansion-prompt:You are a Query Expansion Agent. Rewrite the following user query for a university assistant to be more precise for vector search. Think about synonyms for university terms (e.g. 'admission' -> 'enrollment', 'scholarship' -> 'financial aid'). Original: %s}")
    private String queryExpansionPrompt = "You are a Query Expansion Agent. Rewrite the following user query for a university assistant to be more precise for vector search. Think about synonyms for university terms (e.g. 'admission' -> 'enrollment', 'scholarship' -> 'financial aid'). Original: %s";

    @org.springframework.beans.factory.annotation.Value("${nexus.rag.retrieval-limit:20}")
    private int retrievalLimit = 20;

    public RetrievalService(EmbeddingStore<TextSegment> embeddingStore,
                            EmbeddingModel embeddingModel, 
                            ScoringModel scoringModel,
                            ChatLanguageModel chatModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.scoringModel = scoringModel;
        this.chatModel = chatModel;
    }

    /**
     * Query Expansion: Rewrites vague user queries into optimized search strings.
     */
    public String expandQuery(String originalQuery) {
        String prompt = String.format(queryExpansionPrompt, originalQuery);
        return chatModel.generate(prompt);
    }

    /**
     * Hybrid Search Implementation + Re-Ranking.
     * Retains only results with score >= 0.7 after re-ranking.
     */
    public List<TextSegment> retrieveRelevantSegments(String query) {
        String expandedQuery = expandQuery(query);

        // Pre-fetch candidates for better re-ranking pool
        var searchResults = embeddingStore.findRelevant(embeddingModel.embed(expandedQuery).content(), retrievalLimit);

        if (searchResults.isEmpty()) {
            return List.of();
        }

        // Re-Ranking using Cross-Encoder Scoring Model
        List<TextSegment> segmentsToRank = searchResults.stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());

        List<Double> scores = scoringModel.scoreAll(segmentsToRank, expandedQuery).content();

        // Capture metadata scores and unique sources for citation badges
        double maxScore = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        RetrievalContextHolder.setMaxScore(maxScore);

        List<TextSegment> topSegments = java.util.stream.IntStream.range(0, segmentsToRank.size())
                .filter(i -> scores.get(i) >= 0.7)
                .boxed()
                .sorted((i, j) -> Double.compare(scores.get(j), scores.get(i)))
                .map(segmentsToRank::get)
                .limit(5)
                .collect(Collectors.toList());

        // Extract metadata for the UI
        List<java.util.Map<String, String>> citations = topSegments.stream()
                .map(seg -> seg.metadata().asMap())
                .collect(Collectors.toList());
        RetrievalContextHolder.setSources(citations);

        return topSegments;
    }
}
