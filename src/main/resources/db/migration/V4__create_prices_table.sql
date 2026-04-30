CREATE TABLE IF NOT EXISTS prices (
    time     TIMESTAMPTZ      NOT NULL,
    location INTEGER          NOT NULL DEFAULT 0,
    product  VARCHAR(250),
    price    DOUBLE PRECISION NULL,
    brand          VARCHAR(255)             NOT NULL,
    quantity_base  VARCHAR(20)              NOT NULL DEFAULT 'GRAMS'
);
