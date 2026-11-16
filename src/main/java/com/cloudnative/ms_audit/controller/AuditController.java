package com.cloudnative.ms_audit.controller;

import java.time.Instant;
import java.util.List;
import com.cloudnative.ms_audit.dto.AuditPageDTO;
import com.cloudnative.ms_audit.service.AuditService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Read-only: events only enter through Kafka.
@RestController
@RequestMapping("/api/v1/audit")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin')")
public class AuditController {
    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping("/events")
    public AuditPageDTO events(@RequestParam(required = false) String user,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1 || size > 200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page >= 0 y size entre 1 y 200.");
        if (from != null && to != null && !from.isBefore(to))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from debe ser anterior a to.");
        return service.search(user, type, orderId, from, to, page, size);
    }

    @GetMapping("/event-types")
    public List<String> eventTypes() {
        return service.eventTypes();
    }
}
