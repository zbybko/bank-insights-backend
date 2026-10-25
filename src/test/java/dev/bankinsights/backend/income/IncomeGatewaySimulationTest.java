package dev.bankinsights.backend.income;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tools.jackson.databind.json.JsonMapper;
import dev.bankinsights.backend.integration.astropods.AstropodsClient;
import dev.bankinsights.backend.transaction.Transaction;

@EnabledIfSystemProperty(named = "income.simulation.url", matches = ".+")
class IncomeGatewaySimulationTest {
    @Test
    void realHttpClientSendsMemoryAndParsesGatewayEnvelope() {
        var client = new AstropodsClient(System.getProperty("income.simulation.url"), "synthetic-test-key", "test-model");
        var classifier = new AiIncomeClassifier(client, JsonMapper.builder().build());
        var previous = new Transaction("Synthetic Employer", "EUR", "COMPLETED", new BigDecimal("1800.00"),
            BigDecimal.ZERO, LocalDateTime.of(2026, 8, 27, 12, 0));
        var confirmed = new IncomeAssessment(previous);
        confirmed.confirm(IncomeType.SALARY);
        var current = new Transaction("Synthetic Employer", "EUR", "COMPLETED", new BigDecimal("1850.00"),
            BigDecimal.ZERO, LocalDateTime.of(2026, 9, 27, 12, 0));
        var result = classifier.suggest(current, List.of(confirmed));
        assertThat(result.type()).isEqualTo(IncomeType.SALARY);
        assertThat(result.reviewRequired()).isTrue();
        assertThat(result.question()).contains("main salary");
    }
}
