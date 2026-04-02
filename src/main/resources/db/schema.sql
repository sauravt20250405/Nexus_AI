-- Nexus RAG Database Schema

CREATE DATABASE IF NOT EXISTS nexus_rag;
USE nexus_rag;

-- User Sessions: Tracks unique continuous multi-turn conversations.
CREATE TABLE IF NOT EXISTS user_sessions (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    metadata JSON -- Flexible storage for user-specific context
);

-- Audit Logs: Records bot responses, tool calls, and retrieval metrics.
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255),
    user_query TEXT NOT NULL,
    bot_response TEXT NOT NULL,
    retrieval_score DOUBLE, -- Re-ranking score for verification
    source_documents JSON, -- List of [FileName, Page] cited
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    process_time_ms LONG, -- Time taken for agentic reasoning
    FOREIGN KEY (session_id) REFERENCES user_sessions(id) ON DELETE SET NULL
);

-- Indexing for performance
CREATE INDEX idx_session_user ON user_sessions(user_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp desc);
