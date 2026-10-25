package dev.bankinsights.backend.analytics;

import java.util.List;

import dev.bankinsights.backend.transaction.TransactionCategory;

public record MonthlySummary(String month, List<CurrencySummary> currencies) {
    public record CurrencySummary(
        String currency,
        String incoming,
        String outgoing,
        String net,
        String fees,
        int transactionCount,
        int reviewCount,
        List<CategorySpending> categories
    ) {}

    public record CategorySpending(
        TransactionCategory category,
        String amount,
        int transactionCount,
        int shareBasisPoints
    ) {}
}
