package com.romain.docmanager;

import com.romain.docmanager.model.Document;
import com.romain.docmanager.repository.DocumentRepository;
import com.romain.docmanager.service.DocumentService;
import com.romain.docmanager.service.KafkaProducerService;
import com.romain.docmanager.service.S3Service;
import com.romain.docmanager.dto.Dtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private S3Service s3Service;
    @Mock private KafkaProducerService kafkaProducer;

    @InjectMocks private DocumentService documentService;

    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        mockFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "PDF content".getBytes()
        );
    }

    @Test
    void uploadDocument_shouldSaveAndPublishEvent() throws IOException {
        String s3Key = "documents/user/uuid_test.pdf";
        when(s3Service.uploadFile(any(), any(), any())).thenReturn(s3Key);

        Document saved = Document.builder()
                .id(UUID.randomUUID())
                .filename("uuid_test.pdf")
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .fileSize(11L)
                .s3Key(s3Key)
                .uploadedBy("testuser")
                .status(Document.DocumentStatus.PENDING)
                .build();

        when(documentRepository.save(any())).thenReturn(saved);

        Dtos.DocumentResponse response = documentService.uploadDocument(mockFile, "testuser");

        assertThat(response).isNotNull();
        assertThat(response.getUploadedBy()).isEqualTo("testuser");
        assertThat(response.getStatus()).isEqualTo(Document.DocumentStatus.PENDING);

        verify(s3Service).uploadFile(eq(mockFile), eq("testuser"), anyString());
        verify(documentRepository).save(any(Document.class));
        verify(kafkaProducer).publishDocumentUploaded(any());
    }

    @Test
    void deleteDocument_shouldMarkDeletedAndPublishEvent() {
        UUID docId = UUID.randomUUID();
        Document doc = Document.builder()
                .id(docId)
                .filename("test.pdf")
                .originalFilename("test.pdf")
                .s3Key("documents/user/test.pdf")
                .uploadedBy("testuser")
                .status(Document.DocumentStatus.PENDING)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any())).thenReturn(doc);
        doNothing().when(s3Service).deleteFile(any());

        documentService.deleteDocument(docId, "testuser", false);

        assertThat(doc.getStatus()).isEqualTo(Document.DocumentStatus.DELETED);
        verify(s3Service).deleteFile(doc.getS3Key());
        verify(kafkaProducer).publishDocumentDeleted(any());
    }

    @Test
    void getDocument_shouldThrowWhenUserNotOwner() {
        UUID docId = UUID.randomUUID();
        Document doc = Document.builder()
                .id(docId)
                .uploadedBy("anotheruser")
                .status(Document.DocumentStatus.PENDING)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.getDocument(docId, "testuser", false))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
