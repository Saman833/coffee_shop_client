CREATE DATABASE coffee_shard_0;
CREATE DATABASE coffee_shard_1;

GRANT ALL PRIVILEGES ON DATABASE coffee_coordinator TO coffee;
GRANT ALL PRIVILEGES ON DATABASE coffee_shard_0 TO coffee;
GRANT ALL PRIVILEGES ON DATABASE coffee_shard_1 TO coffee;
