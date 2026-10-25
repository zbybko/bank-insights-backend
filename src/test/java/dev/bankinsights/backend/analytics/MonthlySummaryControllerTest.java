package dev.bankinsights.backend.analytics;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MonthlySummaryControllerTest {
    private final MonthlySummaryService service = mock(MonthlySummaryService.class);
    private final org.springframework.test.web.servlet.MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new MonthlySummaryController(service)).build();

    @Test
    void validMonthReturnsSummary() throws Exception {
        when(service.summarize(YearMonth.of(2026, 10))).thenReturn(new MonthlySummary("2026-10", List.of()));
        mvc.perform(get("/api/transactions/summary").param("month", "2026-10"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.month").value("2026-10"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-13", "2026-1", "yesterday", ""})
    void invalidMonthReturns400(String month) throws Exception {
        mvc.perform(get("/api/transactions/summary").param("month", month)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
