package com.smartdoc.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class TextExtractionService {

    private final Tika tika = new Tika();

    /**
     * Detects the content type of an uploaded file.
     */
    public String detectContentType(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return tika.detect(is, file.getOriginalFilename());
        } catch (IOException e) {
            log.warn("Could not detect content type, falling back to declared: {}", file.getContentType());
            return file.getContentType();
        }
    }

    /**
     * Extracts plain text from any supported document format.
     * Supports PDF, DOCX, TXT, and more via Tika.
     */
    public String extractText(MultipartFile file) throws IOException, TikaException, SAXException {
        log.debug("Extracting text from: {}", file.getOriginalFilename());

        // Use a large buffer limit (-1 = unlimited)
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        AutoDetectParser parser = new AutoDetectParser();

        try (InputStream stream = file.getInputStream()) {
            parser.parse(stream, handler, metadata);
        }

        String text = handler.toString().trim();
        log.debug("Extracted {} characters from {}", text.length(), file.getOriginalFilename());
        return text;
    }

    /**
     * Splits text into overlapping chunks for embedding.
     */
    public String[] chunkText(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }

        // Split by paragraphs first for natural boundaries
        String[] paragraphs = text.split("\n\n+");
        java.util.List<String> chunks = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isBlank()) continue;

            if (current.length() + paragraph.length() > chunkSize && current.length() > 0) {
                chunks.add(current.toString().trim());

                // Keep overlap: retain last `overlap` characters
                String currentStr = current.toString();
                int start = Math.max(0, currentStr.length() - overlap);
                current = new StringBuilder(currentStr.substring(start));
            }
            current.append(paragraph).append("\n\n");
        }

        if (!current.toString().isBlank()) {
            chunks.add(current.toString().trim());
        }

        log.debug("Split text into {} chunks (size={}, overlap={})", chunks.size(), chunkSize, overlap);
        return chunks.toArray(new String[0]);
    }
}
