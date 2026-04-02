package com.nexusrag.controller;

import com.nexusrag.agent.AssistantAgent;
import com.nexusrag.model.AuditLog;
import com.nexusrag.repository.AuditLogRepository;
import com.nexusrag.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/api/nexus")
public class NexusRagController {

    private final AssistantAgent assistantAgent;
    private final DocumentService documentService;
    private final AuditLogRepository auditLogRepository;

    public NexusRagController(AssistantAgent assistantAgent, 
                              DocumentService documentService,
                              AuditLogRepository auditLogRepository) {
        this.assistantAgent = assistantAgent;
        this.documentService = documentService;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Ingest a PDF document into the vector store.
     */
    @PostMapping("/ingest")
    public ResponseEntity<String> ingestDocument(@RequestParam("file") MultipartFile file) {
        try {
            Path tempFile = Files.createTempFile("nexus-upload-", file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            documentService.ingestDocument(tempFile);
            
            Files.deleteIfExists(tempFile);
            return ResponseEntity.ok("Document processed and indexed successfully: " + file.getOriginalFilename());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error processing document: " + e.getMessage());
        }
    }

    /**
     * Chat with the Nexus RAG Assistant with Audit Logging.
     */
    @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam("query") String query) {
        long startTime = System.currentTimeMillis();
        try {
            String response = assistantAgent.answer(query);
            
            // Audit Logging
            AuditLog log = new AuditLog();
            log.setUserQuery(query);
            log.setBotResponse(response);
            log.setProcessTimeMs(System.currentTimeMillis() - startTime);
            auditLogRepository.save(log);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Assistant Error: " + e.getMessage());
        }
    }
}
