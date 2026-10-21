package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.SettlementBatchNotFoundException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import com.taca.paymentwallet.application.port.in.GetAdminSettlementDetailUseCase;
import com.taca.paymentwallet.application.port.in.GetAdminSettlementsUseCase;
import com.taca.paymentwallet.application.query.GetAdminSettlementDetailQuery;
import com.taca.paymentwallet.application.result.GetAdminSettlementBatchItemResult;
import com.taca.paymentwallet.application.result.GetAdminSettlementBatchResult;
import com.taca.paymentwallet.application.result.GetAdminSettlementDetailResult;
import com.taca.paymentwallet.application.result.GetAdminSettlementLineResult;
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

class AdminSettlementControllerTest {

    private GetAdminSettlementsUseCase getAdminSettlementsUseCase;
    private FinanceOpsPolicy financeOpsPolicy;
    private MockMvc mockMvc;
    private GetAdminSettlementDetailUseCase getAdminSettlementDetailUseCase;

    @BeforeEach
    void setUp() {
        getAdminSettlementsUseCase = mock(GetAdminSettlementsUseCase.class);
        financeOpsPolicy = mock(FinanceOpsPolicy.class);
        getAdminSettlementDetailUseCase = mock(GetAdminSettlementDetailUseCase.class);

        AdminSettlementController controller =
                new AdminSettlementController(
                        getAdminSettlementsUseCase,
                        getAdminSettlementDetailUseCase,
                        financeOpsPolicy
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnSettlementBatches() throws Exception {
        UUID id = UUID.randomUUID();

        when(getAdminSettlementsUseCase.execute()).thenReturn(
                List.of(
                        new GetAdminSettlementBatchResult(
                                id,
                                Instant.parse("2026-10-01T00:00:00Z"),
                                Instant.parse("2026-10-08T00:00:00Z"),
                                "COMPLETED",
                                2,
                                1_000_000L,
                                100_000L,
                                50_000L,
                                850_000L,
                                800_000L,
                                50_000L,
                                Instant.parse("2026-10-08T01:00:00Z"),
                                Instant.parse("2026-10-08T01:05:00Z"),
                                null
                        )
                )
        );

        mockMvc.perform(
                        get("/api/v1/admin/settlements")
                                .header("X-Request-ID", "req-admin-settlement-001")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data[0].period_start")
                        .value("2026-10-01T00:00:00Z"))
                .andExpect(jsonPath("$.data[0].period_end")
                        .value("2026-10-08T00:00:00Z"))
                .andExpect(jsonPath("$.data[0].status")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.data[0].shop_count")
                        .value(2))
                .andExpect(jsonPath("$.data[0].total_gross")
                        .value(1_000_000))
                .andExpect(jsonPath("$.data[0].total_commission")
                        .value(100_000))
                .andExpect(jsonPath("$.data[0].total_tax")
                        .value(50_000))
                .andExpect(jsonPath("$.data[0].total_net")
                        .value(850_000))
                .andExpect(jsonPath("$.data[0].total_released")
                        .value(800_000))
                .andExpect(jsonPath("$.data[0].total_held")
                        .value(50_000))
                .andExpect(jsonPath("$.data[0].created_at")
                        .value("2026-10-08T01:00:00Z"))
                .andExpect(jsonPath("$.data[0].closed_at")
                        .value("2026-10-08T01:05:00Z"))
                .andExpect(jsonPath("$.meta.request_id")
                        .value("req-admin-settlement-001"));

        verify(financeOpsPolicy).requireFinanceOps();
        verify(getAdminSettlementsUseCase).execute();
    }

    @Test
    void shouldReturnEmptyList() throws Exception {
        when(getAdminSettlementsUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/admin/settlements")
                                .header("X-Request-ID", "req-admin-settlement-empty")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(financeOpsPolicy).requireFinanceOps();
        verify(getAdminSettlementsUseCase).execute();
    }

    @Test
    void shouldRequireRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settlements"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("MISSING_REQUIRED_HEADER"));

        verifyNoInteractions(financeOpsPolicy);
        verifyNoInteractions(getAdminSettlementsUseCase);
    }

    @Test
    void shouldRejectAnonymousActor() throws Exception {
        doThrow(new UnauthenticatedException())
                .when(financeOpsPolicy)
                .requireFinanceOps();

        mockMvc.perform(
                        get("/api/v1/admin/settlements")
                                .header("X-Request-ID", "req-admin-settlement-anonymous")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_UNAUTHENTICATED"));

        verifyNoInteractions(getAdminSettlementsUseCase);
    }

    @Test
    void shouldRejectNonFinanceOpsActor() throws Exception {
        doThrow(new ForbiddenException())
                .when(financeOpsPolicy)
                .requireFinanceOps();

        mockMvc.perform(
                        get("/api/v1/admin/settlements")
                                .header("X-Request-ID", "req-admin-settlement-forbidden")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_FORBIDDEN"));

        verifyNoInteractions(getAdminSettlementsUseCase);
    }

    @Test
    void shouldAuthorizeBeforeReadingSettlements() throws Exception {
        when(getAdminSettlementsUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/admin/settlements")
                                .header("X-Request-ID", "req-admin-settlement-order")
                )
                .andExpect(status().isOk());

        var inOrder = inOrder(
                financeOpsPolicy,
                getAdminSettlementsUseCase
        );

        inOrder.verify(financeOpsPolicy).requireFinanceOps();
        inOrder.verify(getAdminSettlementsUseCase).execute();
    }

    @Test
    void shouldReturnSettlementDetail() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID allocationId = UUID.randomUUID();

        when(
                getAdminSettlementDetailUseCase.execute(
                        any(GetAdminSettlementDetailQuery.class)
                )
        ).thenReturn(
                new GetAdminSettlementDetailResult(
                        batchId,
                        Instant.parse("2026-10-01T00:00:00Z"),
                        Instant.parse("2026-10-08T00:00:00Z"),
                        "COMPLETED",
                        1,
                        1_000_000L,
                        100_000L,
                        50_000L,
                        850_000L,
                        850_000L,
                        0L,
                        Instant.parse("2026-10-08T01:00:00Z"),
                        Instant.parse("2026-10-08T01:05:00Z"),
                        null,
                        List.of(
                                new GetAdminSettlementBatchItemResult(
                                        itemId,
                                        batchId,
                                        shopId,
                                        walletId,
                                        1_000_000L,
                                        100_000L,
                                        50_000L,
                                        850_000L,
                                        850_000L,
                                        0L,
                                        "COMPLETED",
                                        UUID.randomUUID(),
                                        Instant.parse("2026-10-08T01:00:00Z"),
                                        List.of(
                                                new GetAdminSettlementLineResult(
                                                        lineId,
                                                        itemId,
                                                        allocationId,
                                                        850_000L,
                                                        Instant.parse("2026-10-08T01:00:00Z")
                                                )
                                        )
                                )
                        )
                )
        );

        mockMvc.perform(
                        get("/api/v1/admin/settlements/{batchId}", batchId)
                                .header(
                                        "X-Request-ID",
                                        "req-admin-settlement-detail-001"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(batchId.toString()))
                .andExpect(jsonPath("$.data.items.length()")
                        .value(1))
                .andExpect(jsonPath("$.data.items[0].shop_id")
                        .value(shopId.toString()))
                .andExpect(jsonPath("$.data.items[0].lines.length()")
                        .value(1))
                .andExpect(jsonPath(
                        "$.data.items[0].lines[0].payment_allocation_id"
                ).value(allocationId.toString()))
                .andExpect(jsonPath("$.meta.request_id")
                        .value("req-admin-settlement-detail-001"));

        verify(financeOpsPolicy).requireFinanceOps();
        verify(getAdminSettlementDetailUseCase)
                .execute(any(GetAdminSettlementDetailQuery.class));
    }

    @Test
    void shouldRejectMalformedBatchId() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/settlements/not-a-uuid")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-settlement-invalid"
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("INVALID_REQUEST"));

        verify(financeOpsPolicy).requireFinanceOps();
        verifyNoInteractions(getAdminSettlementDetailUseCase);
    }

    @Test
    void shouldReturnNotFoundWhenBatchDoesNotExist() throws Exception {
        UUID batchId = UUID.randomUUID();

        when(
                getAdminSettlementDetailUseCase.execute(
                        any(GetAdminSettlementDetailQuery.class)
                )
        ).thenThrow(
                new SettlementBatchNotFoundException(batchId)
        );

        mockMvc.perform(
                        get("/api/v1/admin/settlements/{batchId}", batchId)
                                .header(
                                        "X-Request-ID",
                                        "req-admin-settlement-not-found"
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code")
                        .value("SETTLEMENT_BATCH_NOT_FOUND"));
    }

    @Test
    void shouldAuthorizeBeforeReadingSettlementDetail() throws Exception {
        UUID batchId = UUID.randomUUID();

        doThrow(new ForbiddenException())
                .when(financeOpsPolicy)
                .requireFinanceOps();

        mockMvc.perform(
                        get("/api/v1/admin/settlements/{batchId}", batchId)
                                .header(
                                        "X-Request-ID",
                                        "req-admin-settlement-detail-forbidden"
                                )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(getAdminSettlementDetailUseCase);
    }
}