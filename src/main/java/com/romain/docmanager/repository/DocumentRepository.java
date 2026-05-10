package com.romain.docmanager.repository;

import com.romain.docmanager.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByUploadedByAndStatusNot(String uploadedBy, Document.DocumentStatus status, Pageable pageable);

    Page<Document> findByStatusNot(Document.DocumentStatus status, Pageable pageable);

    Optional<Document> findByIdAndUploadedBy(UUID id, String uploadedBy);
}
