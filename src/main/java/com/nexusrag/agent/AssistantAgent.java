package com.nexusrag.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.TokenStream;

/**
 * Nexus RAG: Autonomous University Assistant Agent.
 * Uses Source Grounding and strict citing format: [Source: FileName, Page: X].
 * 
 * NOTE: This interface is wired up via AiServices.builder() in AssistantConfig.
 * Do NOT use @AiService here as it conflicts with the manual bean definition.
 */
public interface AssistantAgent {

    @SystemMessage("""
            You are Nexus RAG, a production-grade autonomous university assistant.
            Use the provided context to answer questions accurately and concisely.
            
            RULES:
            1. Source Grounding: You MUST cite your sources at the end of each statement using the format: [Source: FileName, Page: X].
            2. Web Search: If information is MISSING from the uploaded PDFs, USE the searchUniversityWebsite tool to fetch live data.
            3. Self-Correction: If retrieval scores are low (< 0.7) and search also fails, state: "I apologize, but I do not have enough information to answer this reliably." 
            4. Do not hallucinate. Citation-less answers are forbidden.
            """)
    String answer(@UserMessage String question);

    TokenStream streamAnswer(@UserMessage String query);
}
