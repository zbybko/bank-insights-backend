package dev.bankinsights.backend.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.bankinsights.backend.categorization.CategorySuggestion;
import org.junit.jupiter.api.Test;

class TransactionTest {
    private final Transaction transaction = new Transaction("Example marketplace", "EUR", "COMPLETED",
        new BigDecimal("-42.50"), BigDecimal.ZERO, LocalDateTime.of(2026, 10, 1, 12, 0));

    @Test
    void uncertainSuggestionDoesNotBecomeConfirmedCategory() {
        transaction.applyAiSuggestion(new CategorySuggestion(TransactionCategory.SHOPPING,
            "The item purchased is unknown.", true, "What did you buy?"));

        assertThat(transaction.getCategory()).isEqualTo(TransactionCategory.UNCATEGORIZED);
        assertThat(transaction.getSuggestedCategory()).isEqualTo(TransactionCategory.SHOPPING);
        assertThat(transaction.getCategorySource()).isEqualTo(CategorySource.AI);
        assertThat(transaction.getCategoryReviewRequired()).isTrue();
        assertThat(transaction.getCategoryQuestion()).isEqualTo("What did you buy?");
        assertThat(transaction.getCategorizedAt()).isNotNull();
    }

    @Test
    void userConfirmationClosesReviewAndPreservesOriginalSuggestion() {
        transaction.applyAiSuggestion(new CategorySuggestion(TransactionCategory.SHOPPING,
            "The item purchased is unknown.", true, "What did you buy?"));
        transaction.confirmCategory(TransactionCategory.GROCERIES);

        assertThat(transaction.getCategory()).isEqualTo(TransactionCategory.GROCERIES);
        assertThat(transaction.getCategorySource()).isEqualTo(CategorySource.USER);
        assertThat(transaction.getSuggestedCategory()).isEqualTo(TransactionCategory.SHOPPING);
        assertThat(transaction.getCategoryReviewRequired()).isFalse();
        assertThat(transaction.getCategoryQuestion()).isEmpty();
    }

    @Test
    void clearSuggestionAppliesCategoryWithoutReview() {
        transaction.applyAiSuggestion(new CategorySuggestion(TransactionCategory.GROCERIES,
            "Supermarket purchase.", false, ""));
        assertThat(transaction.getCategory()).isEqualTo(TransactionCategory.GROCERIES);
        assertThat(transaction.getCategoryReviewRequired()).isFalse();
    }
}
