package com.romain.docmanager.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class DocumentEvents {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DocumentUploadedEvent {
        private UUID documentId;
        private String filename;
        private String s3Key;
        private String uploadedBy;
        private Long fileSize;
        private LocalDateTime uploadedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DocumentDeletedEvent {
        private UUID documentId;
        private String filename;
        private String s3Key;
        private String deletedBy;
        private LocalDateTime deletedAt;
    }
}
