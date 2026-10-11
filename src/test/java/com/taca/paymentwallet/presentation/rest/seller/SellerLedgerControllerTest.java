package com.taca.paymentwallet.presentation.rest.seller;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import com.taca.paymentwallet.application.port.in.GetSellerLedgerUseCase;
import com.taca.paymentwallet.application.query.GetSellerLedgerQuery;
import com.taca.paymentwallet.application.result.GetSellerLedgerEntryResult;
import com.taca.paymentwallet.application.result.GetSellerLedgerResult;
import com.taca.paymentwallet.application.security.SellerAccessPolicy;
import com.taca.paymentwallet.presentation.rest.GlobalRestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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

class SellerLedgerControllerTest {

    private GetSellerLedgerUseCase getSellerLedgerUseCase;
    private SellerAccessPolicy sellerAccessPolicy;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        getSellerLedgerUseCase = mock(GetSellerLedgerUseCase.class);
        sellerAccessPolicy = mock(SellerAccessPolicy.class);

        SellerLedgerController controller = new SellerLedgerController(
                getSellerLedgerUseCase,
                sellerAccessPolicy
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnSellerLedger() throws Exception {
        UUID shopId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID postingId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();

        when(sellerAccessPolicy.requireSellerShopId()).thenReturn(shopId);
        when(getSellerLedgerUseCase.execute(any())).thenReturn(
                new GetSellerLedgerResult(
                        List.of(
                                new GetSellerLedgerEntryResult(
                                        entryId,
                                        postingId,
                                        "CREDIT",
                                        92_000,
                                        92_000,
                                        "PAYMENT",
                                        referenceId,
                                        "Seller revenue",
                                        Instant.parse("2026-10-11T05:00:00Z")
                                )
                        ),
                        1,
                        20,
                        1,
                        1
                )
        );

        mockMvc.perform(
                        get("/api/v1/seller/wallet/ledger")
                                .header("X-Request-ID", "req-ledger-001")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].entry_id").value(entryId.toString()))
                .andExpect(jsonPath("$.data[0].posting_id").value(postingId.toString()))
                .andExpect(jsonPath("$.data[0].entry_type").value("CREDIT"))
                .andExpect(jsonPath("$.data[0].amount").value(92_000))
                .andExpect(jsonPath("$.data[0].balance_after").value(92_000))
                .andExpect(jsonPath("$.data[0].reference.type").value("PAYMENT"))
                .andExpect(jsonPath("$.data[0].reference.id").value(referenceId.toString()))
                .andExpect(jsonPath("$.data[0].description").value("Seller revenue"))
                .andExpect(jsonPath("$.meta.request_id").value("req-ledger-001"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(20))
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.meta.total_pages").value(1));
    }

    @Test
    void shouldUseDefaultPagination() throws Exception {
        UUID shopId = UUID.randomUUID();

        when(sellerAccessPolicy.requireSellerShopId()).thenReturn(shopId);
        when(getSellerLedgerUseCase.execute(any())).thenReturn(
                new GetSellerLedgerResult(List.of(), 1, 20, 0, 0)
        );

        mockMvc.perform(
                        get("/api/v1/seller/wallet/ledger")
                                .header("X-Request-ID", "req-ledger-default")
                )
                .andExpect(status().isOk());

        ArgumentCaptor<GetSellerLedgerQuery> captor =
                ArgumentCaptor.forClass(GetSellerLedgerQuery.class);

        verify(getSellerLedgerUseCase).execute(captor.capture());

        assertThat(captor.getValue().shopId()).isEqualTo(shopId);
        assertThat(captor.getValue().page()).isEqualTo(1);
        assertThat(captor.getValue().size()).isEqualTo(20);
        assertThat(captor.getValue().from()).isNull();
        assertThat(captor.getValue().to()).isNull();
        assertThat(captor.getValue().type()).isNull();
    }

    @Test
    void shouldAcceptRequestedPageAndSize() throws Exception {
        UUID shopId = UUID.randomUUID();

        when(sellerAccessPolicy.requireSellerShopId()).thenReturn(shopId);
        when(getSellerLedgerUseCase.execute(any())).thenReturn(
                new GetSellerLedgerResult(List.of(), 2, 50, 0, 0)
        );

        mockMvc.perform(
                        get("/api/v1/seller/wallet/ledger")
                                .header("X-Request-ID", "req-ledger-page")
                                .param("page", "2")
                                .param("size", "50")
                )
                .andExpect(status().isOk());

        ArgumentCaptor<GetSellerLedgerQuery> captor =
                ArgumentCaptor.forClass(GetSellerLedgerQuery.class);

        verify(getSellerLedgerUseCase).execute(captor.capture());
        assertThat(captor.getValue().page()).isEqualTo(2);
        assertThat(captor.getValue().size()).isEqualTo(50);
    }

    @Test
    void shouldAuthorizeBeforeQueryingLedger() throws Exception {
        UUID shopId = UUID.randomUUID();

        when(sellerAccessPolicy.requireSellerShopId()).thenReturn(shopId);
        when(getSellerLedgerUseCase.execute(any())).thenReturn(
                new GetSellerLedgerResult(List.of(), 1, 20, 0, 0)
        );

        mockMvc.perform(
                        get("/api/v1/seller/wallet/ledger")
                                .header("X-Request-ID", "req-ledger-auth")
                )
                .andExpect(status().isOk());

        InOrder inOrder = inOrder(sellerAccessPolicy, getSellerLedgerUseCase);
        inOrder.verify(sellerAccessPolicy).requireSellerShopId();
        inOrder.verify(getSellerLedgerUseCase).execute(any());
    }

    @Test
    void shouldRejectAnonymousSeller() throws Exception {
        doThrow(new UnauthenticatedException())
                .when(sellerAccessPolicy)
                .requireSellerShopId();

        mockMvc.perform(
                        get("/api/v1/seller/wallet/ledger")
                                .header("X-Request-ID", "req-ledger-anonymous")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_UNAUTHENTICATED"));

        verifyNoInteractions(getSellerLedgerUseCase);
    }

    @Test
    void shouldRejectUnauthorizedSeller() throws Exception {
        doThrow(new ForbiddenException())
                .when(sellerAccessPolicy)
                .requireSellerShopId();

        mockMvc.perform(
                        get("/api/v1/seller/wallet/ledger")
                                .header("X-Request-ID", "req-ledger-forbidden")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_FORBIDDEN"));

        verifyNoInteractions(getSellerLedgerUseCase);
    }

    @Test
    void shouldRequireRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/seller/wallet/ledger"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_REQUIRED_HEADER"));

        verifyNoInteractions(sellerAccessPolicy);
        verifyNoInteractions(getSellerLedgerUseCase);
    }
}