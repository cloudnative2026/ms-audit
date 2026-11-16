package com.cloudnative.ms_audit.repository;

import java.util.List;
import com.cloudnative.ms_audit.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String>, JpaSpecificationExecutor<AuditEvent> {
    @Query("select distinct e.eventType from AuditEvent e order by e.eventType")
    List<String> findEventTypes();
}
