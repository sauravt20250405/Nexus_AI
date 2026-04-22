package com.nexusrag.config;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AssistantConfig {

    // OpenAI
    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String openAiApiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-3.5-turbo}")
    private String openAiModelName;

    // Ollama
    @Value("${langchain4j.ollama.chat-model.base-url}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.chat-model.model-name}")
    private String ollamaModelName;

    @Value("${langchain4j.ollama.chat-model.temperature}")
    private Double ollamaTemperature;

    // Gemini
    @Value("${langchain4j.gemini.chat-model.api-key}")
    private String geminiApiKey;

    @Value("${langchain4j.gemini.chat-model.model-name:gemini-1.5-pro}")
    private String geminiModelName;

    @Bean("nexusOpenAiChatModel")
    public StreamingChatLanguageModel openAiChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(openAiModelName)
                .temperature(0.7)
                .build();
    }

    @Bean("nexusOllamaChatModel")
    public StreamingChatLanguageModel ollamaChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl(ollamaBaseUrl.endsWith("/") ? ollamaBaseUrl + "v1" : ollamaBaseUrl + "/v1")
                .modelName(ollamaModelName)
                .temperature(ollamaTemperature)
                .build();
    }

    @Bean("nexusGeminiChatModel")
    public StreamingChatLanguageModel geminiChatModel() {
        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModelName)
                .temperature(0.7)
                .build();
    }
}
