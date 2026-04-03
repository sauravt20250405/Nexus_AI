package com.nexusrag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentServiceTest {

    @Test
    public void testRecursiveCharacterSplittingLogic() {
        // Arrange: A sample text that is sufficiently long to trigger splitting
        // Using a 50 character limit for a simple reproducible test instead of 600 tokens
        String sampleText = "LangChain4j is an amazing library for building LLM applications in Java. " +
                "It simplifies the integration of AI models. " +
                "This framework allows easy chunking and semantic search capabilities.";

        Document document = new Document(sampleText);
        
        // Split with max 50 chars and 10 overlap
        DocumentSplitter splitter = DocumentSplitters.recursive(50, 10);

        // Act
        List<TextSegment> segments = splitter.split(document);

        // Assert
        assertNotNull(segments);
        assertFalse(segments.isEmpty());
        
        // Ensure that chunks do not exceed the max size
        for (TextSegment segment : segments) {
            assertTrue(segment.text().length() <= 50, "Segment exceeded maximum length limit.");
        }
    }
}
