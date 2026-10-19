package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import com.taca.paymentwallet.application.port.in.GetAdminFeeConfigsUseCase;
import com.taca.paymentwallet.application.result.GetAdminFeeConfigResult;
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

class AdminFeeControllerTest {

    private GetAdminFeeConfigsUseCase getAdminFeeConfigsUseCase;
    private FinanceOpsPolicy financeOpsPolicy;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        getAdminFeeConfigsUseCase = mock(GetAdminFeeConfigsUseCase.class);
        financeOpsPolicy = mock(FinanceOpsPolicy.class);

        AdminFeeController controller = new AdminFeeController(
                getAdminFeeConfigsUseCase,
                financeOpsPolicy
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnFeeConfigs() throws Exception {
        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        when(getAdminFeeConfigsUseCase.execute()).thenReturn(
                List.of(
                        new GetAdminFeeConfigResult(
                                id,
                                "PLATFORM",
                                null,
                                700,
                                Instant.parse("2026-10-01T00:00:00Z"),
                                "Platform commission",
                                createdBy,
                                Instant.parse("2026-09-30T00:00:00Z")
                        )
                )
        );

        mockMvc.perform(
                        get("/api/v1/admin/fees")
                                .header("X-Request-ID", "req-admin-fees-001")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data[0].scope").value("PLATFORM"))
                .andExpect(jsonPath("$.data[0].rate_bps").value(700))
                .andExpect(jsonPath("$.data[0].effective_from")
                        .value("2026-10-01T00:00:00Z"))
                .andExpect(jsonPath("$.data[0].note")
                        .value("Platform commission"))
                .andExpect(jsonPath("$.data[0].created_by")
                        .value(createdBy.toString()))
                .andExpect(jsonPath("$.data[0].created_at")
                        .value("2026-09-30T00:00:00Z"))
                .andExpect(jsonPath("$.meta.request_id")
                        .value("req-admin-fees-001"));

        verify(financeOpsPolicy).requireFinanceOps();
        verify(getAdminFeeConfigsUseCase).execute();
    }

    @Test
    void shouldReturnEmptyList() throws Exception {
        when(getAdminFeeConfigsUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/admin/fees")
                                .header("X-Request-ID", "req-admin-fees-empty")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(financeOpsPolicy).requireFinanceOps();
        verify(getAdminFeeConfigsUseCase).execute();
    }

    @Test
    void shouldRequireRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/admin/fees"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("MISSING_REQUIRED_HEADER"));

        verifyNoInteractions(financeOpsPolicy);
        verifyNoInteractions(getAdminFeeConfigsUseCase);
    }

    @Test
    void shouldRejectAnonymousActor() throws Exception {
        doThrow(new UnauthenticatedException())
                .when(financeOpsPolicy)
                .requireFinanceOps();

        mockMvc.perform(
                        get("/api/v1/admin/fees")
                                .header("X-Request-ID", "req-admin-fees-anonymous")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_UNAUTHENTICATED"));

        verifyNoInteractions(getAdminFeeConfigsUseCase);
    }

    @Test
    void shouldRejectNonFinanceOpsActor() throws Exception {
        doThrow(new ForbiddenException())
                .when(financeOpsPolicy)
                .requireFinanceOps();

        mockMvc.perform(
                        get("/api/v1/admin/fees")
                                .header("X-Request-ID", "req-admin-fees-forbidden")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_FORBIDDEN"));

        verifyNoInteractions(getAdminFeeConfigsUseCase);
    }

    @Test
    void shouldAuthorizeBeforeReadingFeeConfigs() throws Exception {
        when(getAdminFeeConfigsUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/admin/fees")
                                .header("X-Request-ID", "req-admin-fees-order")
                )
                .andExpect(status().isOk());

        var inOrder = inOrder(
                financeOpsPolicy,
                getAdminFeeConfigsUseCase
        );

        inOrder.verify(financeOpsPolicy).requireFinanceOps();
        inOrder.verify(getAdminFeeConfigsUseCase).execute();
    }
}