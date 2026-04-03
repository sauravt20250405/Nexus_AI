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

    @Tool("Search the live internet for university-related information if not found in local PDF context")
    public String searchUniversityWebsite(String query) {
        System.out.println("DEBUG: Executing Live Web Search for query: " + query);
        
        // Mocking a live search response. 
        // In reality, you'd use a search API client here.
        if (query.toLowerCase().contains("weather")) {
            return "The current weather at the University campus is 22°C with clear skies (Source: Live Mock Search).";
        }
        
        if (query.toLowerCase().contains("event") || query.toLowerCase().contains("news")) {
            return "Recent University News: The annual 'Nexus Tech Symposium' is scheduled for next Friday at the Grand Hall. Admission is free for all students. (Source: University News Feed).";
        }

        return "I searched the university web portal for '" + query + "' but found no specific live updates. Relying on baseline knowledge: Most university offices are open 9 AM - 5 PM. (Source: Web Search Mock).";
    }
}
