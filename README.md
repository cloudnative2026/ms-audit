# ms-audit

Auditoria / timeline de Pedidos360. Java 21 / Spring Boot, PostgreSQL y Liquibase.

Consume `audit.timeline` (publicado por ms-orders) y guarda quien / que / cuando / desde donde.
Tras 3 reintentos el mensaje va a `audit.timeline.ms-audit.DLT` con los headers de error
(excepcion, stack trace, topic/particion/offset original, consumer group). Los mensajes
repetidos se ignoran por `eventId`.

## Ejecutar

```sh
docker compose up -d          # PostgreSQL en 5434
./mvnw spring-boot:run        # Kafka en localhost:9092 (compose general)
```

Configuracion: DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD, SPRING_KAFKA_BOOTSTRAP_SERVERS,
SERVER_PORT, ENTRA_ISSUER, ENTRA_AUDIENCE.

## API (solo lectura, rol Admin)

| Metodo | Ruta |
| --- | --- |
| GET | /api/v1/audit/events?user=&type=&orderId=&from=&to=&page=0&size=50 |
| GET | /api/v1/audit/event-types |

`from`/`to` son instantes ISO-8601 (`to` exclusivo). `user` busca por nombre (contiene) o subject exacto.
Orden: mas reciente primero. Swagger: /swagger-ui/index.html
