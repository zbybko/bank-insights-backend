package dev.bankinsights.backend.analytics;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MonthlySummaryController {
    private final MonthlySummaryService service;

    public MonthlySummaryController(MonthlySummaryService service) {
        this.service = service;
    }

    @GetMapping("/api/transactions/summary")
    public MonthlySummary summary(@RequestParam String month) {
        try {
            if (!month.matches("\\d{4}-\\d{2}")) throw new DateTimeParseException("Invalid month", month, 0);
            return service.summarize(YearMonth.parse(month));
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must use YYYY-MM format", ex);
        }
    }
}
