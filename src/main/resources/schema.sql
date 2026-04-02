-- Create Database if not exists
CREATE DATABASE IF NOT EXISTS nexus_rag;
USE nexus_rag;

-- User Sessions Table
CREATE TABLE IF NOT EXISTS user_sessions (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_activity DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    metadata JSON
);

-- Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255),
    user_query TEXT NOT NULL,
    bot_response TEXT NOT NULL,
    retrieval_score DOUBLE,
    source_documents JSON,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    process_time_ms BIGINT,
    FOREIGN KEY (session_id) REFERENCES user_sessions(id) ON DELETE SET NULL
);
