package com.cloudnative.ms_audit;

import java.time.Instant;
import java.util.List;
import com.cloudnative.ms_audit.dto.AuditEntryMessage;
import com.cloudnative.ms_audit.repository.AuditEventRepository;
import com.cloudnative.ms_audit.service.AuditService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.kafka.listener.auto-startup=false", "spring.kafka.admin.auto-create=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditApiTests {
    @Autowired MockMvc mvc;
    @Autowired AuditService service;
    @Autowired AuditEventRepository events;
    static final String URL = "/api/v1/audit/events";

    static RequestPostProcessor role(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    static AuditEntryMessage entry(String id, String type, String orderId, String actor, String at) {
        return new AuditEntryMessage(id, type, "ORDER", orderId, actor + "-sub", actor, List.of("Admin"),
            Instant.parse(at), "203.0.113.7", "JUnit", "ms-orders", "detalle");
    }

    @BeforeEach void setup() {
        events.deleteAll();
        service.record(entry("e1", "OrderCreated", "1", "Ana Admin", "2026-11-01T10:00:00Z"));
        service.record(entry("e2", "OrderAccepted", "1", "Oscar Operador", "2026-11-02T10:00:00Z"));
        service.record(entry("e3", "OrderCreated", "2", "Ana Admin", "2026-11-03T10:00:00Z"));
    }

    @Test void redeliveredEventsAreStoredOnce() {
        service.record(entry("e1", "OrderCreated", "1", "Ana Admin", "2026-11-01T10:00:00Z"));
        assertThat(events.count()).isEqualTo(3);
    }

    @Test void incompleteEventsAreRejected() {
        assertThatThrownBy(() -> service.record(entry(null, "OrderCreated", "1", "Ana", "2026-11-01T10:00:00Z")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void newestFirstWithFilters() throws Exception {
        mvc.perform(get(URL).with(role("Admin"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[*].eventId", contains("e3", "e2", "e1")))
            .andExpect(jsonPath("$.content[0].actorRoles[0]").value("Admin"))
            .andExpect(jsonPath("$.content[0].sourceIp").value("203.0.113.7"));
        mvc.perform(get(URL).param("user", "ana").with(role("Admin")))
            .andExpect(jsonPath("$.content[*].eventId", contains("e3", "e1")));
        mvc.perform(get(URL).param("user", "Oscar Operador-sub").with(role("Admin")))
            .andExpect(jsonPath("$.content[*].eventId", contains("e2")));
        mvc.perform(get(URL).param("type", "OrderCreated").with(role("Admin")))
            .andExpect(jsonPath("$.content[*].eventId", contains("e3", "e1")));
        mvc.perform(get(URL).param("orderId", "1").with(role("Admin")))
            .andExpect(jsonPath("$.content[*].eventId", contains("e2", "e1")));
        mvc.perform(get(URL).param("from", "2026-11-02T00:00:00Z").param("to", "2026-11-03T00:00:00Z").with(role("Admin")))
            .andExpect(jsonPath("$.content[*].eventId", contains("e2")));
        mvc.perform(get(URL).param("size", "2").param("page", "1").with(role("Admin")))
            .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.content[*].eventId", contains("e1")));
        mvc.perform(get("/api/v1/audit/event-types").with(role("Admin")))
            .andExpect(jsonPath("$", contains("OrderAccepted", "OrderCreated")));
    }

    @Test void invalidQueriesAreRejected() throws Exception {
        mvc.perform(get(URL).param("size", "500").with(role("Admin"))).andExpect(status().isBadRequest());
        mvc.perform(get(URL).param("from", "2026-11-03T00:00:00Z").param("to", "2026-11-02T00:00:00Z").with(role("Admin")))
            .andExpect(status().isBadRequest());
        mvc.perform(get(URL).param("from", "ayer").with(role("Admin"))).andExpect(status().isBadRequest());
    }

    @Test void onlyAdminsCanRead() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(get(URL).with(role("Operador"))).andExpect(status().isForbidden());
        mvc.perform(get(URL).with(role("Cliente"))).andExpect(status().isForbidden());
    }
}
