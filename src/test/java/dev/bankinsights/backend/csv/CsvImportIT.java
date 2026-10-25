package dev.bankinsights.backend.csv;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.bankinsights.backend.categorization.TransactionEnrichmentService;
import dev.bankinsights.backend.integration.astropods.AstropodsClient;
import dev.bankinsights.backend.transaction.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
    "bankinsights.csv-path=unused-in-tests.csv",
    "bankinsights.ai.gateway-url=http://127.0.0.1:1",
    "bankinsights.ai.api-key=test-only",
    "spring.devtools.restart.enabled=false"
})
@Testcontainers
class CsvImportIT {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired dev.bankinsights.backend.analytics.MonthlySummaryService summaries;
    @Autowired CsvImportService service;
    @Autowired TransactionRepository transactions;
    @Autowired CsvImportRepository imports;
    @Autowired TransactionEnrichmentService enrichment;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AstropodsClient client;
    @TempDir Path directory;

    @BeforeEach
    void clearTestDatabase() {
        transactions.deleteAll();
        imports.deleteAll();
    }

    @Test
    void migrationsCreateValidSchemaAndIdenticalBytesImportOnlyOnce() throws Exception {
        Path file = fixture();
        var first = service.importFile(file);
        var t = transactions.findAllByOrderByBookingDateDescIdDesc().getFirst();
        enrichment.confirmCategory(t.getId(), TransactionCategory.GROCERIES);
        Path renamed = Files.copy(file, directory.resolve("renamed.csv"));
        var second = service.importFile(renamed);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(first.getTransactionCount()).isEqualTo(2);
        assertThat(first.getFileHash()).hasSize(64);
        assertThat(transactions.count()).isEqualTo(2);
        assertThat(imports.count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from transactions where csv_import_id = ?", Long.class,
            first.getId())).isEqualTo(2);
        var loaded = transactions.findById(t.getId()).orElseThrow();
        assertThat(loaded.getCategory()).isEqualTo(TransactionCategory.GROCERIES);
        assertThat(loaded.getCategorySource()).isEqualTo(CategorySource.USER);
        assertThat(loaded.getCategoryReviewRequired()).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success = true and version in ('1','2','3')", Long.class))
            .isEqualTo(3);
    }

    @Test
    void failedTransactionInsertRollsBackImportAndAllRows() throws Exception {
        Path file = fixture();
        Files.writeString(file, Files.readString(file).replace("Example Café,-3.75,0.00,EUR",
            "Example Café,-3.75,0.00,TOOLONG"), StandardCharsets.UTF_8);
        assertThatThrownBy(() -> service.importFile(file)).isInstanceOf(RuntimeException.class);
        assertThat(imports.count()).isZero();
        assertThat(transactions.count()).isZero();
    }

    @Test
    void reviewedSuggestionSurvivesDatabaseRoundTrip() throws Exception {
        service.importFile(fixture());
        var t = transactions.findAll().getFirst();
        t.applyAiSuggestion(new dev.bankinsights.backend.categorization.CategorySuggestion(
            TransactionCategory.SHOPPING, "Purchase unknown.", true, "What did you buy?"));
        transactions.saveAndFlush(t);
        var loaded = transactions.findById(t.getId()).orElseThrow();
        assertThat(loaded.getSuggestedCategory()).isEqualTo(TransactionCategory.SHOPPING);
        assertThat(loaded.getCategory()).isEqualTo(TransactionCategory.UNCATEGORIZED);
        assertThat(loaded.getCategoryQuestion()).isEqualTo("What did you buy?");
        assertThat(loaded.getCategorizedAt()).isNotNull();
    }

    @Test
    void summaryUsesCompletedRowsWithinExactMonthBoundaries() {
        var first = new Transaction("Start of month", "EUR", "COMPLETED", new java.math.BigDecimal("100.00"),
            java.math.BigDecimal.ZERO, java.time.LocalDateTime.of(2026, 10, 1, 0, 0));
        var last = new Transaction("End of month", "EUR", "COMPLETED", new java.math.BigDecimal("-3.75"),
            java.math.BigDecimal.ZERO, java.time.LocalDateTime.of(2026, 10, 31, 23, 59, 59));
        last.confirmCategory(TransactionCategory.HEALTH);
        var previous = new Transaction("Previous month", "EUR", "COMPLETED", new java.math.BigDecimal("50.00"),
            java.math.BigDecimal.ZERO, java.time.LocalDateTime.of(2026, 9, 30, 23, 59, 59));
        var next = new Transaction("Next month", "EUR", "COMPLETED", new java.math.BigDecimal("200.00"),
            java.math.BigDecimal.ZERO, java.time.LocalDateTime.of(2026, 11, 1, 0, 0));
        var pending = new Transaction("Pending", "EUR", "PENDING", new java.math.BigDecimal("-90.00"),
            java.math.BigDecimal.ZERO, java.time.LocalDateTime.of(2026, 10, 3, 0, 0));
        transactions.saveAllAndFlush(java.util.List.of(first, last, previous, next, pending));
        var summary = summaries.summarize(java.time.YearMonth.of(2026, 10)).currencies().getFirst();
        assertThat(summary.incoming()).isEqualTo("100.00");
        assertThat(summary.outgoing()).isEqualTo("3.75");
        assertThat(summary.transactionCount()).isEqualTo(2);
        assertThat(summary.categories().getFirst().category()).isEqualTo(TransactionCategory.HEALTH);
    }

    private Path fixture() throws Exception {
        Path path = directory.resolve("statement.csv");
        try (var input = getClass().getResourceAsStream("/fixtures/revolut-synthetic.csv")) {
            Files.copy(input, path);
        }
        return path;
    }
}
