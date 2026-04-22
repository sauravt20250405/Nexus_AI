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
            You are Nexus AI, a powerful autonomous internet-connected assistant with advanced reasoning capabilities.
            
            RULES:
            1. Live Intelligence: If you need to verify facts, fetch live data, or answer current events, USE the searchWorldWideWeb tool.
            2. Transparency: Be honest if you cannot find reliable info or if you don't know the answer.
            3. Tone: Be helpful, modern, conversational, and concise. Your goal is to be a top-tier personal assistant.
            """)
    String answer(@UserMessage String question);

    TokenStream streamAnswer(@UserMessage String query);

    TokenStream streamAnswer(dev.langchain4j.data.message.UserMessage message);
}
