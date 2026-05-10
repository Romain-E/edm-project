package com.romain.docmanager.service;

import com.romain.docmanager.event.DocumentEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.document-uploaded}")
    private String documentUploadedTopic;

    @Value("${kafka.topics.document-deleted}")
    private String documentDeletedTopic;

    public void publishDocumentUploaded(DocumentEvents.DocumentUploadedEvent event) {
        kafkaTemplate.send(documentUploadedTopic, event.getDocumentId().toString(), event);
        log.info("Published DocumentUploadedEvent for document {}", event.getDocumentId());
    }

    public void publishDocumentDeleted(DocumentEvents.DocumentDeletedEvent event) {
        kafkaTemplate.send(documentDeletedTopic, event.getDocumentId().toString(), event);
        log.info("Published DocumentDeletedEvent for document {}", event.getDocumentId());
    }
}
