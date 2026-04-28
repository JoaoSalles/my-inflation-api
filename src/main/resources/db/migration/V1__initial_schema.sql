CREATE TABLE IF NOT EXISTS price_snapshots (
    id             BIGSERIAL PRIMARY KEY,
    product_name   VARCHAR(255)             NOT NULL,
    brand          VARCHAR(255)             NOT NULL,
    price          INTEGER                  NOT NULL,
    quantity_base  VARCHAR(20)              NOT NULL,
    scraped_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
