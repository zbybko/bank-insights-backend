package dev.bankinsights.backend.transaction;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import dev.bankinsights.backend.categorization.TransactionEnrichmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TransactionControllerTest {
    private final TransactionRepository repository = mock(TransactionRepository.class);
    private final TransactionEnrichmentService service = mock(TransactionEnrichmentService.class);
    private final UUID id = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TransactionController(repository, service)).build();
    }

    @Test
    void listSerializesExactMoneyAndReviewFields() throws Exception {
        var t = transaction();
        t.confirmCategory(TransactionCategory.GROCERIES);
        when(repository.findAllByOrderByBookingDateDescIdDesc()).thenReturn(List.of(t));
        mvc.perform(get("/api/transactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id.toString()))
            .andExpect(jsonPath("$[0].amount").value("-3.75"))
            .andExpect(jsonPath("$[0].fee").value("0.00"))
            .andExpect(jsonPath("$[0].categorySource").value("USER"))
            .andExpect(jsonPath("$[0].categoryReviewRequired").value(false));
        verifyNoInteractions(service);
    }

    @Test
    void categoryPatchBindsRequestAndReturnsSavedResult() throws Exception {
        var t = transaction();
        t.confirmCategory(TransactionCategory.GROCERIES);
        when(service.confirmCategory(id, TransactionCategory.GROCERIES)).thenReturn(t);
        mvc.perform(patch("/api/transactions/{id}/category", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"GROCERIES\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.category").value("GROCERIES"))
            .andExpect(jsonPath("$.categorySource").value("USER"))
            .andExpect(jsonPath("$.categoryQuestion").value(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"category\":null}", "{\"category\":\"INVALID\"}", "not-json"})
    void invalidCategoryRequestIs400WithoutServiceCall(String body) throws Exception {
        mvc.perform(patch("/api/transactions/{id}/category", id)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void invalidIdIs400() throws Exception {
        mvc.perform(post("/api/transactions/not-a-uuid/enrichment")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void missingTransactionIs404ForBothMutations() throws Exception {
        when(service.enrich(id)).thenThrow(new TransactionNotFoundException());
        when(service.confirmCategory(id, TransactionCategory.GROCERIES)).thenThrow(new TransactionNotFoundException());
        mvc.perform(post("/api/transactions/{id}/enrichment", id)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/transactions/{id}/category", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"GROCERIES\"}"))
            .andExpect(status().isNotFound());
    }

    private Transaction transaction() {
        var t = new Transaction("Example shop", "EUR", "COMPLETED", new BigDecimal("-3.75"),
            new BigDecimal("0.00"), LocalDateTime.of(2026, 10, 1, 12, 0));
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }
}
