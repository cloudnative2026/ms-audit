package com.cloudnative.ms_audit.model;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "audit_event")
public class AuditEvent {
    // The producer's event id: redelivered records are recognised and skipped.
    @Id
    @Column(length = 36)
    private String id;
    @Column(nullable = false, length = 50)
    private String eventType;
    @Column(nullable = false, length = 30)
    private String entityType;
    @Column(nullable = false, length = 64)
    private String entityId;
    @Column(length = 255)
    private String actorSubject;
    @Column(length = 255)
    private String actorName;
    @Column(length = 255)
    private String actorRoles;
    @Column(nullable = false)
    private Instant occurredAt;
    @Column(length = 64)
    private String sourceIp;
    @Column(length = 512)
    private String userAgent;
    @Column(length = 64)
    private String sourceService;
    @Column(length = 512)
    private String detail;
    @Column(nullable = false)
    private Instant receivedAt;
}
