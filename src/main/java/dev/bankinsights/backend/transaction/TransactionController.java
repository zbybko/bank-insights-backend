package dev.bankinsights.backend.transaction;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.bankinsights.backend.categorization.TransactionEnrichmentService;

@RestController
public class TransactionController {

    private final TransactionRepository repository;
    private final TransactionEnrichmentService enrichmentService;


    public TransactionController(
        TransactionRepository repository,
        TransactionEnrichmentService enrichmentService
    ) {
        this.repository = repository;
        this.enrichmentService = enrichmentService;
    }

    @GetMapping("/api/transactions")
    public List<Transaction> getTransactions() {
        return repository.findAllByOrderByBookingDateDescIdDesc();
    }

    @PostMapping("/api/transactions/{id}/enrichment")
    public Transaction enrichTransaction(@PathVariable UUID id) {
        return enrichmentService.enrich(id);
    }

    @PatchMapping("/api/transactions/{id}/category")
    public Transaction updateCategory(
        @PathVariable UUID id,
        @RequestBody CategoryUpdateRequest request
    ) {
        if (request.category() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Category is required"
            );
        }

        return enrichmentService.confirmCategory(id, request.category());
    }
}
