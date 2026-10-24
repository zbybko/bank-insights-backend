package dev.bankinsights.backend;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 32)
    private String state;

    @Column(nullable = false, columnDefinition = "numeric")
    private BigDecimal amount;

    @Column(nullable = false, columnDefinition = "numeric")
    private BigDecimal fee;

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionCategory category = TransactionCategory.UNCATEGORIZED;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_source", nullable = false, length = 32)
    private CategorySource categorySource = CategorySource.UNCLASSIFIED;

    @Column(name = "category_explanation", columnDefinition = "text")
    private String categoryExplanation;

    @Column(name = "category_review_required")
    private Boolean categoryReviewRequired;

    @Column(name = "categorized_at")
    private Instant categorizedAt;

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

    protected Transaction() {
    }

    public UUID getId() {
        return id;
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

    public void applyAiSuggestion(CategorySuggestion suggestion) {
        java.util.Objects.requireNonNull(suggestion);

        assignCategory(suggestion.category(), CategorySource.AI);

        this.categoryExplanation = suggestion.explanation();
        this.categoryReviewRequired = suggestion.reviewRequired();
        this.categorizedAt = Instant.now();
    }
} 