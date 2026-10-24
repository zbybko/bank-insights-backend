ALTER TABLE transactions
    ADD COLUMN suggested_category VARCHAR(32),
    ADD COLUMN category_question TEXT;