package dev.bankinsights.backend.income;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IncomeControllerTest {
    @Test
    void patchBindsIncomeTypeAndReturnsIndependentAssessment() throws Exception {
        var service = mock(IncomeService.class);
        var id = UUID.randomUUID();
        when(service.confirm(id, IncomeType.SALARY)).thenReturn(new IncomeAssessment.View(id,
            IncomeType.SALARY, null, IncomeAssessment.Source.USER, "Confirmed", false, "", Instant.now()));
        var mvc = MockMvcBuilders.standaloneSetup(new IncomeController(service)).build();
        mvc.perform(patch("/api/transactions/{id}/income", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"SALARY\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("SALARY"))
            .andExpect(jsonPath("$.source").value("USER"));
        mvc.perform(patch("/api/transactions/{id}/income", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"NOT_A_TYPE\"}")).andExpect(status().isBadRequest());
        verify(service, times(1)).confirm(id, IncomeType.SALARY);
    }
}
