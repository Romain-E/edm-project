package com.romain.docmanager.service;

import com.romain.docmanager.dto.Dtos;
import com.romain.docmanager.event.DocumentEvents;
import com.romain.docmanager.model.Document;
import com.romain.docmanager.repository.DocumentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final S3Service s3Service;
    private final KafkaProducerService kafkaProducer;

    @Transactional
    public Dtos.DocumentResponse uploadDocument(MultipartFile file, String username) throws IOException {
        String generatedFilename = generateFilename(file.getOriginalFilename());
        String s3Key = s3Service.uploadFile(file, username, generatedFilename);

        Document document = Document.builder()
                .filename(generatedFilename)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .s3Key(s3Key)
                .uploadedBy(username)
                .status(Document.DocumentStatus.PENDING)
                .build();

        Document saved = documentRepository.save(document);

        kafkaProducer.publishDocumentUploaded(DocumentEvents.DocumentUploadedEvent.builder()
                .documentId(saved.getId())
                .filename(saved.getFilename())
                .s3Key(s3Key)
                .uploadedBy(username)
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .build());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Dtos.PagedResponse<Dtos.DocumentResponse> listDocuments(String username, boolean isAdmin, Pageable pageable) {
        Page<Document> page = isAdmin
                ? documentRepository.findByStatusNot(Document.DocumentStatus.DELETED, pageable)
                : documentRepository.findByUploadedByAndStatusNot(username, Document.DocumentStatus.DELETED, pageable);

        return Dtos.PagedResponse.<Dtos.DocumentResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public Dtos.DocumentResponse getDocument(UUID id, String username, boolean isAdmin) {
        Document document = findAccessibleDocument(id, username, isAdmin);
        String downloadUrl = s3Service.generatePresignedUrl(document.getS3Key());
        Dtos.DocumentResponse response = toResponse(document);
        response.setDownloadUrl(downloadUrl);
        return response;
    }

    @Transactional
    public void deleteDocument(UUID id, String username, boolean isAdmin) {
        Document document = findAccessibleDocument(id, username, isAdmin);

        s3Service.deleteFile(document.getS3Key());
        document.setStatus(Document.DocumentStatus.DELETED);
        documentRepository.save(document);

        kafkaProducer.publishDocumentDeleted(DocumentEvents.DocumentDeletedEvent.builder()
                .documentId(document.getId())
                .filename(document.getFilename())
                .s3Key(document.getS3Key())
                .deletedBy(username)
                .deletedAt(LocalDateTime.now())
                .build());
    }

    private Document findAccessibleDocument(UUID id, String username, boolean isAdmin) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + id));

        if (document.getStatus() == Document.DocumentStatus.DELETED) {
            throw new EntityNotFoundException("Document not found: " + id);
        }

        if (!isAdmin && !document.getUploadedBy().equals(username)) {
            throw new AccessDeniedException("Access denied to document: " + id);
        }

        return document;
    }

    private Dtos.DocumentResponse toResponse(Document doc) {
        return Dtos.DocumentResponse.builder()
                .id(doc.getId())
                .filename(doc.getFilename())
                .originalFilename(doc.getOriginalFilename())
                .contentType(doc.getContentType())
                .fileSize(doc.getFileSize())
                .uploadedBy(doc.getUploadedBy())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    private String generateFilename(String original) {
        return UUID.randomUUID() + "_" + original;
    }
}
