package com.nexusrag.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
public class UserSession {

    @Id
    private String id;

    @Column(nullable = false)
    private String userId;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastActivity = LocalDateTime.now();

    private boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
