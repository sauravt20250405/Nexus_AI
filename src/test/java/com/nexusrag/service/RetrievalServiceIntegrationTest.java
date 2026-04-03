package com.nexusrag.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.data.embedding.Embedding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class RetrievalServiceIntegrationTest {

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private ScoringModel scoringModel;

    @Mock
    private ChatLanguageModel chatModel;

    private RetrievalService retrievalService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        retrievalService = new RetrievalService(embeddingStore, embeddingModel, scoringModel, chatModel);
    }

    @Test
    public void testRetrieveRelevantSegments_ThresholdFiltering() {
        // Arrange
        String query = "University tuition fees";
        when(chatModel.generate(any(String.class))).thenReturn("expanded query for fees");
        
        dev.langchain4j.model.output.Response<Embedding> embedResponse = new dev.langchain4j.model.output.Response<>(new Embedding(new float[]{0.1f}));
        when(embeddingModel.embed(any(String.class))).thenReturn(embedResponse);

        TextSegment lowScoreSegment = TextSegment.from("This segment is vaguely related to costs.");
        TextSegment highScoreSegment = TextSegment.from("The university tuition fees for 2024 are $20,000.");
        
        EmbeddingMatch<TextSegment> match1 = new EmbeddingMatch<>(0.5, "id1", new Embedding(new float[]{0.1f}), lowScoreSegment);
        EmbeddingMatch<TextSegment> match2 = new EmbeddingMatch<>(0.9, "id2", new Embedding(new float[]{0.1f}), highScoreSegment);
        
        EmbeddingSearchResult<TextSegment> searchResult = new EmbeddingSearchResult<>(Arrays.asList(match1, match2));
        when(embeddingStore.findRelevant(any(Embedding.class), anyInt())).thenReturn(searchResult.matches());

        dev.langchain4j.model.output.Response<List<Double>> scoresResponse = new dev.langchain4j.model.output.Response<>(Arrays.asList(0.4, 0.85));
        when(scoringModel.scoreAll(any(), any(String.class))).thenReturn(scoresResponse);

        // Act
        List<TextSegment> results = retrievalService.retrieveRelevantSegments(query);

        // Assert
        // Expecting only the segment with score >= 0.7 to be returned
        assertEquals(1, results.size(), "Only one segment should pass the 0.7 threshold");
        assertEquals("The university tuition fees for 2024 are $20,000.", results.get(0).text());
    }
}
