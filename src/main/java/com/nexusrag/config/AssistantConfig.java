package com.nexusrag.config;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
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

    // Groq (OpenAI-Compatible)
    @Value("${langchain4j.groq.api-key}")
    private String groqApiKey;

    @Value("${langchain4j.groq.model-name:llama-3.3-70b-versatile}")
    private String groqModelName;

    @Lazy
    @Bean("nexusOpenAiChatModel")
    public StreamingChatLanguageModel openAiChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(openAiModelName)
                .temperature(0.7)
                .build();
    }

    @Lazy
    @Bean("nexusOllamaChatModel")
    public StreamingChatLanguageModel ollamaChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl(ollamaBaseUrl.endsWith("/") ? ollamaBaseUrl + "v1" : ollamaBaseUrl + "/v1")
                .modelName(ollamaModelName)
                .temperature(ollamaTemperature)
                .build();
    }

    @Lazy
    @Bean("nexusGeminiChatModel")
    public StreamingChatLanguageModel geminiChatModel() {
        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModelName)
                .temperature(0.7)
                .build();
    }

    @Lazy
    @Bean("nexusOpenAiImageModel")
    public dev.langchain4j.model.image.ImageModel openAiImageModel() {
        return OpenAiImageModel.builder()
                .apiKey(openAiApiKey)
                .modelName("dall-e-3")
                .build();
    }

    @Lazy
    @Bean("nexusGroqChatModel")
    public StreamingChatLanguageModel groqChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .apiKey(groqApiKey)
                .modelName(groqModelName)
                .temperature(0.7)
                .build();
    }
}
