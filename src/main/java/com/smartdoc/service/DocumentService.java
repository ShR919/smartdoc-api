package com.smartdoc.service;

import com.smartdoc.dto.DocumentDtos.*;
import com.smartdoc.exception.DocumentNotFoundException;
import com.smartdoc.exception.DocumentProcessingException;
import com.smartdoc.exception.UnsupportedFileTypeException;
import com.smartdoc.model.Document;
import com.smartdoc.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final TextExtractionService textExtractionService;
    private final VectorStore vectorStore;

    @Value("${smartdoc.chunk.size:1000}")
    private int chunkSize;

    @Value("${smartdoc.chunk.overlap:200}")
    private int chunkOverlap;

    @Value("${smartdoc.upload.allowed-types}")
    private List<String> allowedContentTypes;

    /**
     * Step 1: Persist the document record and kick off async processing.
     */
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file) {
        String contentType = textExtractionService.detectContentType(file);
        validateFileType(contentType);

        Document document = Document.builder()
                .fileName(UUID.randomUUID() + "_" + file.getOriginalFilename())
                .originalFileName(file.getOriginalFilename())
                .contentType(contentType)
                .fileSize(file.getSize())
                .status(Document.DocumentStatus.UPLOADED)
                .build();

        document = documentRepository.save(document);
        log.info("Document uploaded: {} (id={})", file.getOriginalFilename(), document.getId());

        // Trigger async indexing
        processDocumentAsync(document.getId(), file);

        return toResponse(document);
    }

    /**
     * Step 2: Extract text → chunk → embed → store in pgvector (runs in background).
     */
    @Async
    public void processDocumentAsync(UUID documentId, MultipartFile file) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        try {
            document.setStatus(Document.DocumentStatus.PROCESSING);
            documentRepository.save(document);

            // Extract plain text
            String text = textExtractionService.extractText(file);
            document.setExtractedText(text);

            // Chunk the text
            String[] chunks = textExtractionService.chunkText(text, chunkSize, chunkOverlap);
            document.setChunkCount(chunks.length);

            // Build Spring AI documents with metadata and store embeddings
            List<org.springframework.ai.document.Document> aiDocs = new ArrayList<>();
            for (int i = 0; i < chunks.length; i++) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("documentId", documentId.toString());
                metadata.put("documentName", document.getOriginalFileName());
                metadata.put("chunkIndex", i);

                aiDocs.add(new org.springframework.ai.document.Document(chunks[i], metadata));
            }

            vectorStore.add(aiDocs);

            document.setStatus(Document.DocumentStatus.READY);
            log.info("Document indexed: {} chunks for document {}", chunks.length, documentId);

        } catch (Exception e) {
            log.error("Failed to process document {}: {}", documentId, e.getMessage(), e);
            document.setStatus(Document.DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
        }

        documentRepository.save(document);
    }

    public DocumentResponse getDocument(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteDocument(UUID id) {
        Document document = findOrThrow(id);

        // Remove from vector store by metadata filter
        vectorStore.delete(List.of("documentId == '" + id + "'"));

        documentRepository.delete(document);
        log.info("Document deleted: {}", id);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Document findOrThrow(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    private void validateFileType(String contentType) {
        if (!allowedContentTypes.contains(contentType)) {
            throw new UnsupportedFileTypeException(contentType, allowedContentTypes);
        }
    }

    public DocumentResponse toResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .originalFileName(doc.getOriginalFileName())
                .contentType(doc.getContentType())
                .fileSize(doc.getFileSize())
                .status(doc.getStatus())
                .chunkCount(doc.getChunkCount())
                .errorMessage(doc.getErrorMessage())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
