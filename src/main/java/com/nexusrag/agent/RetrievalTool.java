package com.nexusrag.agent;

import com.nexusrag.service.RetrievalService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RetrievalTool {

    private final RetrievalService retrievalService;

    public RetrievalTool(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    /**
     * Agentic Tool for high-dimensional search and re-ranking.
     * The agent calls this tool when it needs context to answer a university-related query.
     */
    @Tool("Search the university knowledge base for relevant documents and academic policies.")
    public String searchUniversityKnowledge(String query) {
        List<TextSegment> segments = retrievalService.retrieveRelevantSegments(query);
        
        if (segments.isEmpty()) {
            return "No relevant university documents found with sufficient confidence (score < 0.7).";
        }

        return segments.stream()
                .map(segment -> String.format("[%s, Page: %s]: %s", 
                        segment.metadata().getString("source_name"),
                        segment.metadata().getString("page_number"),
                        segment.text()))
                .collect(Collectors.joining("\n\n"));
    }
}
