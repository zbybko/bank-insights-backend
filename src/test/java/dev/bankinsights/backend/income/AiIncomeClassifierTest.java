package dev.bankinsights.backend.income;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import dev.bankinsights.backend.integration.astropods.AstropodsClient;
import dev.bankinsights.backend.transaction.Transaction;

class AiIncomeClassifierTest {
    private final AstropodsClient client = mock(AstropodsClient.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final AiIncomeClassifier classifier = new AiIncomeClassifier(client, mapper);

    @Test
    void passesConfirmedHistoryAndSourceFieldsWithoutLosingMoneyPrecision() {
        var previous = new IncomeAssessment(transaction());
        previous.confirm(IncomeType.SALARY);
        reply("{\"type\":\"SALARY\",\"explanation\":\"Familiar employer.\",\"reviewRequired\":true,\"question\":\"Main salary?\"}");
        var result = classifier.suggest(transaction(), List.of(previous));
        var input = ArgumentCaptor.forClass(String.class);
        verify(client).ask(anyString(), input.capture());
        var data = mapper.readTree(input.getValue());
        assertThat(data.path("confirmedHistory").path(0).path("confirmedType").asString()).isEqualTo("SALARY");
        assertThat(data.path("transaction").path("amount").asString()).isEqualTo("1826.97");
        assertThat(data.path("transaction").path("sourceProduct").asString()).isEqualTo("Current");
        assertThat(result.reviewRequired()).isTrue();
    }

    @Test
    void payrollCannotSilentlyBecomeConfirmedEvenIfModelIgnoresInstructions() {
        reply("{\"type\":\"SALARY\",\"explanation\":\"Employer.\",\"reviewRequired\":false,\"question\":\"\"}");
        var assessment = new IncomeAssessment(transaction());
        assessment.apply(classifier.suggest(transaction(), List.of()));
        assertThat(assessment.view().type()).isEqualTo(IncomeType.UNKNOWN);
        assertThat(assessment.view().suggestedType()).isEqualTo(IncomeType.SALARY);
        assertThat(assessment.view().question()).isNotBlank();
    }

    @Test
    void applyingAiNeverOverwritesUserConfirmation() {
        var assessment = new IncomeAssessment(transaction());
        assessment.confirm(IncomeType.INTERNAL_TRANSFER);
        assessment.apply(new IncomeSuggestion(IncomeType.SALARY, "Guess", false, ""));
        assertThat(assessment.getType()).isEqualTo(IncomeType.INTERNAL_TRANSFER);
        assertThat(assessment.getSource()).isEqualTo(IncomeAssessment.Source.USER);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"type\":\"INVALID\",\"explanation\":\"x\",\"reviewRequired\":false,\"question\":\"\"}",
        "{\"type\":\"UNKNOWN\",\"explanation\":\"x\",\"reviewRequired\":false,\"question\":\"\"}",
        "{\"type\":\"SALARY\",\"explanation\":\"x\",\"reviewRequired\":true,\"question\":\"\"}",
        "{\"type\":\"REFUND\",\"explanation\":\"x\",\"reviewRequired\":false,\"question\":\"Why?\"}",
        "{\"type\":\"REFUND\",\"explanation\":\" \",\"reviewRequired\":false,\"question\":\"\"}"
    })
    void rejectsUnusableModelResponses(String content) {
        reply(content);
        assertThatThrownBy(() -> classifier.suggest(transaction(), List.of())).isInstanceOf(RuntimeException.class);
    }

    private Transaction transaction() {
        var t = new Transaction("Example Employer", "EUR", "COMPLETED", new BigDecimal("1826.97"),
            BigDecimal.ZERO, LocalDateTime.of(2026, 9, 27, 12, 0));
        t.setSourceDetails("Transfer", "Current");
        return t;
    }

    private void reply(String content) {
        when(client.ask(anyString(), anyString())).thenReturn(mapper.writeValueAsString(
            Map.of("choices", List.of(Map.of("message", Map.of("content", content))))));
    }
}
