package com.nexusrag.config;

import com.nexusrag.agent.AssistantAgent;
import com.nexusrag.agent.RetrievalTool;
import com.nexusrag.agent.tool.WebSearchTool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.bge.small.en.v15.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.model.cohere.CohereScoringModel;
import org.springframework.beans.factory.annotation.Value;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;


@Configuration
public class AssistantConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String openAiApiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-3.5-turbo}")
    private String openAiModelName;

    @Value("${langchain4j.cohere.api-key:PLACEHOLDER}")
    private String cohereApiKey;

    /**
     * Local BGE embedding model - runs entirely in-process, no API keys needed.
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new BgeSmallEnV15QuantizedEmbeddingModel();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(openAiModelName)
                .temperature(0.7)
                .build();
    }

    @Bean
    public AssistantAgent assistantAgent(ChatLanguageModel chatLanguageModel,
                                         StreamingChatLanguageModel streamingChatLanguageModel,
                                         RetrievalTool retrievalTool,
                                         WebSearchTool webSearchTool) {
        return AiServices.builder(AssistantAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .tools(retrievalTool, webSearchTool)
                .build();
    }

    @Bean
    public ScoringModel scoringModel() {
        return CohereScoringModel.builder()
                .apiKey(cohereApiKey)
                .modelName("rerank-english-v3.0")
                .build();
    }

    /**
     * In-Memory Vector Store (Bypasses Docker ChromaDB requirement).
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
