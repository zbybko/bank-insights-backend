package dev.bankinsights.backend.transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID> {
    List<Transaction> findAllByOrderByBookingDateDescIdDesc();

    List<Transaction> findByStateAndBookingDateGreaterThanEqualAndBookingDateLessThan(
        String state, LocalDateTime start, LocalDateTime end);
}
