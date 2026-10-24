CREATE TABLE transactions (
    id UUID PRIMARY KEY,

    description TEXT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    state VARCHAR(32) NOT NULL,

    amount NUMERIC NOT NULL,
    fee NUMERIC NOT NULL,
    booking_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    category VARCHAR(32) NOT NULL DEFAULT 'UNCATEGORIZED',
    category_source VARCHAR(32) NOT NULL DEFAULT 'UNCLASSIFIED',

    category_explanation TEXT,
    category_review_required BOOLEAN,
    categorized_at TIMESTAMP WITH TIME ZONE
);