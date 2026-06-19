# Coffee Shop Backend (async + sharded)

Spring Boot service that accepts bulk payment requests from the coffee-shop client,
stores them durably in PostgreSQL, and asynchronously propagates each payment to the
remote payments system. Payment rows are sharded across multiple PostgreSQL instances
using a static, hash-modulo shard map.

## Architecture overview

```
Client ──POST batch──▶ PaymentBatchController ──▶ coordinator DB
                                                  (batch_request, batch_item)
                       │ 202 Accepted (requestId)
                       ▼
                Client ──GET status──▶ PaymentBatchController ──▶ coordinator DB

                                BatchWorker (@Scheduled)
                                      │
                                      ▼
                          BatchOrchestrator.processNextChunk()
                                      │
                ┌─────────────────────┼──────────────────────┐
                ▼                     ▼                      ▼
        lease items                remote API           ShardResolver
        from coordinator           (idempotent)         + ShardedPaymentRepository
                                                        ─▶ shard 0..N (payments)
```

## API contract

### `POST /api/v1/payment-batches`
Submit a bulk request. Persists the request and items as `PENDING` and returns
immediately so the worker can drain them in the background.

Request body:
```json
{
  "storeId": "store-1",
  "payments": [
    {
      "idempotencyKey": "k-1",
      "coffeeType": "LATTE",
      "price": 4.50,
      "currency": "USD",
      "loyaltyCardId": "loyalty-1"
    }
  ]
}
```

Successful response: `202 Accepted`
```json
{
  "requestId": "8d4e...",
  "status": "PENDING",
  "total": 1
}
```

Validation errors return `400` with an `application/problem+json` body containing
`fieldErrors`.

### `GET /api/v1/payment-batches/{requestId}`
Returns the aggregate status for a request.

```json
{
  "requestId": "8d4e...",
  "status": "PROCESSING",
  "total": 10,
  "processed": 6,
  "succeeded": 5,
  "replayed": 1,
  "failed": 0,
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-01-01T00:00:01Z"
}
```

Statuses: `PENDING → PROCESSING → DONE | FAILED_PARTIAL | FAILED`. `404` is returned
when the request id does not exist.

## Data model

Coordinator DB (`coffee_coordinator`):
- `batch_request` — request metadata + counters (totals/processed/succeeded/replayed/failed).
- `batch_item` — one row per payment with status, attempts, lease, and optional remote id.

Each shard DB (`coffee_shard_0`, `coffee_shard_1`, …):
- `payments` — durable record of payments propagated to the remote system. Unique on
  `(store_id, idempotency_key)` so an idempotent replay never produces duplicates.

## Sharding strategy

- Number of shards is configured statically via `coffeeshop.sharding.count` and the
  `coffeeshop.sharding.instances[*]` list — this assignment does not implement resharding.
- `ShardResolver` hashes `store_id|idempotency_key` (FNV-1a) and takes `mod count` to pick
  the destination shard. The same logical payment always lands on the same shard.
- `ShardJdbcTemplates` exposes the per-shard `JdbcTemplate` used by `ShardedPaymentRepository`.

## Async processing

- `BatchWorker` polls every `coffeeshop.worker.poll-delay-ms` and delegates to
  `BatchOrchestrator`.
- `leaseNextBatch` uses `UPDATE … RETURNING` with `FOR UPDATE SKIP LOCKED` to atomically
  claim a chunk of `PENDING` (or expired-lease `PROCESSING`) items, so multiple workers can
  run in parallel without double-processing.
- Each item is processed in its own coordinator-DB transaction. Both the shard write and
  the remote API call are idempotent, so re-leasing after a crash is safe.
- On failure the item is requeued back to `PENDING` until `coffeeshop.worker.max-attempts`
  is reached, after which it is marked `FAILED` and the request status moves accordingly.

## Configuration

`src/main/resources/application.properties`:
- `coffeeshop.coordinator.*` — coordinator DB JDBC URL / credentials.
- `coffeeshop.sharding.count` and `coffeeshop.sharding.instances[i].*` — one entry per shard.
- `coffeeshop.remote.base-url` — base URL of the remote payments API.
- `coffeeshop.worker.*` — `enabled`, `poll-delay-ms`, `batch-size`, `max-attempts`,
  `lease-duration-ms`.

## Run locally

Prerequisites: JDK 21, PostgreSQL 14+ reachable on `localhost:5432`.

1. Create the databases referenced in `application.properties`:
   ```sql
   CREATE DATABASE coffee_coordinator;
   CREATE DATABASE coffee_shard_0;
   CREATE DATABASE coffee_shard_1;
   CREATE USER coffee WITH PASSWORD 'coffee';
   GRANT ALL PRIVILEGES ON DATABASE coffee_coordinator TO coffee;
   GRANT ALL PRIVILEGES ON DATABASE coffee_shard_0 TO coffee;
   GRANT ALL PRIVILEGES ON DATABASE coffee_shard_1 TO coffee;
   ```
2. Start the upstream remote payments service on `http://localhost:8080`.
3. Build and run:
   ```bash
   ./gradlew bootRun
   ```
   Flyway migrates the coordinator DB and every shard DB on startup.

## Run with Docker (full stack)

From the monorepo root (`coffee_shop/`):

```bash
docker compose up --build
```

The backend starts with the `docker` Spring profile (`application-docker.properties`).
See the root `README.md` for ports and prerequisites.

## Tests

```bash
./gradlew test
```

Covers:
- `ShardResolverTest` — hash-modulo determinism, range, and distribution.
- `BatchStatusAggregatorTest` — request status transitions from item counters.
- `BatchOrchestratorTest` — leasing, success/replay/retry/permanent-failure paths.
- `PaymentBatchControllerTest` — `202` on submit, `400` ProblemDetail on validation errors,
  `200` / `404` for status polling.

## Known limitations

- No resharding: the shard count is fixed at startup. Changing it would require a manual
  data migration that is out of scope for this assignment.
- Failure recovery relies on remote idempotency. If the remote service stops honoring
  `Idempotency-Key`, retried items can produce duplicate remote rows.
- The worker is a single in-process scheduler; horizontal scaling is supported by the
  `FOR UPDATE SKIP LOCKED` lease but has not been load-tested.
