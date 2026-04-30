CREATE TABLE IF NOT EXISTS prices (
    time     TIMESTAMPTZ      NOT NULL,
    location INTEGER          NOT NULL DEFAULT 0,
    product  VARCHAR(250),
    price    DOUBLE PRECISION NULL,
    grams    DOUBLE PRECISION NULL
);
