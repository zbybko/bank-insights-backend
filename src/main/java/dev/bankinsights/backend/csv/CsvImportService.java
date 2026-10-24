package dev.bankinsights.backend.csv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.bankinsights.backend.transaction.TransactionRepository;

@Service
public class CsvImportService {

    private final RevolutCsvParser parser;
    private final CsvImportRepository importRepository;
    private final TransactionRepository transactionRepository;

    public CsvImportService(
        RevolutCsvParser parser,
        CsvImportRepository importRepository,
        TransactionRepository transactionRepository
    ) {
        this.parser = parser;
        this.importRepository = importRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public CsvImport importFile(Path path)
            throws IOException, NoSuchAlgorithmException {

        byte[] content = Files.readAllBytes(path);

        byte[] hash = MessageDigest.getInstance("SHA-256")
            .digest(content);

        String fileHash = HexFormat.of().formatHex(hash);

        var existingImport = importRepository.findByFileHash(fileHash);

        if (existingImport.isPresent()) {
            return existingImport.get();
        }

        var transactions = parser.parse(content);

        var csvImport = new CsvImport(
            fileHash,
            transactions.size()
        );

        var savedImport = importRepository.saveAndFlush(csvImport);

        for (var transaction : transactions) {
            transaction.attachToImport(savedImport.getId());
        }

        transactionRepository.saveAll(transactions);

        return savedImport;
    }
}
