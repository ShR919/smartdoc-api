package com.smartdoc.controller;

import com.smartdoc.dto.DocumentDtos.*;
import com.smartdoc.model.Document;
import com.smartdoc.repository.DocumentRepository;
import com.smartdoc.service.DocumentService;
import com.smartdoc.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Upload and manage documents")
public class DocumentController {

    private final DocumentService documentService;
    private final QueryService queryService;
    private final DocumentRepository documentRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document", description = "Accepts PDF, DOCX, or TXT. Triggers async embedding pipeline.")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentResponse upload(
            @Parameter(description = "Document file to upload")
            @RequestParam("file") MultipartFile file) {
        return documentService.uploadDocument(file);
    }

    @GetMapping
    @Operation(summary = "List all documents")
    public List<DocumentResponse> listAll() {
        return documentService.getAllDocuments();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID")
    public DocumentResponse getById(@PathVariable UUID id) {
        return documentService.getDocument(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document and its embeddings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        documentService.deleteDocument(id);
    }

    // ── Q&A ──────────────────────────────────────────────────────────────────

    @PostMapping("/query")
    @Operation(summary = "Ask a question across documents (RAG)",
               description = "Retrieves relevant chunks and generates a grounded answer with sources.")
    public QueryResponse query(@Valid @RequestBody QueryRequest request) {
        return queryService.query(request);
    }

    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Streaming Q&A via Server-Sent Events",
               description = "Same as /query but streams tokens as they are generated.")
    public Flux<String> queryStream(@Valid @RequestBody QueryRequest request) {
        return queryService.queryStream(request);
    }

    // ── Summarization ─────────────────────────────────────────────────────────

    @GetMapping("/{id}/summary")
    @Operation(summary = "Summarize a document",
               description = "Returns a 2-3 sentence summary plus key bullet points.")
    public SummaryResponse summarize(@PathVariable UUID id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new com.smartdoc.exception.DocumentNotFoundException(id));

        if (doc.getStatus() != Document.DocumentStatus.READY) {
            throw new IllegalStateException("Document is not ready yet. Current status: " + doc.getStatus());
        }

        return queryService.summarize(id, doc.getExtractedText(), doc.getOriginalFileName());
    }
}
