package com.taca.paymentwallet.presentation.rest.seller;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import com.taca.paymentwallet.application.port.in.GetSellerRevenueUseCase;
import com.taca.paymentwallet.application.query.GetSellerRevenueQuery;
import com.taca.paymentwallet.application.query.RevenueGranularity;
import com.taca.paymentwallet.application.result.*;
import com.taca.paymentwallet.application.security.SellerAccessPolicy;
import com.taca.paymentwallet.presentation.rest.GlobalRestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SellerRevenueControllerTest {

    private GetSellerRevenueUseCase getSellerRevenueUseCase;
    private SellerAccessPolicy sellerAccessPolicy;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        getSellerRevenueUseCase = mock(GetSellerRevenueUseCase.class);
        sellerAccessPolicy = mock(SellerAccessPolicy.class);

        SellerRevenueController controller = new SellerRevenueController(
                getSellerRevenueUseCase,
                sellerAccessPolicy
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnSellerRevenue() throws Exception {
        UUID shopId = UUID.randomUUID();

        when(sellerAccessPolicy.requireSellerShopId()).thenReturn(shopId);
        when(getSellerRevenueUseCase.execute(any())).thenReturn(result());

        mockMvc.perform(
                        get("/api/v1/seller/revenue")
                                .header("X-Request-ID", "req-revenue-001")
                                .param("from", "2026-08-01")
                                .param("to", "2026-08-31")
                                .param("granularity", "DAY")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.range.from").value("2026-08-01"))
                .andExpect(jsonPath("$.data.range.to").value("2026-08-31"))
                .andExpect(jsonPath("$.data.range.granularity").value("DAY"))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.summary.gross").value(125_000_000))
                .andExpect(jsonPath("$.data.summary.commission").value(8_750_000))
                .andExpect(jsonPath("$.data.summary.tax").value(1_250_000))
                .andExpect(jsonPath("$.data.summary.net").value(115_000_000))
                .andExpect(jsonPath("$.data.summary.refunded").value(2_000_000))
                .andExpect(jsonPath("$.data.summary.order_count").value(340))
                .andExpect(jsonPath("$.data.buckets[0].period").value("2026-08-01"))
                .andExpect(jsonPath("$.data.buckets[0].gross").value(4_200_000))
                .andExpect(jsonPath("$.data.buckets[0].order_count").value(12))
                .andExpect(jsonPath("$.meta.request_id").value("req-revenue-001"));
    }

    @Test
    void shouldPassRevenueQueryToUseCase() throws Exception {
        UUID shopId = UUID.randomUUID();

        when(sellerAccessPolicy.requireSellerShopId()).thenReturn(shopId);
        when(getSellerRevenueUseCase.execute(any())).thenReturn(result());

        mockMvc.perform(
                        get("/api/v1/seller/revenue")
                                .header("X-Request-ID", "req-revenue-query")
                                .param("from", "2026-08-01")
                                .param("to", "2026-08-31")
                                .param("granularity", "week")
                )
                .andExpect(status().isOk());

        ArgumentCaptor<GetSellerRevenueQuery> captor =
                ArgumentCaptor.forClass(GetSellerRevenueQuery.class);

        verify(getSellerRevenueUseCase).execute(captor.capture());

        GetSellerRevenueQuery query = captor.getValue();

        assertThat(query.shopId()).isEqualTo(shopId);
        assertThat(query.from()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(query.to()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(query.granularity()).isEqualTo(RevenueGranularity.WEEK);
    }

    @Test
    void shouldAuthorizeBeforeQueryingRevenue() throws Exception {
        when(sellerAccessPolicy.requireSellerShopId()).thenReturn(UUID.randomUUID());
        when(getSellerRevenueUseCase.execute(any())).thenReturn(result());

        mockMvc.perform(
                        get("/api/v1/seller/revenue")
                                .header("X-Request-ID", "req-revenue-auth")
                                .param("from", "2026-08-01")
                                .param("to", "2026-08-31")
                                .param("granularity", "DAY")
                )
                .andExpect(status().isOk());

        InOrder inOrder = inOrder(sellerAccessPolicy, getSellerRevenueUseCase);
        inOrder.verify(sellerAccessPolicy).requireSellerShopId();
        inOrder.verify(getSellerRevenueUseCase).execute(any());
    }

    @Test
    void shouldRejectAnonymousSeller() throws Exception {
        doThrow(new UnauthenticatedException())
                .when(sellerAccessPolicy)
                .requireSellerShopId();

        mockMvc.perform(
                        get("/api/v1/seller/revenue")
                                .header("X-Request-ID", "req-revenue-anonymous")
                                .param("from", "2026-08-01")
                                .param("to", "2026-08-31")
                                .param("granularity", "DAY")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_UNAUTHENTICATED"));

        verifyNoInteractions(getSellerRevenueUseCase);
    }

    @Test
    void shouldRejectUnauthorizedSeller() throws Exception {
        doThrow(new ForbiddenException())
                .when(sellerAccessPolicy)
                .requireSellerShopId();

        mockMvc.perform(
                        get("/api/v1/seller/revenue")
                                .header("X-Request-ID", "req-revenue-forbidden")
                                .param("from", "2026-08-01")
                                .param("to", "2026-08-31")
                                .param("granularity", "DAY")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_FORBIDDEN"));

        verifyNoInteractions(getSellerRevenueUseCase);
    }

    @Test
    void shouldRequireRequestId() throws Exception {
        mockMvc.perform(
                        get("/api/v1/seller/revenue")
                                .param("from", "2026-08-01")
                                .param("to", "2026-08-31")
                                .param("granularity", "DAY")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_REQUIRED_HEADER"));

        verifyNoInteractions(sellerAccessPolicy);
        verifyNoInteractions(getSellerRevenueUseCase);
    }

    @Test
    void shouldRequireRevenueQueryParameters() throws Exception {
        mockMvc.perform(
                        get("/api/v1/seller/revenue")
                                .header("X-Request-ID", "req-revenue-missing")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_REQUIRED_PARAMETER"));

        verifyNoInteractions(sellerAccessPolicy);
        verifyNoInteractions(getSellerRevenueUseCase);
    }

    private GetSellerRevenueResult result() {
        return new GetSellerRevenueResult(
                new GetSellerRevenueRangeResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        "DAY"
                ),
                "VND",
                new GetSellerRevenueSummaryResult(
                        125_000_000,
                        8_750_000,
                        1_250_000,
                        115_000_000,
                        2_000_000,
                        340
                ),
                List.of(
                        new GetSellerRevenueBucketResult(
                                "2026-08-01",
                                4_200_000,
                                294_000,
                                42_000,
                                3_864_000,
                                12
                        )
                )
        );
    }
}