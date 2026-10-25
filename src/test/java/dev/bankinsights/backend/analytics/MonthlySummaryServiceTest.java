package dev.bankinsights.backend.analytics;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import dev.bankinsights.backend.categorization.CategorySuggestion;
import dev.bankinsights.backend.transaction.*;
import org.junit.jupiter.api.Test;

class MonthlySummaryServiceTest {
    private final TransactionRepository repository = mock(TransactionRepository.class);
    private final MonthlySummaryService service = new MonthlySummaryService(repository);
    private final YearMonth month = YearMonth.of(2026, 10);

    @Test
    void separatesCurrenciesAndDoesNotCountIncomingTransfersAsNegativeSpending() {
        var salary = transaction("2400.00", "EUR", "0.00");
        var groceries = transaction("-42.50", "EUR", "0.25");
        groceries.confirmCategory(TransactionCategory.GROCERIES);
        var uncertain = transaction("-7.50", "EUR", "0.00");
        uncertain.applyAiSuggestion(new CategorySuggestion(TransactionCategory.SHOPPING, "Uncertain.", true, "What was bought?"));
        var dollars = transaction("-9.99", "USD", "0.00");
        when(repository.findByStateAndBookingDateGreaterThanEqualAndBookingDateLessThan("COMPLETED",
            month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay()))
            .thenReturn(List.of(salary, groceries, uncertain, dollars));
        var result = service.summarize(month);
        assertThat(result.currencies()).hasSize(2);
        var euro = result.currencies().getFirst();
        assertThat(euro.currency()).isEqualTo("EUR");
        assertThat(euro.incoming()).isEqualTo("2400.00");
        assertThat(euro.outgoing()).isEqualTo("50.00");
        assertThat(euro.net()).isEqualTo("2350.00");
        assertThat(euro.fees()).isEqualTo("0.25");
        assertThat(euro.reviewCount()).isEqualTo(1);
        assertThat(euro.categories()).extracting(MonthlySummary.CategorySpending::category)
            .containsExactly(TransactionCategory.GROCERIES, TransactionCategory.UNCATEGORIZED);
        assertThat(euro.categories().getFirst().shareBasisPoints()).isEqualTo(8500);
        assertThat(result.currencies().get(1).outgoing()).isEqualTo("9.99");
    }

    @Test
    void emptyMonthDoesNotInventCurrencyOrBalance() {
        when(repository.findByStateAndBookingDateGreaterThanEqualAndBookingDateLessThan(any(), any(), any()))
            .thenReturn(List.of());
        assertThat(service.summarize(month).currencies()).isEmpty();
    }

    private Transaction transaction(String amount, String currency, String fee) {
        return new Transaction("Synthetic", currency, "COMPLETED", new BigDecimal(amount), new BigDecimal(fee),
            month.atDay(3).atStartOfDay());
    }
}
