package com.nexusrag.config;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Custom StreamingChatLanguageModel that attempts a primary model (OpenAI) 
 * and falls back to a non-streaming secondary model (Ollama) on failure.
 * 
 * We use a non-streaming fallback because Ollama's OpenAI-compatible SSE 
 * streaming is unstable with current LangChain4j versions when tools are involved.
 */
public class FallbackStreamingChatModel implements StreamingChatLanguageModel {
    private static final Logger log = LoggerFactory.getLogger(FallbackStreamingChatModel.class);

    private final StreamingChatLanguageModel primary;
    private final ChatLanguageModel fallback;

    public FallbackStreamingChatModel(StreamingChatLanguageModel primary, ChatLanguageModel fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        primary.generate(messages, new FallbackHandler(messages, null, null, handler));
    }

    @Override
    public void generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications, StreamingResponseHandler<AiMessage> handler) {
        primary.generate(messages, toolSpecifications, new FallbackHandler(messages, toolSpecifications, null, handler));
    }

    @Override
    public void generate(List<ChatMessage> messages, ToolSpecification toolSpecification, StreamingResponseHandler<AiMessage> handler) {
        primary.generate(messages, toolSpecification, new FallbackHandler(messages, null, toolSpecification, handler));
    }

    private class FallbackHandler implements StreamingResponseHandler<AiMessage> {
        private final List<ChatMessage> messages;
        private final List<ToolSpecification> toolSpecifications;
        private final ToolSpecification toolSpecification;
        private final StreamingResponseHandler<AiMessage> delegate;
        private boolean tokenEmitted = false;

        public FallbackHandler(List<ChatMessage> messages,
                               List<ToolSpecification> toolSpecifications,
                               ToolSpecification toolSpecification,
                               StreamingResponseHandler<AiMessage> delegate) {
            this.messages = messages;
            this.toolSpecifications = toolSpecifications;
            this.toolSpecification = toolSpecification;
            this.delegate = delegate;
        }

        @Override
        public void onNext(String token) {
            tokenEmitted = true;
            delegate.onNext(token);
        }

        @Override
        public void onComplete(Response<AiMessage> response) {
            delegate.onComplete(response);
        }

        @Override
        public void onError(Throwable error) {
            if (tokenEmitted) {
                log.error("Primary model failed mid-stream: {}. Cannot fallback.", error.getMessage());
                delegate.onError(error);
            } else {
                log.warn("Primary model failed on initial request: {}. Falling back to non-streaming secondary model.", error.getMessage());
                try {
                    Response<AiMessage> response;
                    if (toolSpecifications != null) {
                        response = fallback.generate(messages, toolSpecifications);
                    } else if (toolSpecification != null) {
                        response = fallback.generate(messages, toolSpecification);
                    } else {
                        response = fallback.generate(messages);
                    }

                    if (response != null && response.content() != null) {
                        String text = response.content().text();
                        if (text != null && !text.isEmpty()) {
                            delegate.onNext(text);
                        }
                        delegate.onComplete(response);
                    } else {
                        log.error("Fallback model returned empty response.");
                        delegate.onError(new RuntimeException("Fallback model returned empty response."));
                    }
                } catch (Exception e) {
                    log.error("Fallback model also failed: {}", e.getMessage(), e);
                    delegate.onError(e);
                }
            }
        }
    }
}
