package com.cloudnative.ms_audit.dto;

import java.util.List;

public record AuditPageDTO(List<AuditEventDTO> content, long totalElements, int page, int size) {}
