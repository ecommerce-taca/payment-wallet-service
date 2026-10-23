package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import com.taca.paymentwallet.application.port.in.GetAdminFinanceSummaryUseCase;
import com.taca.paymentwallet.application.result.GetAdminFinanceSummaryResult;
import com.taca.paymentwallet.application.security.FinanceOpsPolicy;
import com.taca.paymentwallet.presentation.rest.GlobalRestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminFinanceSummaryControllerTest {

    private GetAdminFinanceSummaryUseCase useCase;
    private FinanceOpsPolicy financeOpsPolicy;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = mock(
                GetAdminFinanceSummaryUseCase.class
        );

        financeOpsPolicy = mock(
                FinanceOpsPolicy.class
        );

        AdminFinanceSummaryController controller =
                new AdminFinanceSummaryController(
                        useCase,
                        financeOpsPolicy
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalRestExceptionHandler()
                )
                .build();
    }

    @Test
    void shouldReturnFinanceSummary() throws Exception {
        when(useCase.execute()).thenReturn(
                summary()
        );

        mockMvc.perform(
                        get("/api/v1/admin/finance/summary")
                                .header(
                                        "X-Request-ID",
                                        "req-finance-summary-001"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.captured_amount")
                        .value(1_000_000))
                .andExpect(jsonPath("$.data.refunded_amount")
                        .value(100_000))
                .andExpect(jsonPath("$.data.gross_amount")
                        .value(900_000))
                .andExpect(jsonPath("$.data.commission_amount")
                        .value(90_000))
                .andExpect(jsonPath("$.data.tax_amount")
                        .value(45_000))
                .andExpect(jsonPath("$.data.seller_net_amount")
                        .value(765_000))
                .andExpect(jsonPath("$.data.available_balance")
                        .value(500_000))
                .andExpect(jsonPath("$.data.pending_balance")
                        .value(265_000))
                .andExpect(jsonPath("$.data.completed_payout_amount")
                        .value(200_000))
                .andExpect(jsonPath("$.data.as_of")
                        .value("2026-10-23T10:00:00Z"))
                .andExpect(jsonPath("$.meta.request_id")
                        .value("req-finance-summary-001"));

        verify(financeOpsPolicy)
                .requireFinanceOps();

        verify(useCase)
                .execute();
    }

    @Test
    void shouldRequireRequestId() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/finance/summary")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("MISSING_REQUIRED_HEADER"));

        verifyNoInteractions(financeOpsPolicy);
        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectAnonymousActor() throws Exception {
        doThrow(new UnauthenticatedException())
                .when(financeOpsPolicy)
                .requireFinanceOps();

        mockMvc.perform(
                        get("/api/v1/admin/finance/summary")
                                .header(
                                        "X-Request-ID",
                                        "req-finance-summary-anonymous"
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_UNAUTHENTICATED"));

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectNonFinanceOpsActor() throws Exception {
        doThrow(new ForbiddenException())
                .when(financeOpsPolicy)
                .requireFinanceOps();

        mockMvc.perform(
                        get("/api/v1/admin/finance/summary")
                                .header(
                                        "X-Request-ID",
                                        "req-finance-summary-forbidden"
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_FORBIDDEN"));

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldAuthorizeBeforeReadingSummary() throws Exception {
        when(useCase.execute()).thenReturn(
                summary()
        );

        mockMvc.perform(
                        get("/api/v1/admin/finance/summary")
                                .header(
                                        "X-Request-ID",
                                        "req-finance-summary-order"
                                )
                )
                .andExpect(status().isOk());

        InOrder inOrder =
                inOrder(
                        financeOpsPolicy,
                        useCase
                );

        inOrder.verify(financeOpsPolicy)
                .requireFinanceOps();

        inOrder.verify(useCase)
                .execute();
    }

    private GetAdminFinanceSummaryResult summary() {
        return new GetAdminFinanceSummaryResult(
                1_000_000L,
                100_000L,
                900_000L,
                90_000L,
                45_000L,
                765_000L,
                500_000L,
                265_000L,
                200_000L,
                Instant.parse(
                        "2026-10-23T10:00:00Z"
                )
        );
    }
}