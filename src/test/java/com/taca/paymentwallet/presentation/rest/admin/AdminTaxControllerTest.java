package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import com.taca.paymentwallet.application.port.in.GetAdminTaxConfigsUseCase;
import com.taca.paymentwallet.application.result.GetAdminTaxConfigResult;
import com.taca.paymentwallet.application.security.FinanceOpsPolicy;
import com.taca.paymentwallet.presentation.rest.GlobalRestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminTaxControllerTest {

    private GetAdminTaxConfigsUseCase getAdminTaxConfigsUseCase;
    private FinanceOpsPolicy financeOpsPolicy;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        getAdminTaxConfigsUseCase = mock(GetAdminTaxConfigsUseCase.class);
        financeOpsPolicy = mock(FinanceOpsPolicy.class);

        AdminTaxController controller = new AdminTaxController(
                getAdminTaxConfigsUseCase,
                financeOpsPolicy
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnTaxConfigs() throws Exception {
        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        when(getAdminTaxConfigsUseCase.execute()).thenReturn(
                List.of(
                        new GetAdminTaxConfigResult(
                                id,
                                "PLATFORM",
                                null,
                                100,
                                Instant.parse("2026-10-01T00:00:00Z"),
                                "Platform tax",
                                createdBy,
                                Instant.parse("2026-09-30T00:00:00Z")
                        )
                )
        );

        mockMvc.perform(
                        get("/api/v1/admin/taxes")
                                .header("X-Request-ID", "req-admin-taxes-001")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data[0].scope").value("PLATFORM"))
                .andExpect(jsonPath("$.data[0].rate_bps").value(100))
                .andExpect(jsonPath("$.data[0].effective_from")
                        .value("2026-10-01T00:00:00Z"))
                .andExpect(jsonPath("$.data[0].note")
                        .value("Platform tax"))
                .andExpect(jsonPath("$.data[0].created_by")
                        .value(createdBy.toString()))
                .andExpect(jsonPath("$.data[0].created_at")
                        .value("2026-09-30T00:00:00Z"))
                .andExpect(jsonPath("$.meta.request_id")
                        .value("req-admin-taxes-001"));

        verify(financeOpsPolicy).requireFinanceOps();
        verify(getAdminTaxConfigsUseCase).execute();
    }

    @Test
    void shouldReturnEmptyList() throws Exception {
        when(getAdminTaxConfigsUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/admin/taxes")
                                .header("X-Request-ID", "req-admin-taxes-empty")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(financeOpsPolicy).requireFinanceOps();
        verify(getAdminTaxConfigsUseCase).execute();
    }

    @Test
    void shouldRequireRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/admin/taxes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("MISSING_REQUIRED_HEADER"));

        verifyNoInteractions(financeOpsPolicy);
        verifyNoInteractions(getAdminTaxConfigsUseCase);
    }

    @Test
    void shouldRejectAnonymousActor() throws Exception {
        doThrow(new UnauthenticatedException())
                .when(financeOpsPolicy)
                .requireFinanceOps();

        mockMvc.perform(
                        get("/api/v1/admin/taxes")
                                .header("X-Request-ID", "req-admin-taxes-anonymous")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_UNAUTHENTICATED"));

        verifyNoInteractions(getAdminTaxConfigsUseCase);
    }

    @Test
    void shouldRejectNonFinanceOpsActor() throws Exception {
        doThrow(new ForbiddenException())
                .when(financeOpsPolicy)
                .requireFinanceOps();

        mockMvc.perform(
                        get("/api/v1/admin/taxes")
                                .header("X-Request-ID", "req-admin-taxes-forbidden")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_FORBIDDEN"));

        verifyNoInteractions(getAdminTaxConfigsUseCase);
    }

    @Test
    void shouldAuthorizeBeforeReadingTaxConfigs() throws Exception {
        when(getAdminTaxConfigsUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/admin/taxes")
                                .header("X-Request-ID", "req-admin-taxes-order")
                )
                .andExpect(status().isOk());

        var inOrder = inOrder(
                financeOpsPolicy,
                getAdminTaxConfigsUseCase
        );

        inOrder.verify(financeOpsPolicy).requireFinanceOps();
        inOrder.verify(getAdminTaxConfigsUseCase).execute();
    }
}