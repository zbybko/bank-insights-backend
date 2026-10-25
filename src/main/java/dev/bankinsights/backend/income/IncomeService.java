package dev.bankinsights.backend.income;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import dev.bankinsights.backend.transaction.Transaction;
import dev.bankinsights.backend.transaction.TransactionNotFoundException;
import dev.bankinsights.backend.transaction.TransactionRepository;

@Service
public class IncomeService {
    private final TransactionRepository transactions;
    private final IncomeAssessmentRepository assessments;
    private final AiIncomeClassifier classifier;

    public IncomeService(TransactionRepository transactions, IncomeAssessmentRepository assessments,
                         AiIncomeClassifier classifier) {
        this.transactions = transactions;
        this.assessments = assessments;
        this.classifier = classifier;
    }

    @Transactional(readOnly = true)
    public IncomeAssessment.View get(UUID id) {
        incoming(id);
        return assessments.findById(id).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Income has not been classified yet")).view();
    }

    @Transactional
    public IncomeAssessment.View enrich(UUID id) {
        var transaction = incoming(id);
        var existing = assessments.findById(id);
        // Both AI and user results are cached; explicit correction goes through confirm().
        if (existing.isPresent()) return existing.get().view();
        var history = assessments.findConfirmedHistory(IncomeAssessment.Source.USER,
            transaction.getDescription(), transaction.getCurrency(), transaction.getBookingDate(), PageRequest.of(0, 10));
        var suggestion = classifier.suggest(transaction, history);
        var assessment = new IncomeAssessment(transaction);
        assessment.apply(suggestion);
        return assessments.saveAndFlush(assessment).view();
    }

    @Transactional
    public IncomeAssessment.View confirm(UUID id, IncomeType type) {
        var transaction = incoming(id);
        if (type == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Income type is required");
        var assessment = assessments.findById(id).orElseGet(() -> new IncomeAssessment(transaction));
        assessment.confirm(type);
        return assessments.saveAndFlush(assessment).view();
    }

    private Transaction incoming(UUID id) {
        var transaction = transactions.findById(id).orElseThrow(TransactionNotFoundException::new);
        if (transaction.getAmount().signum() <= 0 || !"COMPLETED".equals(transaction.getState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A completed incoming transaction is required");
        }
        return transaction;
    }
}
