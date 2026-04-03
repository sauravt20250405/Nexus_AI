package com.nexusrag.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
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

    @Column(columnDefinition = "TEXT")
    private String sourceDocuments;

    private LocalDateTime timestamp = LocalDateTime.now();

    private Long processTimeMs;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserSession getSession() { return session; }
    public void setSession(UserSession session) { this.session = session; }

    public String getUserQuery() { return userQuery; }
    public void setUserQuery(String userQuery) { this.userQuery = userQuery; }

    public String getBotResponse() { return botResponse; }
    public void setBotResponse(String botResponse) { this.botResponse = botResponse; }

    public Double getRetrievalScore() { return retrievalScore; }
    public void setRetrievalScore(Double retrievalScore) { this.retrievalScore = retrievalScore; }

    public String getSourceDocuments() { return sourceDocuments; }
    public void setSourceDocuments(String sourceDocuments) { this.sourceDocuments = sourceDocuments; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getProcessTimeMs() { return processTimeMs; }
    public void setProcessTimeMs(Long processTimeMs) { this.processTimeMs = processTimeMs; }
}
