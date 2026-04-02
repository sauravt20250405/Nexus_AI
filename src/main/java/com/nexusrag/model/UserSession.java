package com.nexusrag.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Data
public class UserSession {

    @Id
    private String id; // UUID or String session ID

    @Column(nullable = false)
    private String userId;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastActivity = LocalDateTime.now();

    private boolean active = true;

    // JSON metadata can be stored as a String or using specific JSON converters
    @Column(columnDefinition = "JSON")
    private String metadata;
}
