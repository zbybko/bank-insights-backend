package dev.bankinsights.backend;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Component;

@Component 
public class RevolutCsvParser {
    public List<Transaction> parse(Path path) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        
        try (
            var reader = Files.newBufferedReader(
                path, StandardCharsets.UTF_8
            );

            var csv = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .get()
            .parse(reader)
        ) {
            for (var row: csv) {
                String state = row.get("State");

                if (!"COMPLETED".equals(state)) {
                    continue;
                }

                String description = row.get("Description");
                String currency = row.get("Currency");
                BigDecimal amount = new BigDecimal(row.get("Amount"));
                BigDecimal fee = new BigDecimal(row.get("Fee"));

                Transaction transaction = new Transaction(description, currency, state, amount, fee);

                transactions.add(transaction);
            }

            return transactions;
        }
    }
}
