package dev.bankinsights.backend;

public interface TransactionCategorizer {
    CategorySuggestion suggestCategory(Transaction transaction);
}
