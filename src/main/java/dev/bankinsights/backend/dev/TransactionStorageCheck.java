package dev.bankinsights.backend.dev;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import dev.bankinsights.backend.categorization.CategorySuggestion;
import dev.bankinsights.backend.transaction.Transaction;
import dev.bankinsights.backend.transaction.TransactionCategory;
import dev.bankinsights.backend.transaction.TransactionRepository;

@Component
@Profile("db-check")
public class TransactionStorageCheck implements CommandLineRunner {

    private final TransactionRepository repository;

    public TransactionStorageCheck(TransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        var transaction = new Transaction(
            "Demo grocery purchase",
            "EUR",
            "COMPLETED",
            new BigDecimal("-42.50"),
            BigDecimal.ZERO,
            LocalDateTime.of(2026, 10, 24, 12, 0)
        );

        transaction.applyAiSuggestion(new CategorySuggestion(
            TransactionCategory.GROCERIES,
            "Synthetic suggestion for checking database storage.",
            false,
            ""
        ));

        var saved = repository.save(transaction);

        var loaded = repository.findById(saved.getId())
            .orElseThrow();

        System.out.println("Saved transaction: " + loaded.getId());
        System.out.println("Description: " + loaded.getDescription());
    }
}
