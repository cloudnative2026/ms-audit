package com.cloudnative.ms_audit;

import java.time.Duration;
import java.util.Map;
import com.cloudnative.ms_audit.config.KafkaConfig;
import com.cloudnative.ms_audit.repository.AuditEventRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.datasource.url=jdbc:h2:mem:audit-kafka;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
@EmbeddedKafka(partitions = 3, topics = KafkaConfig.SOURCE_TOPIC)
@ActiveProfiles("test")
class AuditKafkaTests {
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired AuditEventRepository events;
    @Autowired EmbeddedKafkaBroker broker;

    @Test void validRecordsArePersistedAndMalformedOnesGoToTheDeadLetterTopic() throws Exception {
        kafka.send(KafkaConfig.SOURCE_TOPIC, "k1", """
            {"eventId":"k1","eventType":"OrderCreated","entityType":"ORDER","entityId":"7",
             "actorSubject":"sub","actorName":"Ana","actorRoles":["Admin"],"occurredAt":"2026-11-01T10:00:00Z",
             "sourceIp":"10.0.0.1","userAgent":"JUnit","sourceService":"ms-orders","detail":"Pedido creado"}
            """).get();
        long deadline = System.currentTimeMillis() + 15_000;
        while (!events.existsById("k1") && System.currentTimeMillis() < deadline) Thread.sleep(100);
        assertThat(events.findById("k1")).hasValueSatisfying(e -> assertThat(e.getEntityId()).isEqualTo("7"));

        kafka.send(KafkaConfig.SOURCE_TOPIC, "bad", "{not json").get();
        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "dlt-reader", false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (var consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
                .createConsumer()) {
            consumer.subscribe(java.util.List.of(KafkaConfig.DEAD_LETTER_TOPIC));
            var record = KafkaTestUtils.getSingleRecord(consumer, KafkaConfig.DEAD_LETTER_TOPIC, Duration.ofSeconds(15));
            assertThat(record.value()).isEqualTo("{not json");
            assertThat(record.headers().lastHeader("kafka_dlt-exception-stacktrace")).isNotNull();
            assertThat(new String(record.headers().lastHeader("kafka_dlt-original-consumer-group").value())).isEqualTo("ms-audit");
        }
    }
}
