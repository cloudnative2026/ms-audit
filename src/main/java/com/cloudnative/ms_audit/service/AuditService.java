package com.cloudnative.ms_audit.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.cloudnative.ms_audit.dto.*;
import com.cloudnative.ms_audit.model.AuditEvent;
import com.cloudnative.ms_audit.repository.AuditEventRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditEventRepository events;

    public AuditService(AuditEventRepository events) {
        this.events = events;
    }

    @Transactional
    public void record(AuditEntryMessage message) {
        if (message.eventId() == null || message.eventType() == null || message.entityType() == null
                || message.entityId() == null || message.occurredAt() == null) {
            throw new IllegalArgumentException("Registro de auditoria incompleto: " + message);
        }
        if (events.existsById(message.eventId())) return;
        var event = new AuditEvent();
        event.setId(message.eventId());
        event.setEventType(message.eventType());
        event.setEntityType(message.entityType());
        event.setEntityId(message.entityId());
        event.setActorSubject(message.actorSubject());
        event.setActorName(cut(message.actorName(), 255));
        event.setActorRoles(message.actorRoles() == null ? null : cut(String.join(",", message.actorRoles()), 255));
        event.setOccurredAt(message.occurredAt());
        event.setSourceIp(cut(message.sourceIp(), 64));
        event.setUserAgent(cut(message.userAgent(), 512));
        event.setSourceService(cut(message.sourceService(), 64));
        event.setDetail(cut(message.detail(), 512));
        event.setReceivedAt(Instant.now());
        events.save(event);
    }

    @Transactional(readOnly = true)
    public AuditPageDTO search(String user, String type, Long orderId, Instant from, Instant to, int page, int size) {
        Specification<AuditEvent> spec = (root, query, cb) -> {
            var where = new ArrayList<Predicate>();
            if (user != null && !user.isBlank()) where.add(cb.or(cb.equal(root.get("actorSubject"), user.trim()),
                    cb.like(cb.lower(root.get("actorName")), "%" + user.trim().toLowerCase() + "%")));
            if (type != null && !type.isBlank()) where.add(cb.equal(root.get("eventType"), type));
            if (orderId != null) {
                where.add(cb.equal(root.get("entityType"), "ORDER"));
                where.add(cb.equal(root.get("entityId"), orderId.toString()));
            }
            if (from != null) where.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) where.add(cb.lessThan(root.get("occurredAt"), to));
            return cb.and(where.toArray(Predicate[]::new));
        };
        var result = events.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
        return new AuditPageDTO(result.map(this::dto).getContent(), result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public List<String> eventTypes() {
        return events.findEventTypes();
    }

    private AuditEventDTO dto(AuditEvent e) {
        var roles = e.getActorRoles() == null || e.getActorRoles().isEmpty() ? List.<String>of()
                : Arrays.asList(e.getActorRoles().split(","));
        return new AuditEventDTO(e.getId(), e.getEventType(), e.getEntityType(), e.getEntityId(), e.getActorSubject(),
                e.getActorName(), roles, e.getOccurredAt(), e.getSourceIp(), e.getUserAgent(), e.getSourceService(),
                e.getDetail());
    }

    private static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
