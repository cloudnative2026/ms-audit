package com.cloudnative.ms_audit.dto;

import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Record published by ms-orders to audit.timeline.
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuditEntryMessage(String eventId, String eventType, String entityType, String entityId,
        String actorSubject, String actorName, List<String> actorRoles, Instant occurredAt,
        String sourceIp, String userAgent, String sourceService, String detail) {}
