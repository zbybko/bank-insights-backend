package dev.bankinsights.backend.csv;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Component;

import dev.bankinsights.backend.transaction.Transaction;

@Component
public class RevolutCsvParser {
    public List<Transaction> parse(byte[] content) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

        try (
            var reader = new InputStreamReader(
                new ByteArrayInputStream(content),
                StandardCharsets.UTF_8
            );

            var csv = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get()
                .parse(reader)
        ) {
            for (var row : csv) {
                String state = row.get("State");

                if (!"COMPLETED".equals(state)) {
                    continue;
                }

                String description = row.get("Description");
                String currency = row.get("Currency");
                BigDecimal amount = new BigDecimal(row.get("Amount"));
                BigDecimal fee = new BigDecimal(row.get("Fee"));
                LocalDateTime bookingDate = LocalDateTime.parse(
                    row.get("Completed Date"),
                    formatter
                );

                Transaction transaction = new Transaction(
                    description, currency, state, amount, fee, bookingDate
                );

                transactions.add(transaction);
                transaction.setSourceDetails(
                    row.isMapped("Type") ? row.get("Type") : null,
                    row.isMapped("Product") ? row.get("Product") : null
                );
            }

            transactions.sort(
                Comparator.comparing(
                    (Transaction transaction) -> transaction.getBookingDate()
                ).reversed()
            );

            return transactions;
        }
    }

    public List<Transaction> parse(Path path) throws IOException {
        return parse(Files.readAllBytes(path));
    }
}
