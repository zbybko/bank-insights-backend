ALTER TABLE transactions
    ADD COLUMN source_type VARCHAR(64),
    ADD COLUMN source_product VARCHAR(64);

CREATE TABLE income_assessments (
    transaction_id UUID PRIMARY KEY REFERENCES transactions(id) ON DELETE CASCADE,
    version BIGINT NOT NULL DEFAULT 0,
    type VARCHAR(32) NOT NULL,
    suggested_type VARCHAR(32),
    source VARCHAR(16) NOT NULL,
    explanation TEXT NOT NULL,
    review_required BOOLEAN NOT NULL,
    question TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_transactions_income_history
    ON transactions (lower(trim(description)), currency, booking_date DESC)
    WHERE state = 'COMPLETED' AND amount > 0;
