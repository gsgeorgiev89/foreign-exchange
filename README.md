# FX Conversion Service

A Spring Boot service for foreign-exchange rate lookup and currency conversion
against per-client account balances.

## How to run

### Option 1 — Docker Compose (recommended)
```bash
docker compose up --build
```
The service starts on http://localhost:8080

### Option 2 — Local (requires PostgreSQL running on localhost:5432)
```bash
./mvnw spring-boot:run
```

## Demo clients

Seeded via Flyway migration `V2__seed_data.sql`:

| Client ID   | Currency | Balance    |
|-------------|----------|------------|
| CLIENT-001  | USD      | 10,000.00  |
| CLIENT-001  | EUR      | 8,000.00   |
| CLIENT-002  | GBP      | 5,000.00   |

## API endpoints

| Method | Path                          | Description                    |
|--------|-------------------------------|--------------------------------|
| GET    | /rates?from=USD&to=EUR        | Get current exchange rate      |
| POST   | /conversions                  | Convert currency for a client  |
| GET    | /conversions?clientId=...     | Get paginated conversion history|
| GET    | /clients/{clientId}/balances  | Get client balances            |

Swagger UI: http://localhost:8080/swagger-ui.html

## Example conversion request

```bash
curl -X POST http://localhost:8080/conversions \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "clientId": "CLIENT-001",
    "fromCurrency": "USD",
    "toCurrency": "EUR",
    "amount": 100.00
  }'
```

## Design decisions

### Concurrency strategy — Pessimistic locking
`SELECT FOR UPDATE` on the balance row before debit/credit.
Chosen over optimistic locking because financial operations have high
contention and retry logic on `OptimisticLockException` adds complexity
with no benefit here. Two simultaneous conversions for the same client
queue up at the database level — no double-spend possible.

### Idempotency
The client passes an `Idempotency-Key` header. On first request the key
is persisted with `COMPLETED` status after the conversion succeeds — both
in the same `@Transactional` block. On replay the original response is
returned without touching balances. The UUID for the conversion is
generated before the transaction starts so the foreign key from
`idempotency_keys` to `conversions` is always valid at commit time.

### Rate caching
Exchange rates are cached in Caffeine with a 60-second TTL. This balances
freshness against provider rate limits. With more time I would expose a
cache eviction endpoint and add a configurable TTL property.

### Schema migrations
Flyway manages all DDL. `ddl-auto` is set to `validate` — Hibernate
checks the schema matches entities but never modifies it.

### What I would do with more time
- Testcontainers for integration tests (isolated PostgreSQL per test run)
- Redis for distributed idempotency across multiple service instances
- PROCESSING status for idempotency keys using `REQUIRES_NEW` transaction
- Deposit/withdraw endpoints
- Metrics and alerting via Micrometer + Prometheus