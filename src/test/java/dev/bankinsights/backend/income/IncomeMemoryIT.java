package dev.bankinsights.backend.income;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import dev.bankinsights.backend.transaction.Transaction;
import dev.bankinsights.backend.transaction.TransactionRepository;

@SpringBootTest(properties = {
    "bankinsights.csv-path=unused.csv", "bankinsights.ai.gateway-url=http://127.0.0.1:1",
    "bankinsights.ai.api-key=test-only", "spring.devtools.restart.enabled=false"
})
@Testcontainers
class IncomeMemoryIT {
    @Container static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired TransactionRepository transactions;
    @Autowired IncomeAssessmentRepository assessments;
    @Autowired IncomeService service;
    @MockitoBean AiIncomeClassifier classifier;

    @BeforeEach
    void resetData() { assessments.deleteAll(); transactions.deleteAll(); }

    @Test
    @SuppressWarnings("unchecked")
    void onlyEarlierUserConfirmedMatchingCurrencyAndDescriptionBecomeMemory() {
        var first = save("Example Employer", "EUR", "2026-08-27T12:00:00", "1800.00");
        service.confirm(first.getId(), IncomeType.SALARY);
        var otherCurrency = save("Example Employer", "USD", "2026-08-27T12:00:00", "1800.00");
        service.confirm(otherCurrency.getId(), IncomeType.BONUS);
        var otherName = save("Different Employer", "EUR", "2026-08-27T12:00:00", "1800.00");
        service.confirm(otherName.getId(), IncomeType.SALARY);
        var future = save("Example Employer", "EUR", "2026-10-27T12:00:00", "1800.00");
        service.confirm(future.getId(), IncomeType.SALARY);
        var aiOnly = save("Example Employer", "EUR", "2026-08-28T12:00:00", "50.00");
        var ai = new IncomeAssessment(aiOnly);
        ai.apply(new IncomeSuggestion(IncomeType.OTHER, "Not confirmed", false, ""));
        assessments.saveAndFlush(ai);
        var current = save(" example employer ", "EUR", "2026-09-27T12:00:00", "1826.97");
        when(classifier.suggest(any(), anyList())).thenReturn(new IncomeSuggestion(
            IncomeType.SALARY, "Earlier salary exists", true, "Main salary?"));
        var result = service.enrich(current.getId());
        ArgumentCaptor<List<IncomeAssessment>> memory = ArgumentCaptor.forClass(List.class);
        verify(classifier).suggest(any(), memory.capture());
        assertThat(memory.getValue()).hasSize(1);
        assertThat(memory.getValue().getFirst().view().transactionId()).isEqualTo(first.getId());
        assertThat(result.type()).isEqualTo(IncomeType.UNKNOWN);
        assertThat(result.suggestedType()).isEqualTo(IncomeType.SALARY);
        var confirmed = service.confirm(current.getId(), IncomeType.BONUS);
        assertThat(confirmed.source()).isEqualTo(IncomeAssessment.Source.USER);
        assertThat(service.enrich(current.getId()).type()).isEqualTo(IncomeType.BONUS);
        assertThat(service.get(current.getId()).suggestedType()).isEqualTo(IncomeType.SALARY);
        verify(classifier, times(1)).suggest(any(), anyList());
    }

    @Test
    void rejectsOutgoingAndDoesNotPersistFailedAiResults() {
        var outgoing = save("Shop", "EUR", "2026-09-27T12:00:00", "-10.00");
        assertThatThrownBy(() -> service.enrich(outgoing.getId())).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(classifier);
        var incoming = save("Unknown", "EUR", "2026-09-27T12:00:00", "10.00");
        when(classifier.suggest(any(), anyList())).thenThrow(new IllegalStateException("Bad AI response"));
        assertThatThrownBy(() -> service.enrich(incoming.getId())).isInstanceOf(IllegalStateException.class);
        assertThat(assessments.count()).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void historyIsBoundedAndUsesMostRecentConfirmations() {
        for (int day = 1; day <= 12; day++) {
            var t = save("Example Sender", "EUR", "2026-08-%02dT12:00:00".formatted(day), "100.00");
            service.confirm(t.getId(), IncomeType.PERSONAL_TRANSFER);
        }
        var current = save("Example Sender", "EUR", "2026-09-27T12:00:00", "100.00");
        when(classifier.suggest(any(), anyList())).thenReturn(new IncomeSuggestion(
            IncomeType.UNKNOWN, "Purpose unclear", true, "What was this transfer for?"));
        service.enrich(current.getId());
        ArgumentCaptor<List<IncomeAssessment>> memory = ArgumentCaptor.forClass(List.class);
        verify(classifier).suggest(any(), memory.capture());
        assertThat(memory.getValue()).hasSize(10);
        assertThat(memory.getValue().getFirst().getTransaction().getBookingDate().getDayOfMonth()).isEqualTo(12);
        assertThat(memory.getValue().getLast().getTransaction().getBookingDate().getDayOfMonth()).isEqualTo(3);
    }

    private Transaction save(String name, String currency, String date, String amount) {
        var t = new Transaction(name, currency, "COMPLETED", new BigDecimal(amount), BigDecimal.ZERO,
            LocalDateTime.parse(date));
        t.setSourceDetails("Transfer", "Current");
        var saved = transactions.saveAndFlush(t);
        assertThat(transactions.findById(saved.getId()).orElseThrow().getSourceProduct()).isEqualTo("Current");
        return saved;
    }
}
