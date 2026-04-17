package com.smartdoc.dto;

import com.smartdoc.model.Document;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class DocumentDtos {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentResponse {
        private UUID id;
        private String fileName;
        private String originalFileName;
        private String contentType;
        private Long fileSize;
        private Document.DocumentStatus status;
        private Integer chunkCount;
        private String errorMessage;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryRequest {
        private String question;
        private List<UUID> documentIds; // optional filter; null = search all
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryResponse {
        private String answer;
        private List<SourceChunk> sources;
        private long processingTimeMs;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceChunk {
        private UUID documentId;
        private String documentName;
        private String content;
        private double similarityScore;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryResponse {
        private UUID documentId;
        private String fileName;
        private String summary;
        private List<String> keyPoints;
        private long processingTimeMs;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractionRequest {
        private String schema; // JSON schema describing fields to extract
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractionResponse {
        private UUID documentId;
        private String fileName;
        private Object extractedData; // dynamic JSON
        private long processingTimeMs;
    }
}
