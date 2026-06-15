# Coffee Shop

Monorepo for the Coffee Place assignment: React client + async/sharded Spring Boot backend.

## What runs where

| Service | Port | Role |
|---------|------|------|
| `frontend` | 3000 | React UI |
| `backend` | 8081 | Async batch API (`/api/v1/payment-batches`) |
| `remote-payments` | 8080 | Upstream payments API (`/api/v1/payments`) |
| `postgres` | 5432 | Coordinator + 2 payment shards |

## Run everything with Docker

Prerequisites: Docker Desktop, and the upstream repo cloned as a sibling:

```bash
# from d_systems (or your workspace root)
git clone https://github.com/igor-sakhankov/harbour-cloud-26.git harbour-cloud-26-main
```

Then from this folder:

```bash
docker compose up --build
```

Open http://localhost:3000

- Upload a CSV and click **Submit payments (async)** — the client posts to the coffee-shop backend, which stores rows in Postgres and processes them in the background.
- Poll the batch status by request id (shown in the progress panel).
- The **backend preview** panel still reads from the remote payments API on port 8080.

Stop:

```bash
docker compose down
```

## Run without Docker

1. Start Postgres and create `coffee_coordinator`, `coffee_shard_0`, `coffee_shard_1` (see `backend/README.md`).
2. Start remote payments: `cd ../harbour-cloud-26-main && ./gradlew bootRun` (port 8080).
3. Start backend: `cd backend && ./gradlew bootRun` (port 8081).
4. Start client: `cd client && npm install && npm run dev` (port 5173).

Client dev proxies:
- `/api/v1/payment-batches` → `http://localhost:8081`
- `/api/v1/payments` → `http://localhost:8080`

## Assignment checklist

- [x] Bulk submit stored in relational DB (Postgres), returns `requestId` immediately (`202`)
- [x] Client queries batch status by `requestId`
- [x] Worker processes each payment asynchronously and calls remote API
- [x] Payments table sharded across multiple Postgres databases (static hash-modulo)
- [x] No resharding (shard count fixed in config)
- [x] Remote repo not modified — built from a local clone only

## Project layout

```
coffee_shop/
  client/     React frontend
  backend/    Spring Boot async + sharded backend
  docker/     Shared Docker assets (Postgres init, remote-payments Dockerfile)
  compose.yaml
```
