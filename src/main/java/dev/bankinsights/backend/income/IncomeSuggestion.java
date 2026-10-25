package dev.bankinsights.backend.income;

public record IncomeSuggestion(IncomeType type, String explanation, boolean reviewRequired, String question) {}
