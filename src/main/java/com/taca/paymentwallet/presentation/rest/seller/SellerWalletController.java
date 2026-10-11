package com.taca.paymentwallet.presentation.rest.seller;

import com.taca.paymentwallet.application.port.in.GetSellerWalletUseCase;
import com.taca.paymentwallet.application.query.GetSellerWalletQuery;
import com.taca.paymentwallet.application.result.GetSellerWalletResult;
import com.taca.paymentwallet.application.security.ShopScopePolicy;
import com.taca.paymentwallet.presentation.rest.ApiMeta;
import com.taca.paymentwallet.presentation.rest.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller")
public class SellerWalletController {

    private final GetSellerWalletUseCase getSellerWalletUseCase;
    private final ShopScopePolicy shopScopePolicy;

    public SellerWalletController(
            GetSellerWalletUseCase getSellerWalletUseCase,
            ShopScopePolicy shopScopePolicy
    ) {
        this.getSellerWalletUseCase = Objects.requireNonNull(getSellerWalletUseCase);
        this.shopScopePolicy = Objects.requireNonNull(shopScopePolicy);
    }

    @GetMapping("/wallet")
    public ResponseEntity<ApiResponse<SellerWalletData>> getWallet(
            @RequestHeader("X-Request-ID") String requestId
    ) {
        UUID shopId = shopScopePolicy.requireSingleShopId();

        GetSellerWalletResult result = getSellerWalletUseCase.execute(
                new GetSellerWalletQuery(shopId)
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        toData(result),
                        new ApiMeta(requestId)
                )
        );
    }

    private SellerWalletData toData(GetSellerWalletResult result) {
        return new SellerWalletData(
                result.walletId(),
                result.shopId(),
                result.availableBalance(),
                result.pendingBalance(),
                result.currency(),
                result.status()
        );
    }
}