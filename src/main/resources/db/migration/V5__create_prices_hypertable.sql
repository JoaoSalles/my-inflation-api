SELECT create_hypertable('prices', by_range('time'));
SELECT add_dimension('prices', by_hash('product', 4));
CREATE INDEX ON prices (product, brand, time DESC);