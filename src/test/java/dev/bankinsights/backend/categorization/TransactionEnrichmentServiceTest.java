package dev.bankinsights.backend.categorization;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import dev.bankinsights.backend.transaction.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

class TransactionEnrichmentServiceTest {
    private final TransactionCategorizer categorizer = mock(TransactionCategorizer.class);
    private final TransactionRepository repository = mock(TransactionRepository.class);
    private final TransactionEnrichmentService service = new TransactionEnrichmentService(categorizer, repository);
    private final UUID id = UUID.randomUUID();
    private final Transaction transaction = new Transaction("Example shop", "EUR", "COMPLETED",
        new BigDecimal("-42.50"), BigDecimal.ZERO, LocalDateTime.of(2026, 10, 1, 12, 0));

    @Test
    void newSuggestionIsSaved() {
        when(repository.findById(id)).thenReturn(Optional.of(transaction));
        when(categorizer.suggestCategory(transaction)).thenReturn(new CategorySuggestion(
            TransactionCategory.UNCATEGORIZED, "Unknown purchase.", true, "What did you buy?"));
        when(repository.save(transaction)).thenReturn(transaction);

        assertThat(service.enrich(id)).isSameAs(transaction);
        assertThat(transaction.getCategoryReviewRequired()).isTrue();
        verify(repository).save(transaction);
        verify(categorizer).suggestCategory(transaction);
    }

    @ParameterizedTest
    @EnumSource(value = CategorySource.class, names = {"USER", "AI"})
    void alreadyClassifiedTransactionsDoNotCallAiOrSaveAgain(CategorySource source) {
        transaction.assignCategory(TransactionCategory.GROCERIES, source);
        when(repository.findById(id)).thenReturn(Optional.of(transaction));

        assertThat(service.enrich(id)).isSameAs(transaction);
        assertThat(transaction.getCategory()).isEqualTo(TransactionCategory.GROCERIES);
        assertThat(transaction.getCategorySource()).isEqualTo(source);
        verifyNoInteractions(categorizer);
        verify(repository, never()).save(any());
    }

    @Test
    void missingTransactionNeverCallsAi() {
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.enrich(id)).isInstanceOf(TransactionNotFoundException.class);
        assertThatThrownBy(() -> service.confirmCategory(id, TransactionCategory.HEALTH))
            .isInstanceOf(TransactionNotFoundException.class);
        verifyNoInteractions(categorizer);
        verify(repository, never()).save(any());
    }

    @Test
    void providerFailureDoesNotSavePartialClassification() {
        when(repository.findById(id)).thenReturn(Optional.of(transaction));
        when(categorizer.suggestCategory(transaction)).thenThrow(new IllegalStateException("Provider unavailable"));
        assertThatThrownBy(() -> service.enrich(id)).isInstanceOf(IllegalStateException.class);
        assertThat(transaction.getCategorySource()).isEqualTo(CategorySource.UNCLASSIFIED);
        verify(repository, never()).save(any());
    }

    @Test
    void manualConfirmationSavesWithoutCallingAi() {
        when(repository.findById(id)).thenReturn(Optional.of(transaction));
        when(repository.save(transaction)).thenReturn(transaction);
        assertThat(service.confirmCategory(id, TransactionCategory.HEALTH)).isSameAs(transaction);
        assertThat(transaction.getCategorySource()).isEqualTo(CategorySource.USER);
        assertThat(transaction.getCategory()).isEqualTo(TransactionCategory.HEALTH);
        verifyNoInteractions(categorizer);
        verify(repository).save(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1826.97", "0.00"})
    void nonExpensesCannotReceiveSpendingCategoriesOrCallAi(String amount) {
        var incoming = new Transaction("Example employer", "EUR", "COMPLETED", new BigDecimal(amount),
            BigDecimal.ZERO, LocalDateTime.of(2026, 9, 28, 8, 0));
        incoming.assignCategory(TransactionCategory.GROCERIES, CategorySource.USER);
        when(repository.findById(id)).thenReturn(Optional.of(incoming));
        assertThatThrownBy(() -> service.confirmCategory(id, TransactionCategory.GROCERIES))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.enrich(id)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.enrich(incoming)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(categorizer);
        verify(repository, never()).save(any());
    }

    @Test
    void legacyIncorrectSpendingCategoryCanBeClearedOnIncome() {
        var incoming = new Transaction("Example employer", "EUR", "COMPLETED", new BigDecimal("1826.97"),
            BigDecimal.ZERO, LocalDateTime.of(2026, 9, 28, 8, 0));
        incoming.confirmCategory(TransactionCategory.GROCERIES);
        when(repository.findById(id)).thenReturn(Optional.of(incoming));
        when(repository.save(incoming)).thenReturn(incoming);
        assertThat(service.confirmCategory(id, TransactionCategory.UNCATEGORIZED).getCategory())
            .isEqualTo(TransactionCategory.UNCATEGORIZED);
        verifyNoInteractions(categorizer);
    }
}
