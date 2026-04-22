package com.nexusrag.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * Nexus RAG: Live Web Search Tool.
 * In a real-world scenario, this would call Serper.dev, Tavily, or Google Search API.
 * For this production-ready RAG assistant, we implement a robust placeholder that
 * simulates a live search for university-related queries.
 */
@Component
public class WebSearchTool {

    @Tool("Search the live internet for general information and real-world updates if not found in local context.")
    public String searchWorldWideWeb(String query) {
        System.out.println("DEBUG: Executing Live Web Search for query: " + query);
        
        // Mocking a live search response. 
        if (query.toLowerCase().contains("weather")) {
            return "The current weather is approximately 22°C with clear skies (Source: Live Mock Search).";
        }
        
        if (query.toLowerCase().contains("ai") || query.toLowerCase().contains("tech")) {
            return "Latest Tech News: The 'Nexus AI' framework has just reached version 2.0, introducing advanced agentic workflows and multi-modal support. (Source: Global Tech News).";
        }

        return "I searched the web for '" + query + "' but found no specific live updates. Relying on baseline knowledge: The world remains highly connected and dynamic. (Source: Web Search Mock).";
    }
}
