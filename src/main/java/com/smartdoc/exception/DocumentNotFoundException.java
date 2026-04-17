package com.smartdoc.exception;

import java.util.List;
import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(UUID id) {
        super("Document not found: " + id);
    }
}
