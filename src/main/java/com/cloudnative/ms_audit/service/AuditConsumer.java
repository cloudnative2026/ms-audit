package com.cloudnative.ms_audit.service;

import com.cloudnative.ms_audit.config.KafkaConfig;
import com.cloudnative.ms_audit.dto.AuditEntryMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AuditConsumer {
    private final AuditService service;
    private final JsonMapper json;

    public AuditConsumer(AuditService service, JsonMapper json) {
        this.service = service;
        this.json = json;
    }

    // Failures are retried and then sent to the DLT by KafkaConfig's error handler.
    @KafkaListener(topics = KafkaConfig.SOURCE_TOPIC)
    public void consume(String value) {
        service.record(json.readValue(value, AuditEntryMessage.class));
    }
}
