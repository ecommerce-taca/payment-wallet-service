package com.taca.paymentwallet.presentation.rest.seller;

import com.taca.paymentwallet.application.port.in.GetSellerWalletUseCase;
import com.taca.paymentwallet.application.query.GetSellerWalletQuery;
import com.taca.paymentwallet.application.result.GetSellerWalletResult;
import com.taca.paymentwallet.application.security.SellerAccessPolicy;
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
    private final SellerAccessPolicy sellerAccessPolicy;

    public SellerWalletController(
            GetSellerWalletUseCase getSellerWalletUseCase,
            SellerAccessPolicy sellerAccessPolicy
    ) {
        this.getSellerWalletUseCase = Objects.requireNonNull(getSellerWalletUseCase);
        this.sellerAccessPolicy = Objects.requireNonNull(sellerAccessPolicy);
    }

    @GetMapping("/wallet")
    public ResponseEntity<ApiResponse<SellerWalletData>> getWallet(
            @RequestHeader("X-Request-ID") String requestId
    ) {
        UUID shopId = sellerAccessPolicy.requireSellerShopId();
        GetSellerWalletResult result = getSellerWalletUseCase.execute(new GetSellerWalletQuery(shopId));

        return ResponseEntity.ok(
                new ApiResponse<>(toData(result), new ApiMeta(requestId))
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