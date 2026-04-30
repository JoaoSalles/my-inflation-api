CREATE TABLE IF NOT EXISTS products_to_scrap (
    id             BIGSERIAL PRIMARY KEY,
    product_name   VARCHAR(250)             NOT NULL,
    search          VARCHAR(250)             NOT NULL,
    quantity_base  VARCHAR(20)              NOT NULL DEFAULT 'GRAMS',
    key_words      TEXT                     NOT NULL DEFAULT '[]',
    deny_words     TEXT                     NOT NULL DEFAULT '[]',
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
