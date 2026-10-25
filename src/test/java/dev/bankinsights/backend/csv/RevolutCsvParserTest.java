package dev.bankinsights.backend.csv;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

class RevolutCsvParserTest {
    private final RevolutCsvParser parser = new RevolutCsvParser();

    @Test
    void completedRowsPreserveExactMoneyUtf8AndQuotedDescriptions() throws Exception {
        var transactions = parser.parse(fixture());
        assertThat(transactions).hasSize(2);
        assertThat(transactions.get(0).getDescription()).isEqualTo("Example Café");
        assertThat(transactions.get(0).getBookingDate()).isEqualTo(LocalDateTime.of(2026, 10, 3, 9, 1));
        var shop = transactions.get(1);
        assertThat(shop.getDescription()).isEqualTo("Example shop, downtown");
        assertThat(shop.getAmount()).isEqualByComparingTo("-42.50");
        assertThat(shop.getFee()).isEqualByComparingTo("0.25");
        assertThat(shop.getCurrency()).isEqualTo("EUR");
        assertThat(shop.getSourceType()).isEqualTo("CARD_PAYMENT");
        assertThat(shop.getSourceProduct()).isEqualTo("Current");
    }

    @Test
    void impossibleCompletedDateIsRejected() throws Exception {
        String csv = new String(fixture(), StandardCharsets.UTF_8)
            .replace("2026-10-01 09:01:00", "2026-02-30 09:01:00");
        assertThatThrownBy(() -> parser.parse(csv.getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void missingRequiredHeaderIsRejected() {
        assertThatThrownBy(() -> parser.parse("Description\nExample\n".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private byte[] fixture() throws Exception {
        try (var stream = getClass().getResourceAsStream("/fixtures/revolut-synthetic.csv")) {
            return stream.readAllBytes();
        }
    }
}
