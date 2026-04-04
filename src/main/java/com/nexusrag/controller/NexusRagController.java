package com.nexusrag.controller;

import com.nexusrag.agent.AssistantAgent;
import com.nexusrag.model.AuditLog;
import com.nexusrag.repository.AuditLogRepository;
import com.nexusrag.service.DocumentService;
import com.nexusrag.service.RetrievalContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nexus")
@CrossOrigin(origins = "*")
public class NexusRagController {

    private final AssistantAgent assistantAgent;
    private final DocumentService documentService;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public NexusRagController(AssistantAgent assistantAgent, 
                              DocumentService documentService,
                              AuditLogRepository auditLogRepository,
                              ObjectMapper objectMapper) {
        this.assistantAgent = assistantAgent;
        this.documentService = documentService;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestDocument(@RequestParam("file") MultipartFile file) {
        try {
            Path tempFile = Files.createTempFile("nexus-upload-", file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            documentService.ingestDocument(tempFile);
            Files.deleteIfExists(tempFile);
            return ResponseEntity.ok("Document processed and indexed: " + file.getOriginalFilename());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/chat")
    public SseEmitter chatStream(@RequestParam("query") String query, 
                                 @RequestParam(value = "model", defaultValue = "openai") String model) {
        long startTime = System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(180000L);

        try {
            assistantAgent.streamAnswer(query)
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
                        // Send citations as a final event
                        List<Map<String, String>> sources = RetrievalContextHolder.getSources();
                        String citationsJson = objectMapper.writeValueAsString(Map.of("type", "citations", "content", sources));
                        emitter.send(SseEmitter.event().data(citationsJson));

                        // Audit Logging
                        AuditLog log = new AuditLog();
                        log.setUserQuery(query);
                        log.setBotResponse((response != null && response.content() != null) ? response.content().text() : "");
                        log.setProcessTimeMs(System.currentTimeMillis() - startTime);
                        log.setRetrievalScore(RetrievalContextHolder.getMaxScore());
                        auditLogRepository.save(log);

                        RetrievalContextHolder.clear();
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .onError(error -> {
                    try {
                        String errorJson = objectMapper.writeValueAsString(Map.of("type", "error", "content", error.getMessage()));
                        emitter.send(SseEmitter.event().data(errorJson));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .start();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @GetMapping("/history")
    public ResponseEntity<List<AuditLog>> getChatHistory() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }
}
