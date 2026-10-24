package dev.bankinsights.backend.dev;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import dev.bankinsights.backend.csv.CsvImportService;
import dev.bankinsights.backend.transaction.TransactionRepository;

@Component
@Profile("csv-import-check")
public class CsvImportCheck implements CommandLineRunner {

    private final CsvImportService importService;
    private final TransactionRepository transactionRepository;
    private final Path csvPath;

    public CsvImportCheck(
        CsvImportService importService,
        TransactionRepository transactionRepository,
        @Value("${bankinsights.csv-path}") String csvPath
    ) {
        this.importService = importService;
        this.transactionRepository = transactionRepository;
        this.csvPath = Path.of(csvPath);
    }

    @Override
    public void run(String... args) throws Exception {
        var first = importService.importFile(csvPath);
        long countAfterFirst = transactionRepository.count();

        var second = importService.importFile(csvPath);
        long countAfterSecond = transactionRepository.count();

        if (!first.getId().equals(second.getId())
            || countAfterFirst != countAfterSecond) {
            throw new IllegalStateException(
                "Repeated import created duplicates"
            );
        }

        System.out.println("Import ID: " + first.getId());
        System.out.println("Transactions after first: " + countAfterFirst);
        System.out.println("Transactions after second: " + countAfterSecond);
        System.out.println("Repeated import check passed");
    }
}
