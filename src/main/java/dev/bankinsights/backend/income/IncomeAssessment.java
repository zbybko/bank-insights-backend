package dev.bankinsights.backend.income;

import java.time.Instant;
import java.util.UUID;

import dev.bankinsights.backend.transaction.Transaction;
import jakarta.persistence.*;

@Entity
@Table(name = "income_assessments")
public class IncomeAssessment {
    public enum Source { AI, USER }

    @Id
    private UUID transactionId;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
    @Version
    private long version;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IncomeType type = IncomeType.UNKNOWN;
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private IncomeType suggestedType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Source source;
    @Column(nullable = false, columnDefinition = "text")
    private String explanation;
    @Column(nullable = false)
    private boolean reviewRequired;
    @Column(nullable = false, columnDefinition = "text")
    private String question;
    @Column(nullable = false)
    private Instant updatedAt;

    protected IncomeAssessment() {}

    public IncomeAssessment(Transaction transaction) {
        this.transaction = java.util.Objects.requireNonNull(transaction);
    }

    public void apply(IncomeSuggestion suggestion) {
        if (source == Source.USER) return;
        suggestedType = suggestion.type();
        type = suggestion.reviewRequired() ? IncomeType.UNKNOWN : suggestion.type();
        source = Source.AI;
        explanation = suggestion.explanation();
        reviewRequired = suggestion.reviewRequired();
        question = suggestion.question();
        updatedAt = Instant.now();
    }

    public void confirm(IncomeType selected) {
        type = java.util.Objects.requireNonNull(selected);
        source = Source.USER;
        explanation = "Income type selected by the user.";
        reviewRequired = false;
        question = "";
        updatedAt = Instant.now();
    }

    public View view() {
        return new View(transactionId, type, suggestedType, source, explanation, reviewRequired, question, updatedAt);
    }

    public Source getSource() { return source; }
    public IncomeType getType() { return type; }
    public Transaction getTransaction() { return transaction; }

    public record View(UUID transactionId, IncomeType type, IncomeType suggestedType, Source source,
                       String explanation, boolean reviewRequired, String question, Instant updatedAt) {}
}
