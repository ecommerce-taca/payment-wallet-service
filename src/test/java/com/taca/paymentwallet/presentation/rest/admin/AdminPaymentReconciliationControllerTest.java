package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import com.taca.paymentwallet.application.port.in.GetAdminPaymentReconciliationUseCase;
import com.taca.paymentwallet.application.query.GetAdminPaymentReconciliationQuery;
import com.taca.paymentwallet.application.result.GetAdminPaymentReconciliationItemResult;
import com.taca.paymentwallet.application.result.GetAdminPaymentReconciliationResult;
import com.taca.paymentwallet.application.security.FinanceOpsPolicy;
import com.taca.paymentwallet.presentation.rest.GlobalRestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminPaymentReconciliationControllerTest {

    private GetAdminPaymentReconciliationUseCase useCase;
    private FinanceOpsPolicy financeOpsPolicy;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = mock(GetAdminPaymentReconciliationUseCase.class);
        financeOpsPolicy = mock(FinanceOpsPolicy.class);

        AdminPaymentReconciliationController controller =
                new AdminPaymentReconciliationController(
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
    void shouldReturnReconciliationPage() throws Exception {
        UUID paymentId = UUID.randomUUID();

        when(useCase.execute(any())).thenReturn(
                new GetAdminPaymentReconciliationResult(
                        List.of(
                                new GetAdminPaymentReconciliationItemResult(
                                        paymentId,
                                        "VNPAY",
                                        "SUCCESS",
                                        "SUCCESS",
                                        1_094_000L,
                                        true,
                                        Instant.parse(
                                                "2026-10-22T02:00:00Z"
                                        )
                                )
                        ),
                        1,
                        20,
                        1,
                        1,
                        0
                )
        );

        mockMvc.perform(
                        get("/api/v1/admin/payments/reconciliation")
                                .header(
                                        "X-Request-ID",
                                        "req-reconciliation-001"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()")
                        .value(1))
                .andExpect(jsonPath("$.data[0].payment_id")
                        .value(paymentId.toString()))
                .andExpect(jsonPath("$.data[0].provider")
                        .value("VNPAY"))
                .andExpect(jsonPath("$.data[0].local_status")
                        .value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].provider_status")
                        .value("success"))
                .andExpect(jsonPath("$.data[0].amount")
                        .value(1_094_000))
                .andExpect(jsonPath("$.data[0].match")
                        .value(true))
                .andExpect(jsonPath("$.data[0].checked_at")
                        .value("2026-10-22T02:00:00Z"))
                .andExpect(jsonPath("$.meta.request_id")
                        .value("req-reconciliation-001"))
                .andExpect(jsonPath("$.meta.page")
                        .value(1))
                .andExpect(jsonPath("$.meta.size")
                        .value(20))
                .andExpect(jsonPath("$.meta.total")
                        .value(1))
                .andExpect(jsonPath("$.meta.total_pages")
                        .value(1))
                .andExpect(jsonPath("$.meta.mismatch_count")
                        .value(0));

        verify(financeOpsPolicy).requireFinanceOps();
        verify(useCase).execute(any());
    }

    @Test
    void shouldUseDefaultPagination() throws Exception {
        when(useCase.execute(any())).thenReturn(
                new GetAdminPaymentReconciliationResult(
                        List.of(),
                        1,
                        20,
                        0,
                        0,
                        0
                )
        );

        mockMvc.perform(
                        get("/api/v1/admin/payments/reconciliation")
                                .header(
                                        "X-Request-ID",
                                        "req-reconciliation-default"
                                )
                )
                .andExpect(status().isOk());

        ArgumentCaptor<GetAdminPaymentReconciliationQuery> captor =
                ArgumentCaptor.forClass(
                        GetAdminPaymentReconciliationQuery.class
                );

        verify(useCase).execute(captor.capture());

        assertThat(captor.getValue().page())
                .isEqualTo(1);

        assertThat(captor.getValue().size())
                .isEqualTo(20);
    }

    @Test
    void shouldAcceptRequestedPageAndSize() throws Exception {
        when(useCase.execute(any())).thenReturn(
                new GetAdminPaymentReconciliationResult(
                        List.of(),
                        2,
                        50,
                        0,
                        0,
                        0
                )
        );

        mockMvc.perform(
                        get("/api/v1/admin/payments/reconciliation")
                                .header(
                                        "X-Request-ID",
                                        "req-reconciliation-page"
                                )
                                .param("page", "2")
                                .param("size", "50")
                )
                .andExpect(status().isOk());

        ArgumentCaptor<GetAdminPaymentReconciliationQuery> captor =
                ArgumentCaptor.forClass(
                        GetAdminPaymentReconciliationQuery.class
                );

        verify(useCase).execute(captor.capture());

        assertThat(captor.getValue().page())
                .isEqualTo(2);

        assertThat(captor.getValue().size())
                .isEqualTo(50);
    }

    @Test
    void shouldRequireRequestId() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/payments/reconciliation")
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
                        get("/api/v1/admin/payments/reconciliation")
                                .header(
                                        "X-Request-ID",
                                        "req-reconciliation-anonymous"
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
                        get("/api/v1/admin/payments/reconciliation")
                                .header(
                                        "X-Request-ID",
                                        "req-reconciliation-forbidden"
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_FORBIDDEN"));

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectInvalidPage() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/payments/reconciliation")
                                .header(
                                        "X-Request-ID",
                                        "req-reconciliation-page"
                                )
                                .param("page", "0")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("INVALID_REQUEST"));

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectNonNumericPage() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/payments/reconciliation")
                                .header(
                                        "X-Request-ID",
                                        "req-reconciliation-page"
                                )
                                .param("page", "abc")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("INVALID_REQUEST"));

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectOversizedPageSize() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/payments/reconciliation")
                                .header(
                                        "X-Request-ID",
                                        "req-reconciliation-size"
                                )
                                .param("size", "101")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("INVALID_REQUEST"));

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldAuthorizeBeforeReadingReconciliation() throws Exception {
        when(useCase.execute(any())).thenReturn(
                new GetAdminPaymentReconciliationResult(
                        List.of(),
                        1,
                        20,
                        0,
                        0,
                        0
                )
        );

        mockMvc.perform(
                        get("/api/v1/admin/payments/reconciliation")
                                .header(
                                        "X-Request-ID",
                                        "req-reconciliation-order"
                                )
                )
                .andExpect(status().isOk());

        var inOrder = inOrder(
                financeOpsPolicy,
                useCase
        );

        inOrder.verify(financeOpsPolicy)
                .requireFinanceOps();

        inOrder.verify(useCase)
                .execute(any());
    }
}