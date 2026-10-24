CREATE TABLE csv_imports (
    id UUID PRIMARY KEY,
    file_hash VARCHAR(64) NOT NULL UNIQUE,
    imported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    transaction_count INTEGER NOT NULL
        CHECK (transaction_count >= 0)
);

ALTER TABLE transactions
    ADD COLUMN csv_import_id UUID
        REFERENCES csv_imports(id);

CREATE INDEX idx_transactions_csv_import_id
    ON transactions(csv_import_id);