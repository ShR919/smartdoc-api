package com.smartdoc.repository;

import com.smartdoc.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByStatus(Document.DocumentStatus status);

    List<Document> findByIdIn(List<UUID> ids);

    boolean existsByOriginalFileNameAndStatus(String originalFileName, Document.DocumentStatus status);
}
