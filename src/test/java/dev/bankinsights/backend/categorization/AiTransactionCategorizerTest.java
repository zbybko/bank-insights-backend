package dev.bankinsights.backend.categorization;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import dev.bankinsights.backend.integration.astropods.AstropodsClient;
import dev.bankinsights.backend.transaction.Transaction;
import dev.bankinsights.backend.transaction.TransactionCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class AiTransactionCategorizerTest {
    private final AstropodsClient client = mock(AstropodsClient.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final AiTransactionCategorizer categorizer = new AiTransactionCategorizer(client, mapper);
    private final Transaction transaction = new Transaction("Example marketplace", "EUR", "COMPLETED",
        new BigDecimal("-42.50"), BigDecimal.ZERO, LocalDateTime.of(2026, 10, 1, 12, 0));

    @Test
    void parsesUncertainSuggestionWithQuestion() {
        reply("""
            {"category":"UNCATEGORIZED","explanation":"Purchase details are unknown.",
             "reviewRequired":true,"question":"What did you buy?"}
            """);
        var result = categorizer.suggestCategory(transaction);
        assertThat(result.category()).isEqualTo(TransactionCategory.UNCATEGORIZED);
        assertThat(result.reviewRequired()).isTrue();
        assertThat(result.question()).isEqualTo("What did you buy?");
    }

    @Test
    void inputContainsOnlyDescriptionAndExactMoneyContext() {
        reply("""
            {"category":"GROCERIES","explanation":"Grocery purchase.","reviewRequired":false,"question":""}
            """);
        categorizer.suggestCategory(transaction);
        var input = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(client).ask(anyString(), input.capture());
        var json = mapper.readTree(input.getValue());
        assertThat(json.size()).isEqualTo(3);
        assertThat(json.path("amount").asString()).isEqualTo("-42.50");
        assertThat(json.path("currency").asString()).isEqualTo("EUR");
        assertThat(json.path("description").asString()).isEqualTo("Example marketplace");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"category\":\"GROCERIES\",\"explanation\":\"x\",\"reviewRequired\":false}",
        "{\"category\":\"GROCERIES\",\"explanation\":\"x\",\"reviewRequired\":\"false\",\"question\":\"\"}",
        "{\"category\":\"GROCERIES\",\"explanation\":\" \",\"reviewRequired\":false,\"question\":\"\"}",
        "{\"category\":\"GROCERIES\",\"explanation\":\"x\",\"reviewRequired\":true,\"question\":\"\"}",
        "{\"category\":\"UNCATEGORIZED\",\"explanation\":\"x\",\"reviewRequired\":false,\"question\":\"\"}",
        "{\"category\":\"GROCERIES\",\"explanation\":\"x\",\"reviewRequired\":false,\"question\":\"Why?\"}",
        "{\"category\":\"GROCERIES\",\"explanation\":\"x\",\"reviewRequired\":false,\"question\":\"\",\"extra\":1}",
        "{\"category\":\"UNKNOWN_CATEGORY\",\"explanation\":\"x\",\"reviewRequired\":false,\"question\":\"\"}",
        "```json\n{}\n```"
    })
    void rejectsInvalidOrInconsistentModelOutput(String content) {
        reply(content);
        assertThatThrownBy(() -> categorizer.suggestCategory(transaction)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsEnvelopeWithoutTextContent() {
        when(client.ask(anyString(), anyString())).thenReturn("{\"choices\":[]}");
        assertThatThrownBy(() -> categorizer.suggestCategory(transaction))
            .isInstanceOf(IllegalStateException.class).hasMessage("AI response has no text content");
    }

    private void reply(String content) {
        when(client.ask(anyString(), anyString())).thenReturn(mapper.writeValueAsString(
            Map.of("choices", List.of(Map.of("message", Map.of("content", content))))));
    }
}
