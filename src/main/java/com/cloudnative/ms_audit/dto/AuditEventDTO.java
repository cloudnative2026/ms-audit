package com.cloudnative.ms_audit.dto;

import java.time.Instant;
import java.util.List;

public record AuditEventDTO(String eventId, String eventType, String entityType, String entityId,
        String actorSubject, String actorName, List<String> actorRoles, Instant occurredAt,
        String sourceIp, String userAgent, String sourceService, String detail) {}
