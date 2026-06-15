# Coffee Place Client

React client for propagating end-of-day notebook payments to a central payments system.

## What it does

1. Loads existing payments from the backend (`GET /api/v1/payments?storeId=...`)
2. Upload a CSV notebook export
3. Validate each payment locally before sending
4. Inspect request/response JSON side by side for each row
5. Send payments to `POST /api/v1/payments` one row at a time
6. Retry safely using `Idempotency-Key`
7. Refresh the backend preview after propagation

## CSV format

```csv
storeId,idempotencyKey,coffeeType,price,currency,loyaltyCardId
store-london-01,order-001,LATTE,3.50,EUR,card-999
```

## Request / response JSON view

Click a valid row in the import table to see:

- **Request** — `POST` URL, headers (`Store-Id`, `Idempotency-Key`), and JSON body
- **Response** — HTTP status and response body after propagation

## Project structure

```
src/
  domain/         # Payment rules and validation
  application/    # parseImport and propagatePayments use cases
  services/       # CSV/JSON reader, API client, retry policy
  presentation/   # React UI, hooks, pages
```

## Run locally

1. Start your backend API server:

```bash
# run your backend so /api/v1/payments is available on localhost:8080
```

2. Start the client:

```bash
npm install
npm run dev
```

3. Open the Vite URL (usually `http://localhost:5173`)
4. The backend preview loads payments for `store-london-01` automatically
5. Upload a CSV file, inspect request/response JSON, then click **Propagate**

The Vite dev server proxies `/api` to `http://localhost:8080` so the browser avoids CORS issues.

## Idempotency

The central API keys payments on `Store-Id` + `Idempotency-Key`:

- First send → `201 Created` (new payment)
- Same key again → `200 OK` (original payment replayed, no duplicate)

In the UI:

- Every request includes `Idempotency-Key` in the JSON headers
- **Send request** twice with the same key to see `200 OK` replay
- **New key** generates a fresh UUID for a new payment
- CSV rows should use stable `idempotencyKey` values per sale

## Reliability

- Each row must include a stable `idempotencyKey`
- Retries reuse the same key, so the API returns `200` instead of creating duplicates
- Transient network and `5xx` errors are retried with exponential backoff
- `400` validation errors are not retried
- Sent payment metadata is stored in `localStorage` for local history and manual reset

