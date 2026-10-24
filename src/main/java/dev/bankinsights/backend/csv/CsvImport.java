package dev.bankinsights.backend.csv;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import dev.bankinsights.backend.transaction.Transaction;

@Entity
@Table(name = "csv_imports")
public class CsvImport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "file_hash", nullable = false, unique = true, length = 64)
    private String fileHash;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount;

    protected CsvImport() {
    }

    public CsvImport(String fileHash, int transactionCount) {
        this.fileHash = java.util.Objects.requireNonNull(fileHash);

        if (transactionCount < 0) {
            throw new IllegalArgumentException(
                "Transaction count must not be negative"
            );
        }

        this.transactionCount = transactionCount;
        this.importedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getFileHash() {
        return fileHash;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public int getTransactionCount() {
        return transactionCount;
    }
}
