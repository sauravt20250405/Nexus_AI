package com.nexusrag.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = true)
    private UserSession session;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userQuery;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String botResponse;

    private Double retrievalScore;

    @Column(columnDefinition = "JSON")
    private String sourceDocuments; // Store grounded citations as JSON

    private LocalDateTime timestamp = LocalDateTime.now();

    private Long processTimeMs; // Benchmarking for agentic latency
}
