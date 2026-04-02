package com.nexusrag.config;

import com.nexusrag.agent.AssistantAgent;
import com.nexusrag.agent.RetrievalTool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.model.cohere.CohereScoringModel;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AssistantConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String openAiApiKey;

    @Value("${langchain4j.cohere.api-key:PLACEHOLDER}")
    private String cohereApiKey;

    @Bean
    public AssistantAgent assistantAgent(ChatLanguageModel chatLanguageModel, RetrievalTool retrievalTool) {
        return AiServices.builder(AssistantAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .tools(retrievalTool)
                .build();
    }

    @Bean
    public ScoringModel scoringModel() {
        // Using Cohere for production-grade re-ranking. 
        // If no key is provided, this might need fallback logic or a dummy implementation.
        return CohereScoringModel.builder()
                .apiKey(cohereApiKey)
                .modelName("rerank-english-v3.0")
                .build();
    }
}
