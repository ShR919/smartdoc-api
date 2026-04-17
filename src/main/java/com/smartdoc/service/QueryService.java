package com.smartdoc.service;

import com.smartdoc.dto.DocumentDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("${smartdoc.retrieval.top-k:5}")
    private int topK;

    @Value("${smartdoc.retrieval.similarity-threshold:0.75}")
    private double similarityThreshold;

    private static final String RAG_SYSTEM_PROMPT = """
            You are SmartDoc, an intelligent document assistant.
            Answer questions based ONLY on the provided document context below.
            If the answer is not found in the context, say "I couldn't find this information in the provided documents."
            Always be concise, accurate, and cite which part of the document supports your answer.
            
            Context:
            {context}
            """;

    private static final String SUMMARY_PROMPT = """
            You are a document summarization expert.
            Provide a clear, structured summary of the following document content.
            
            Format your response as:
            SUMMARY: (2-3 sentence overview)
            
            KEY POINTS:
            - Point 1
            - Point 2
            - (up to 7 key points)
            
            Document content:
            {content}
            """;

    /**
     * RAG-powered Q&A — retrieves relevant chunks then asks the LLM.
     */
    public QueryResponse query(QueryRequest request) {
        long start = System.currentTimeMillis();

        // 1. Retrieve similar chunks from vector store
        List<org.springframework.ai.document.Document> relevantDocs = retrieveRelevantChunks(
                request.getQuestion(), request.getDocumentIds());

        if (relevantDocs.isEmpty()) {
            return QueryResponse.builder()
                    .answer("No relevant content found in the selected documents for your question.")
                    .sources(List.of())
                    .processingTimeMs(System.currentTimeMillis() - start)
                    .build();
        }

        // 2. Build context string from retrieved chunks
        String context = relevantDocs.stream()
                .map(org.springframework.ai.document.Document::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 3. Ask the LLM with the context injected
        String systemPrompt = RAG_SYSTEM_PROMPT.replace("{context}", context);

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(request.getQuestion())
                .call()
                .content();

        // 4. Map source chunks for transparency
        List<SourceChunk> sources = relevantDocs.stream()
                .map(doc -> SourceChunk.builder()
                        .documentId(UUID.fromString((String) doc.getMetadata().get("documentId")))
                        .documentName((String) doc.getMetadata().get("documentName"))
                        .content(truncate(doc.getContent(), 300))
                        .similarityScore((Double) doc.getMetadata().getOrDefault("distance", 0.0))
                        .build())
                .toList();

        return QueryResponse.builder()
                .answer(answer)
                .sources(sources)
                .processingTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    /**
     * Streaming version of query — returns token-by-token via SSE.
     */
    public Flux<String> queryStream(QueryRequest request) {
        List<org.springframework.ai.document.Document> relevantDocs = retrieveRelevantChunks(
                request.getQuestion(), request.getDocumentIds());

        String context = relevantDocs.stream()
                .map(org.springframework.ai.document.Document::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));

        String systemPrompt = RAG_SYSTEM_PROMPT.replace("{context}", context);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(request.getQuestion())
                .stream()
                .content();
    }

    /**
     * Summarizes a full document using its extracted text.
     */
    public SummaryResponse summarize(UUID documentId, String extractedText, String fileName) {
        long start = System.currentTimeMillis();

        // Truncate to avoid token limits (keep first ~12k chars)
        String content = extractedText.length() > 12000
                ? extractedText.substring(0, 12000) + "\n\n[... document truncated for summary ...]"
                : extractedText;

        String prompt = SUMMARY_PROMPT.replace("{content}", content);

        String rawResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // Parse summary and key points from the response
        String summary = extractSection(rawResponse, "SUMMARY:", "KEY POINTS:");
        List<String> keyPoints = extractBulletPoints(rawResponse, "KEY POINTS:");

        return SummaryResponse.builder()
                .documentId(documentId)
                .fileName(fileName)
                .summary(summary.trim())
                .keyPoints(keyPoints)
                .processingTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<org.springframework.ai.document.Document> retrieveRelevantChunks(
            String query, List<UUID> documentIds) {

        SearchRequest.Builder searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold);

        // Optional: filter by specific document IDs
        if (documentIds != null && !documentIds.isEmpty()) {
            FilterExpressionBuilder filter = new FilterExpressionBuilder();
            List<String> idStrings = documentIds.stream().map(UUID::toString).toList();
            searchRequest.filterExpression(filter.in("documentId", idStrings.toArray()).build());
        }

        return vectorStore.similaritySearch(searchRequest.build());
    }

    private String extractSection(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        if (start == -1) return text;
        start += startMarker.length();

        int end = endMarker != null ? text.indexOf(endMarker, start) : text.length();
        if (end == -1) end = text.length();

        return text.substring(start, end).trim();
    }

    private List<String> extractBulletPoints(String text, String sectionMarker) {
        int start = text.indexOf(sectionMarker);
        if (start == -1) return List.of();

        String section = text.substring(start + sectionMarker.length());
        List<String> points = new ArrayList<>();

        for (String line : section.split("\n")) {
            line = line.trim();
            if (line.startsWith("-") || line.startsWith("•") || line.startsWith("*")) {
                points.add(line.replaceFirst("^[-•*]\\s*", "").trim());
            }
        }
        return points;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
