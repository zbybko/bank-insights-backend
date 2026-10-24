package dev.bankinsights.backend.csv;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CsvImportRepository
        extends JpaRepository<CsvImport, UUID> {

    Optional<CsvImport> findByFileHash(String fileHash);
}
