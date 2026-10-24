package dev.bankinsights.backend;

import org.springframework.stereotype.Service;

import java.util.UUID;

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
                false
            );
        }

        CategorySuggestion suggestion =
            categorizer.suggestCategory(transaction);

        transaction.applyAiSuggestion(suggestion);

        return suggestion;
    }

    public Transaction enrich(UUID transactionId) {
        var transaction = repository.findById(transactionId)
            .orElseThrow(() ->
                new IllegalArgumentException("Transaction not found")
            );

        if (transaction.getCategorySource() == CategorySource.USER
            || transaction.getCategorySource() == CategorySource.AI) {
            return transaction;
        }

        enrich(transaction);

        return repository.save(transaction);
    }
}