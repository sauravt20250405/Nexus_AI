package com.nexusrag.config;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FallbackChatModel implements ChatLanguageModel {
    private static final Logger log = LoggerFactory.getLogger(FallbackChatModel.class);

    private final ChatLanguageModel primary;
    private final ChatLanguageModel fallback;

    public FallbackChatModel(ChatLanguageModel primary, ChatLanguageModel fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        try {
            return primary.generate(messages);
        } catch (Exception e) {
            log.warn("Primary model failed: {}. Falling back to secondary model.", e.getMessage());
            return fallback.generate(messages);
        }
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        try {
            return primary.generate(messages, toolSpecifications);
        } catch (Exception e) {
            log.warn("Primary model failed with tools: {}. Falling back to secondary model.", e.getMessage());
            return fallback.generate(messages, toolSpecifications);
        }
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, ToolSpecification toolSpecification) {
        try {
            return primary.generate(messages, toolSpecification);
        } catch (Exception e) {
            log.warn("Primary model failed with single tool: {}. Falling back to secondary model.", e.getMessage());
            return fallback.generate(messages, toolSpecification);
        }
    }
}
