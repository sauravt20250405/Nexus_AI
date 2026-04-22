package com.nexusrag.controller;

import com.nexusrag.agent.AssistantAgent;
import com.nexusrag.model.AuditLog;
import com.nexusrag.repository.AuditLogRepository;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nexus")
@CrossOrigin(origins = "*")
public class NexusRagController {

    private final StreamingChatLanguageModel openAiChatModel;
    private final StreamingChatLanguageModel ollamaChatModel;
    private final StreamingChatLanguageModel geminiChatModel;
    private final StreamingChatLanguageModel groqChatModel;
    private final dev.langchain4j.model.image.ImageModel imageModel;
    private final com.nexusrag.agent.tool.WebSearchTool webSearchTool;
    private final com.nexusrag.agent.tool.AgentTools agentTools;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final dev.langchain4j.memory.ChatMemory chatMemory;
    
    // Persistent Embedding Store (Lazy-loaded to save memory on cloud)
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
    private volatile dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;
    
    private dev.langchain4j.model.embedding.EmbeddingModel getEmbeddingModel() {
        if (embeddingModel == null) {
            synchronized (this) {
                if (embeddingModel == null) {
                    embeddingModel = new AllMiniLmL6V2EmbeddingModel();
                }
            }
        }
        return embeddingModel;
    }

    public NexusRagController(
            @org.springframework.beans.factory.annotation.Qualifier("nexusOpenAiChatModel") StreamingChatLanguageModel openAiChatModel,
            @org.springframework.beans.factory.annotation.Qualifier("nexusOllamaChatModel") StreamingChatLanguageModel ollamaChatModel,
            @org.springframework.beans.factory.annotation.Qualifier("nexusGeminiChatModel") StreamingChatLanguageModel geminiChatModel,
            @org.springframework.beans.factory.annotation.Qualifier("nexusGroqChatModel") StreamingChatLanguageModel groqChatModel,
            @org.springframework.beans.factory.annotation.Qualifier("nexusOpenAiImageModel") dev.langchain4j.model.image.ImageModel imageModel,
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper) {
        this.openAiChatModel = openAiChatModel;
        this.ollamaChatModel = ollamaChatModel;
        this.geminiChatModel = geminiChatModel;
        this.groqChatModel = groqChatModel;
        this.imageModel = imageModel;
        this.webSearchTool = new com.nexusrag.agent.tool.WebSearchTool();
        this.agentTools = new com.nexusrag.agent.tool.AgentTools();
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.chatMemory = dev.langchain4j.memory.chat.MessageWindowChatMemory.withMaxMessages(20);
    }

    @PostMapping("/chat")
    public SseEmitter chatStream(
            @RequestParam("query") String query, 
            @RequestParam(value = "model", defaultValue = "ollama") String model,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        
        long startTime = System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(180000L);

        // DALL-E Image Interceptor
        if (query.trim().startsWith("/imagine")) {
            String imgQuery = query.replace("/imagine", "").trim();
            new Thread(() -> {
                try {
                    String url = imageModel.generate(imgQuery).content().url().toString();
                    String md = "![Generated Image](" + url + ")";
                    String json = objectMapper.writeValueAsString(Map.of("type", "token", "content", md));
                    emitter.send(SseEmitter.event().data(json));
                    emitter.complete();
                } catch (Exception e) {
                    try {
                        String errorJson = objectMapper.writeValueAsString(Map.of("type", "error", "content", "DALL-E Failed: " + e.getMessage()));
                        emitter.send(SseEmitter.event().data(errorJson));
                    } catch (IOException ignored){}
                    emitter.completeWithError(e);
                }
            }).start();
            return emitter;
        }

        List<Content> contents = new ArrayList<>();
        StringBuilder finalQuery = new StringBuilder(query);
                
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                String contentType = file.getContentType();
                if (contentType != null && contentType.startsWith("image")) {
                    if (!"ollama".equalsIgnoreCase(model)) {
                        try {
                            byte[] bytes = file.getBytes();
                            String base64 = Base64.getEncoder().encodeToString(bytes);
                            contents.add(ImageContent.from(base64, contentType));
                        } catch (IOException ignored) {}
                    }
                } else {
                    try (InputStream is = file.getInputStream()) {
                        Document doc = new ApacheTikaDocumentParser().parse(is);
                        // Semantic Vector RAG Processing
                        List<TextSegment> segments = DocumentSplitters.recursive(1000, 200).split(doc);
                        for (TextSegment segment : segments) {
                            embeddingStore.add(getEmbeddingModel().embed(segment).content(), segment);
                        }
                        finalQuery.append("\n\n(System Note: Document '").append(file.getOriginalFilename()).append("' successfully vectorized into memory space.)\n");
                    } catch (Exception ignored) {}
                }
            }
        }
                
        contents.add(0, TextContent.from(finalQuery.toString()));
        UserMessage userMessage = UserMessage.from(contents);

        new Thread(() -> {
            try {
                StreamingChatLanguageModel selectedModel = switch(model.toLowerCase()) {
                    case "ollama" -> ollamaChatModel;
                    case "gemini" -> geminiChatModel;
                    case "groq" -> groqChatModel;
                    default -> openAiChatModel;
                };

                dev.langchain4j.service.AiServices<AssistantAgent> builder = dev.langchain4j.service.AiServices.builder(AssistantAgent.class)
                    .streamingChatLanguageModel(selectedModel)
                    .chatMemory(chatMemory);

                // Auto-inject Semantic RAG Context Retriever
                EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                        .embeddingStore(embeddingStore)
                        .embeddingModel(getEmbeddingModel())
                        .maxResults(5)
                        .minScore(0.6)
                        .build();
                builder = builder.contentRetriever(retriever);

                if ("openai".equalsIgnoreCase(model)) {
                    builder = builder.tools(webSearchTool, agentTools);
                }

                AssistantAgent agent = builder.build();

                agent.streamAnswer(userMessage)
                    .onNext(token -> {
                        try {
                            String json = objectMapper.writeValueAsString(Map.of("type", "token", "content", token));
                            emitter.send(SseEmitter.event().data(json));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .onComplete(response -> {
                        try {
                            AuditLog log = new AuditLog();
                            log.setUserQuery(query); 
                            log.setBotResponse((response != null && response.content() != null) ? response.content().text() : "");
                            log.setProcessTimeMs(System.currentTimeMillis() - startTime);
                            log.setRetrievalScore(0.0);
                            auditLogRepository.save(log);

                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .onError(error -> {
                        try {
                            String errorStr = error.getMessage() != null ? error.getMessage() : "Unknown Error";
                            String errorJson = objectMapper.writeValueAsString(Map.of("type", "error", "content", errorStr));
                            emitter.send(SseEmitter.event().data(errorJson));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .start();
            } catch (Exception e) {
                try {
                    String errorStr = e.getMessage() != null ? e.getMessage() : "Unknown Error";
                    String errorJson = objectMapper.writeValueAsString(Map.of("type", "error", "content", "Processing error: " + errorStr));
                    emitter.send(SseEmitter.event().data(errorJson));
                } catch (IOException ioException) {}
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    @GetMapping("/history")
    public ResponseEntity<List<AuditLog>> getChatHistory() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    @DeleteMapping("/memory")
    public ResponseEntity<String> clearMemory() {
        chatMemory.clear();
        return ResponseEntity.ok("Memory Cleared");
    }
}
