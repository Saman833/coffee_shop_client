CREATE KEYSPACE IF NOT EXISTS starharbour
WITH replication = {
  'class': 'NetworkTopologyStrategy',
  'datacenter1': 3
};

USE starharbour;

CREATE TABLE IF NOT EXISTS customer_orders_by_month (
  customer_id uuid,
  order_month date,
  order_ts timestamp,
  order_id uuid,
  store_id uuid,
  employee_id uuid,
  status text,
  total_amount decimal,
  payment_method text,
  PRIMARY KEY ((customer_id, order_month), order_ts, order_id)
) WITH CLUSTERING ORDER BY (order_ts DESC, order_id ASC);

CREATE TABLE IF NOT EXISTS order_items_by_order (
  order_id uuid,
  product_id uuid,
  product_name text,
  quantity int,
  unit_price decimal,
  line_total decimal,
  PRIMARY KEY ((order_id), product_id)
) WITH CLUSTERING ORDER BY (product_id ASC);

CREATE TABLE IF NOT EXISTS customer_profile_by_id (
  customer_id uuid,
  full_name text,
  email text,
  phone text,
  signup_date date,
  loyalty_tier text,
  loyalty_points int,
  PRIMARY KEY ((customer_id))
);

CREATE TABLE IF NOT EXISTS customer_profile_by_email (
  email text,
  customer_id uuid,
  full_name text,
  phone text,
  signup_date date,
  loyalty_tier text,
  loyalty_points int,
  PRIMARY KEY ((email))
);

CREATE TABLE IF NOT EXISTS orders_by_store_day (
  store_id uuid,
  order_date date,
  order_ts timestamp,
  order_id uuid,
  customer_id uuid,
  employee_id uuid,
  status text,
  total_amount decimal,
  payment_method text,
  PRIMARY KEY ((store_id, order_date), order_ts, order_id)
) WITH CLUSTERING ORDER BY (order_ts DESC, order_id ASC);

CREATE TABLE IF NOT EXISTS product_sales_rank_by_store_month (
  store_id uuid,
  sales_month date,
  units_sold int,
  product_id uuid,
  product_name text,
  category text,
  gross_sales decimal,
  PRIMARY KEY ((store_id, sales_month), units_sold, product_id)
) WITH CLUSTERING ORDER BY (units_sold DESC, product_id ASC);

CREATE TABLE IF NOT EXISTS reviews_by_product_month (
  product_id uuid,
  review_month date,
  review_ts timestamp,
  review_id uuid,
  customer_id uuid,
  rating tinyint,
  comment text,
  customer_name text,
  PRIMARY KEY ((product_id, review_month), review_ts, review_id)
) WITH CLUSTERING ORDER BY (review_ts DESC, review_id ASC);
