package dev.bankinsights.backend;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class Transaction {
    private TransactionCategory category = TransactionCategory.UNCATEGORIZED;
    private CategorySource categorySource = CategorySource.UNCLASSIFIED;

    private String description;
    private String currency;
    private String state;
    private BigDecimal amount;
    private BigDecimal fee;
    private LocalDateTime bookingDate;

    public Transaction(
        String description, 
        String currency,
        String state,
        BigDecimal amount,
        BigDecimal fee,
        LocalDateTime bookingDate
    ) {
        this.description = description;
        this.currency = currency;
        this.state = state;
        this.amount = amount;
        this.fee = fee;
        this.bookingDate = bookingDate;
    }

    public String getDescription() {
        return description;
    }

    public String getCurrency() {
        return currency;
    }

    public String getState() {
        return state;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public BigDecimal getAmount() {
        return amount;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public BigDecimal getFee() {
        return fee;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public LocalDateTime getBookingDate() {
        return bookingDate;
    }
    public TransactionCategory getCategory() {
        return category;
    }

    public CategorySource getCategorySource() {
        return categorySource;
    }

    public void assignCategory(
        TransactionCategory category,
        CategorySource source
    ) {
        this.category = java.util.Objects.requireNonNull(category);
        this.categorySource = java.util.Objects.requireNonNull(source);
    }
}
