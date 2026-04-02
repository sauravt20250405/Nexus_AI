package com.nexusrag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.recursive.RecursiveCharacterSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

@Service
public class DocumentService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public DocumentService(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    /**
     * Ingests a PDF document from a file path.
     * Implements recursive splitting (600 tokens, 15% overlap) and Apache Tika parsing.
     */
    public void ingestDocument(Path filePath) {
        DocumentParser parser = new ApacheTikaDocumentParser();
        Document document = loadDocument(filePath, parser);

        // Metadata Enrichment: Ensure source and timestamp are added to all segments
        Map<String, Object> baseMetadata = new HashMap<>();
        baseMetadata.put("source_name", filePath.getFileName().toString());
        baseMetadata.put("ingestion_timestamp", LocalDateTime.now().toString());
        
        // Recursive Character Splitter: 600 tokens, 15% (90 tokens) overlap
        DocumentSplitter splitter = RecursiveCharacterSplitter.builder()
                .chunkSize(600)
                .chunkOverlap(90)
                .build();

        // The Ingestor will handle the splitting and embedding store storage
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);
    }
}
