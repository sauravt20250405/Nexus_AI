package com.nexusrag.service;

import dev.langchain4j.data.message.UserMessage;
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
    private final ScoringModel scoringModel; // Cross-Encoder for Re-Ranking
    private final ChatLanguageModel chatModel; // For Query Expansion

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
        String prompt = "You are a Query Expansion Agent. Rewrite the following user query for a university assistant to be more precise for vector search. " +
                "Think about synonyms for university terms (e.g. 'admission' -> 'enrollment', 'scholarship' -> 'financial aid'). " +
                "Original: " + originalQuery;
        return chatModel.generate(prompt);
    }

    /**
     * Hybrid Search Implementation + Re-Ranking.
     * Retains only results with score >= 0.7 after re-ranking.
     */
    public List<TextSegment> retrieveRelevantSegments(String query) {
        String expandedQuery = expandQuery(query);

        // Pre-fetch 20 candidates for better re-ranking pool
        var searchResults = embeddingStore.findRelevant(embeddingModel.embed(expandedQuery).content(), 20);

        if (searchResults.isEmpty()) {
            return List.of();
        }

        // Re-Ranking using Cross-Encoder Scoring Model
        List<TextSegment> segmentsToRank = searchResults.stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());

        List<Double> scores = scoringModel.scoreAll(segmentsToRank, expandedQuery).content();

        // Filter by score threshold (0.7) and return sorted top-5
        return segmentsToRank.stream()
                .filter(segment -> {
                    int index = segmentsToRank.indexOf(segment);
                    return scores.get(index) >= 0.7;
                })
                .sorted((s1, s2) -> {
                    int idx1 = segmentsToRank.indexOf(s1);
                    int idx2 = segmentsToRank.indexOf(s2);
                    return Double.compare(scores.get(idx2), scores.get(idx1));
                })
                .limit(5)
                .collect(Collectors.toList());
    }
}
