-- ─────────────────────────────────────────────
-- TRANSACTIONS  (live, mutable records)
-- ─────────────────────────────────────────────

CREATE TABLE transactions (
    id            VARCHAR(36)     PRIMARY KEY,
    external_id   VARCHAR(255)    NOT NULL,
    type          VARCHAR(20)     NOT NULL CHECK (type IN ('flight','hotel','rail','navan_fee')),
    currency      CHAR(3)         NOT NULL,
    occurred_at   TIMESTAMPTZ     NOT NULL,
    total         NUMERIC(19,4)   NOT NULL,
    metadata      JSONB           NOT NULL,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version       BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT uq_transactions_external_id UNIQUE (external_id)
);

CREATE TABLE transaction_tax_lines (
    id              BIGSERIAL       PRIMARY KEY,
    transaction_id  VARCHAR(36)     NOT NULL
                        REFERENCES transactions(id) ON DELETE CASCADE,
    name            VARCHAR(255)    NOT NULL,
    rate            NUMERIC(8,4),           -- nullable: Airport tax has no rate
    amount          NUMERIC(19,4)   NOT NULL,
    position        INT             NOT NULL DEFAULT 0
);

CREATE TABLE transaction_fee_lines (
    id              BIGSERIAL       PRIMARY KEY,
    transaction_id  VARCHAR(36)     NOT NULL
                        REFERENCES transactions(id) ON DELETE CASCADE,
    name            VARCHAR(255)    NOT NULL,
    amount          NUMERIC(19,4)   NOT NULL,
    position        INT             NOT NULL DEFAULT 0
);

-- ─────────────────────────────────────────────
-- INVOICES  (frozen snapshots — never live joins)
-- ─────────────────────────────────────────────

CREATE TABLE invoices (
    id          VARCHAR(36)     PRIMARY KEY,
    currency    CHAR(3)         NOT NULL,
    total       NUMERIC(19,4)   NOT NULL,
    tax_total   NUMERIC(19,4)   NOT NULL DEFAULT 0,
    fee_total   NUMERIC(19,4)   NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version     BIGINT          NOT NULL DEFAULT 0
);

-- One row per transaction included in the invoice.
-- transaction_id is a plain column (no FK) — the invoice owns a copy, not a live reference.
CREATE TABLE invoice_lines (
    id              BIGSERIAL       PRIMARY KEY,
    invoice_id      VARCHAR(36)     NOT NULL
                        REFERENCES invoices(id) ON DELETE CASCADE,
    transaction_id  VARCHAR(36)     NOT NULL
                    REFERENCES transactions(id),   -- RESTRICT by default: blocks txn delete if invoiced
    external_id     VARCHAR(255)    NOT NULL,
    type            VARCHAR(20)     NOT NULL,
    currency        CHAR(3)         NOT NULL,
    occurred_at     TIMESTAMPTZ     NOT NULL,
    total           NUMERIC(19,4)   NOT NULL,
    metadata        JSONB           NOT NULL     -- frozen copy
);

CREATE TABLE invoice_line_tax_lines (
    id              BIGSERIAL       PRIMARY KEY,
    invoice_line_id BIGINT          NOT NULL
                        REFERENCES invoice_lines(id) ON DELETE CASCADE,
    name            VARCHAR(255)    NOT NULL,
    rate            NUMERIC(8,4),
    amount          NUMERIC(19,4)   NOT NULL,
    position        INT             NOT NULL DEFAULT 0
);

CREATE TABLE invoice_line_fee_lines (
    id              BIGSERIAL       PRIMARY KEY,
    invoice_line_id BIGINT          NOT NULL
                        REFERENCES invoice_lines(id) ON DELETE CASCADE,
    name            VARCHAR(255)    NOT NULL,
    amount          NUMERIC(19,4)   NOT NULL,
    position        INT             NOT NULL DEFAULT 0
);

-- Rolled-up totals per invoice (pre-computed at snapshot time)
CREATE TABLE invoice_tax_rollup (
    id          BIGSERIAL       PRIMARY KEY,
    invoice_id  VARCHAR(36)     NOT NULL
                    REFERENCES invoices(id) ON DELETE CASCADE,
    name        VARCHAR(255)    NOT NULL,
    rate        NUMERIC(8,4),
    amount      NUMERIC(19,4)   NOT NULL
);

CREATE TABLE invoice_fee_rollup (
    id          BIGSERIAL       PRIMARY KEY,
    invoice_id  VARCHAR(36)     NOT NULL
                    REFERENCES invoices(id) ON DELETE CASCADE,
    name        VARCHAR(255)    NOT NULL,
    amount      NUMERIC(19,4)   NOT NULL
);

-- ─────────────────────────────────────────────
-- INDEXES  (volume / performance)
-- ─────────────────────────────────────────────

-- Already covered by the UNIQUE constraint above:
-- CREATE UNIQUE INDEX uq_transactions_external_id ON transactions(external_id);

-- Keyset pagination on both main tables
CREATE INDEX idx_transactions_created_at_id ON transactions(created_at, id);
CREATE INDEX idx_invoices_created_at_id     ON invoices(created_at, id);

-- "Is this transaction on any invoice?" — used by PATCH/DELETE guards
CREATE INDEX idx_invoice_lines_transaction_id ON invoice_lines(transaction_id);

-- Child table FK lookups
CREATE INDEX idx_txn_tax_lines_txn_id  ON transaction_tax_lines(transaction_id);
CREATE INDEX idx_txn_fee_lines_txn_id  ON transaction_fee_lines(transaction_id);
CREATE INDEX idx_inv_line_tax_inv_line ON invoice_line_tax_lines(invoice_line_id);
CREATE INDEX idx_inv_line_fee_inv_line ON invoice_line_fee_lines(invoice_line_id);
CREATE INDEX idx_inv_tax_rollup_inv_id ON invoice_tax_rollup(invoice_id);
CREATE INDEX idx_inv_fee_rollup_inv_id ON invoice_fee_rollup(invoice_id);