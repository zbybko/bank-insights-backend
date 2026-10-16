package dev.bankinsights.backend;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController 
public class TransactionController {
    
    private final RevolutCsvParser parser;
    private final Path csvPath;

    public TransactionController(
        RevolutCsvParser parser,
        @Value("${bankinsights.csv-path}") String csvPath
    ) {
        this.parser = parser;
        this.csvPath = Path.of(csvPath);
    }

    @GetMapping("/api/transactions")
    public List<Transaction> getTransactions() throws IOException {
        return parser.parse(csvPath);
    }
}
