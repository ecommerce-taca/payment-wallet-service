package com.taca.paymentwallet.presentation.rest.seller;

import com.taca.paymentwallet.application.port.in.GetSellerWalletUseCase;
import com.taca.paymentwallet.application.query.GetSellerWalletQuery;
import com.taca.paymentwallet.application.result.GetSellerWalletResult;
import com.taca.paymentwallet.application.security.ShopScopePolicy;
import com.taca.paymentwallet.presentation.rest.GlobalRestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SellerWalletControllerTest {

    private GetSellerWalletUseCase getSellerWalletUseCase;
    private ShopScopePolicy shopScopePolicy;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        getSellerWalletUseCase = mock(GetSellerWalletUseCase.class);
        shopScopePolicy = mock(ShopScopePolicy.class);

        SellerWalletController controller = new SellerWalletController(
                getSellerWalletUseCase,
                shopScopePolicy
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnSellerWallet() throws Exception {
        UUID walletId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        when(shopScopePolicy.requireSingleShopId()).thenReturn(shopId);
        when(getSellerWalletUseCase.execute(any())).thenReturn(
                new GetSellerWalletResult(
                        walletId,
                        shopId,
                        12_500_000,
                        3_200_000,
                        "VND",
                        "ACTIVE"
                )
        );

        mockMvc.perform(
                        get("/api/v1/seller/wallet")
                                .header("X-Request-ID", "req-seller-wallet-001")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wallet_id").value(walletId.toString()))
                .andExpect(jsonPath("$.data.shop_id").value(shopId.toString()))
                .andExpect(jsonPath("$.data.available_balance").value(12_500_000))
                .andExpect(jsonPath("$.data.pending_balance").value(3_200_000))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.meta.request_id").value("req-seller-wallet-001"));

        ArgumentCaptor<GetSellerWalletQuery> captor =
                ArgumentCaptor.forClass(GetSellerWalletQuery.class);

        verify(getSellerWalletUseCase).execute(captor.capture());
        assertEquals(shopId, captor.getValue().shopId());
    }

    @Test
    void shouldResolveShopBeforeQueryingWallet() throws Exception {
        UUID shopId = UUID.randomUUID();

        when(shopScopePolicy.requireSingleShopId()).thenReturn(shopId);
        when(getSellerWalletUseCase.execute(any())).thenReturn(
                new GetSellerWalletResult(
                        UUID.randomUUID(),
                        shopId,
                        0,
                        0,
                        "VND",
                        "ACTIVE"
                )
        );

        mockMvc.perform(
                        get("/api/v1/seller/wallet")
                                .header("X-Request-ID", "req-seller-wallet-002")
                )
                .andExpect(status().isOk());

        var inOrder = inOrder(shopScopePolicy, getSellerWalletUseCase);
        inOrder.verify(shopScopePolicy).requireSingleShopId();
        inOrder.verify(getSellerWalletUseCase).execute(any());
    }

    @Test
    void shouldRequireRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/seller/wallet"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_REQUIRED_HEADER"));

        verifyNoInteractions(shopScopePolicy);
        verifyNoInteractions(getSellerWalletUseCase);
    }
}