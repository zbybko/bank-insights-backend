package dev.bankinsights.backend;

import org.springframework.stereotype.Service;

@Service
public class TransactionEnrichmentService {

    private final TransactionCategorizer categorizer;

    public TransactionEnrichmentService(
        TransactionCategorizer categorizer
    ) {
        this.categorizer = categorizer;
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

        transaction.assignCategory(
            suggestion.category(),
            CategorySource.AI
        );

        return suggestion;
    }
}