# StarHarbour Cassandra Design Rationale

## Assumptions

- Cluster runs in one DC (`datacenter1`) with RF=3 for local production-like resiliency.
- `orders` volume: ~1,200 per store/day, ~3 line items per order.
- "Given month" means calendar month; month buckets use the first day of month in a `date` column (for example, `2026-06-01`).
- Query Q1 and Q7 are paginated; the application can read month buckets from newest to older as needed.

## Query -> Table Mapping

| Query ID | Access pattern | Cassandra table | Partition key | Clustering | Notes |
|---|---|---|---|---|---|
| Q1 | Customer order history, newest first | `customer_orders_by_month` | `(customer_id, order_month)` | `order_ts DESC, order_id ASC` | Month bucketing prevents unbounded customer partitions |
| Q2 | Line items for order | `order_items_by_order` | `order_id` | `product_id ASC` | One partition per order |
| Q3 | Customer by `customer_id` | `customer_profile_by_id` | `customer_id` | none | Direct key lookup |
| Q4 | Customer by email | `customer_profile_by_email` | `email` | none | Alternate-key lookup for login |
| Q5 | Store orders for day, newest first | `orders_by_store_day` | `(store_id, order_date)` | `order_ts DESC, order_id ASC` | Bounded naturally by day |
| Q6 | Top-selling products for store+month | `product_sales_rank_by_store_month` | `(store_id, sales_month)` | `units_sold DESC, product_id ASC` | Precomputed rank table (read-optimized) |
| Q7 | Reviews for product, newest first | `reviews_by_product_month` | `(product_id, review_month)` | `review_ts DESC, review_id ASC` | Month bucketing handles long-tail/popular products |

## Denormalization Decisions

The model duplicates data deliberately to avoid joins and to satisfy query-first reads:

- Customer profile fields (`full_name`, `phone`, `signup_date`, `loyalty_tier`, `loyalty_points`) are duplicated in both:
  - `customer_profile_by_id`
  - `customer_profile_by_email`
  This supports both Q3 and Q4 with a single partition read.

- Order header fields (`store_id`, `customer_id`, `employee_id`, `status`, `total_amount`, `payment_method`, timestamps) are duplicated between:
  - `customer_orders_by_month` (Q1)
  - `orders_by_store_day` (Q5)
  Same order event is projected for customer-centric and store/day-centric reads.

- Product descriptive fields (`product_name`, `category`) are duplicated into:
  - `order_items_by_order` (Q2, to render line items without product join)
  - `product_sales_rank_by_store_month` (Q6 ranking response without product join)

- Review display field `customer_name` is duplicated in `reviews_by_product_month` to avoid customer lookup while rendering review feeds.

## Partition Size Analysis

Rule of thumb target: keep partitions well below ~100,000 rows and ~100 MB.

| Table | Estimated rows per partition | Bounded? | Why it is safe / mitigation |
|---|---:|---|---|
| `customer_orders_by_month` | Typical customer: very low (single digits to tens/month); heavy user still usually << 1,000/month | Yes | Month bucket (`order_month`) caps growth over time |
| `order_items_by_order` | ~3 average, maybe up to tens | Yes | Partition is a single order |
| `customer_profile_by_id` | 1 | Yes | One row by key |
| `customer_profile_by_email` | 1 | Yes | One row by key |
| `orders_by_store_day` | ~1,200/day/store | Yes | Day bucket in partition key keeps fixed daily bound |
| `product_sales_rank_by_store_month` | Up to active products in store for month (<= ~300) | Yes | One row per product/month/store |
| `reviews_by_product_month` | Popular product could be large, but monthly capped (for example 10k-100k/month worst case) | Yes (with bucket) | `review_month` prevents lifetime unbounded partitions; if a product exceeds monthly limits, split to week bucket |

### Unbounded-partition risks and redesign

- **Without bucketing**, both customer order history and product reviews would grow indefinitely by customer/product.
- This design explicitly uses month buckets in:
  - `customer_orders_by_month`
  - `reviews_by_product_month`
- If real traffic shows monthly review partitions still too large for a few hot products, move reviews to weekly buckets (`review_week`) with no query contract change besides bucket iteration logic.

## New Order Write Fan-out

For each newly created order (header + N line items), application writes to:

1. `customer_orders_by_month` (one row per order)
2. `orders_by_store_day` (one row per order)
3. `order_items_by_order` (N rows, one per line item)
4. `product_sales_rank_by_store_month` (one upsert per product in order, after recomputing monthly `units_sold` / `gross_sales`)

Total writes per order are `3 + N` minimum if sales ranking is maintained synchronously, or `2 + N` on the order path plus async aggregation updates.

## Update Handling (Application Perspective)

### Customer profile updates

When customer profile fields change (name, phone, loyalty tier/points):

- Update `customer_profile_by_id` and `customer_profile_by_email` in the same request flow.
- Email change should be treated as:
  1. insert new row in `customer_profile_by_email` (new email),
  2. delete old email row,
  3. update `customer_profile_by_id`.

Batching can be used only when all mutations are in the same partition (rare here), so prefer idempotent dual writes with retry logic.

### Order status updates

When order status changes (`PLACED` -> `PAID` -> `COMPLETED`, etc.):

- Update the corresponding row in:
  - `customer_orders_by_month`
  - `orders_by_store_day`

The application must know `customer_id`, `order_month`, `store_id`, `order_date`, `order_ts`, and `order_id` to target the exact clustering row for each table.

### Product / catalog updates

If product names/categories change:

- Update source product catalog table (outside this assignment's query set).
- Optionally backfill denormalized copies in:
  - `order_items_by_order` (historical orders can be left immutable if desired),
  - `product_sales_rank_by_store_month` (usually update for current/future views only).

### Review writes

On new review creation:

- Insert into `reviews_by_product_month` using `(product_id, review_month)` bucket and descending `review_ts`.

## Many-to-many and Aggregates

- Relational `product_supplier` many-to-many is not part of Q1..Q7, so it is intentionally omitted from this serving model.
- Q6 is an aggregate/ranking query. Cassandra serves it via precomputed projection (`product_sales_rank_by_store_month`) rather than ad hoc group-by over raw order items.
- Recommended implementation for Q6 freshness and correctness:
  - Maintain monthly product totals in an ingest/stream job.
  - Re-write rank rows when totals change (delete old rank row + insert new rank row if clustering key `units_sold` changes).
