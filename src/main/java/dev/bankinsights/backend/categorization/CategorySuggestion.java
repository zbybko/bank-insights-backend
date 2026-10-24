package dev.bankinsights.backend.categorization;

import dev.bankinsights.backend.transaction.TransactionCategory;

public record CategorySuggestion(
    TransactionCategory category,
    String explanation,
    boolean reviewRequired,
    String question
) {

}
