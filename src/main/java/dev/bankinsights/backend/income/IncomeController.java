package dev.bankinsights.backend.income;

import java.util.UUID;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions/{id}/income")
public class IncomeController {
    private final IncomeService service;

    public IncomeController(IncomeService service) { this.service = service; }

    @GetMapping
    public IncomeAssessment.View get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping("/enrichment")
    public IncomeAssessment.View enrich(@PathVariable UUID id) { return service.enrich(id); }

    @PatchMapping
    public IncomeAssessment.View confirm(@PathVariable UUID id, @RequestBody Update request) {
        return service.confirm(id, request.type());
    }

    public record Update(IncomeType type) {}
}
