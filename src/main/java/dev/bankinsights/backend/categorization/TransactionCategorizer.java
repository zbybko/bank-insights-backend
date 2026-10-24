package dev.bankinsights.backend.categorization;

import dev.bankinsights.backend.transaction.Transaction;

public interface TransactionCategorizer {
    CategorySuggestion suggestCategory(Transaction transaction);
}
