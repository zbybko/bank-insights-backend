package dev.bankinsights.backend.categorization;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import dev.bankinsights.backend.transaction.CategorySource;
import dev.bankinsights.backend.transaction.Transaction;
import dev.bankinsights.backend.transaction.TransactionCategory;
import dev.bankinsights.backend.transaction.TransactionNotFoundException;
import dev.bankinsights.backend.transaction.TransactionRepository;

@Service
public class TransactionEnrichmentService {

    private final TransactionCategorizer categorizer;
    private final TransactionRepository repository;

    public TransactionEnrichmentService(
        TransactionCategorizer categorizer,
        TransactionRepository repository
    ) {
        this.categorizer = categorizer;
        this.repository = repository;
    }

    public CategorySuggestion enrich(Transaction transaction) {
        requireExpense(transaction);
        if (transaction.getCategorySource() == CategorySource.USER) {
            return new CategorySuggestion(
                transaction.getCategory(),
                "Category selected by the user.",
                false,
                ""
            );
        }

        CategorySuggestion suggestion =
            categorizer.suggestCategory(transaction);

        transaction.applyAiSuggestion(suggestion);

        return suggestion;
    }

    public Transaction enrich(UUID transactionId) {
        var transaction = repository.findById(transactionId)
            .orElseThrow(TransactionNotFoundException::new);

        requireExpense(transaction);
        if (transaction.getCategorySource() == CategorySource.USER
            || transaction.getCategorySource() == CategorySource.AI) {
            return transaction;
        }

        enrich(transaction);

        return repository.save(transaction);
    }

    public Transaction confirmCategory(
        UUID transactionId,
        TransactionCategory category
    ) {
        var transaction = repository.findById(transactionId)
            .orElseThrow(TransactionNotFoundException::new);

        // Permit clearing a legacy incorrect spending label on an incoming payment.
        if (category != TransactionCategory.UNCATEGORIZED) requireExpense(transaction);
        transaction.confirmCategory(category);

        return repository.save(transaction);
    }

    private void requireExpense(Transaction transaction) {
        if (transaction.getAmount().signum() >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Spending categories require a negative amount. Use /income for incoming transactions.");
        }
    }
}
