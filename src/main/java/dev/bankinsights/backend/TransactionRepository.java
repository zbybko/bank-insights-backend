package dev.bankinsights.backend;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID> {
}