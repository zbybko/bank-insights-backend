package dev.bankinsights.backend;

public record CategorySuggestion(
    TransactionCategory category,
    String explanation,
    boolean reviewRequired
) {
    
}
