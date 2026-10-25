package dev.bankinsights.backend.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.bankinsights.backend.transaction.Transaction;
import dev.bankinsights.backend.transaction.TransactionCategory;
import dev.bankinsights.backend.transaction.TransactionRepository;

@Service
public class MonthlySummaryService {
    private final TransactionRepository repository;

    public MonthlySummaryService(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public MonthlySummary summarize(YearMonth month) {
        var transactions = repository.findByStateAndBookingDateGreaterThanEqualAndBookingDateLessThan(
            "COMPLETED", month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
        var byCurrency = new TreeMap<String, List<Transaction>>();
        for (var transaction : transactions) {
            byCurrency.computeIfAbsent(transaction.getCurrency(), ignored -> new ArrayList<>()).add(transaction);
        }
        var summaries = byCurrency.entrySet().stream()
            .map(entry -> summarizeCurrency(entry.getKey(), entry.getValue())).toList();
        return new MonthlySummary(month.toString(), summaries);
    }

    private MonthlySummary.CurrencySummary summarizeCurrency(String currency, List<Transaction> transactions) {
        BigDecimal incoming = BigDecimal.ZERO;
        BigDecimal outgoing = BigDecimal.ZERO;
        BigDecimal fees = BigDecimal.ZERO;
        int reviewCount = 0;
        var amounts = new EnumMap<TransactionCategory, BigDecimal>(TransactionCategory.class);
        var counts = new EnumMap<TransactionCategory, Integer>(TransactionCategory.class);
        for (var transaction : transactions) {
            var amount = transaction.getAmount();
            fees = fees.add(transaction.getFee());
            if (Boolean.TRUE.equals(transaction.getCategoryReviewRequired())) reviewCount++;
            if (amount.signum() > 0) incoming = incoming.add(amount);
            if (amount.signum() < 0) {
                var spent = amount.negate();
                outgoing = outgoing.add(spent);
                amounts.merge(transaction.getCategory(), spent, BigDecimal::add);
                counts.merge(transaction.getCategory(), 1, Integer::sum);
            }
        }
        var categories = new ArrayList<MonthlySummary.CategorySpending>();
        for (var entry : amounts.entrySet()) {
            int share = outgoing.signum() == 0 ? 0 : entry.getValue()
                .multiply(BigDecimal.valueOf(10_000)).divide(outgoing, 0, RoundingMode.HALF_UP).intValueExact();
            categories.add(new MonthlySummary.CategorySpending(entry.getKey(), entry.getValue().toPlainString(),
                counts.get(entry.getKey()), share));
        }
        categories.sort((left, right) -> new BigDecimal(right.amount()).compareTo(new BigDecimal(left.amount())));
        return new MonthlySummary.CurrencySummary(currency, incoming.toPlainString(), outgoing.toPlainString(),
            incoming.subtract(outgoing).toPlainString(), fees.toPlainString(), transactions.size(), reviewCount,
            List.copyOf(categories));
    }
}
