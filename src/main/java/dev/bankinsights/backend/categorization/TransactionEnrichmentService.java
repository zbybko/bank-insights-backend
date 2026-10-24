package dev.bankinsights.backend.categorization;

import java.util.UUID;

import org.springframework.stereotype.Service;

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

        transaction.confirmCategory(category);

        return repository.save(transaction);
    }
}
