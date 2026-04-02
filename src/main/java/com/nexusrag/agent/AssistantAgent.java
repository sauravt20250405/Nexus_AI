package com.nexusrag.agent;

import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.TokenStream;

/**
 * Nexus RAG: Autonomous University Assistant Agent.
 * Uses Source Grounding and strict citing format: [Source: FileName, Page: X].
 */
@AiService
public interface AssistantAgent {

    @SystemMessage("""
            You are Nexus RAG, a production-grade autonomous university assistant.
            Use the provided context to answer questions accurately and concisely.
            
            RULES:
            1. Source Grounding: You MUST cite your sources at the end of each statement using the format: [Source: FileName, Page: X].
            2. Self-Correction & Reasoning: If the provided context is insufficient or retrieval scores are low (score < 0.7), state clearly: "I apologize, but I do not have enough information to answer this reliably." 
            3. Do not hallucinate. If you are unsure, say "I don't know."
            4. If multi-step reasoning is required, explain your steps briefly.
            """)
    String answer(@UserMessage String question);

    TokenStream streamAnswer(@UserMessage String query);
}
