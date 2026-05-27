CREATE TABLE IF NOT EXISTS prices (
    time            TIMESTAMPTZ     NOT NULL,
    location        INTEGER         NOT NULL DEFAULT 0,
    product         VARCHAR(100),
    price           INTEGER,
    brand           VARCHAR(80)    NOT NULL,
    quantity_base   VARCHAR(20)     NOT NULL DEFAULT 'GRAMS',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
