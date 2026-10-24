package dev.bankinsights.backend;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("ai-check")
public class AiCategorizationCheck implements CommandLineRunner {

    private final TransactionEnrichmentService enrichmentService;

    public AiCategorizationCheck(TransactionEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
    }

    @Override
    public void run(String... args) {
        var transaction = new Transaction(
            "Lidl",
            "EUR",
            "COMPLETED",
            new BigDecimal("-42.50"),
            BigDecimal.ZERO,
            LocalDateTime.of(2026, 10, 24, 12, 0)
        );

        var suggestion = enrichmentService.enrich(transaction);

        System.out.println(suggestion);
        System.out.println(transaction.getCategory());
        System.out.println(transaction.getCategorySource());

        System.out.println(suggestion);
    }
}