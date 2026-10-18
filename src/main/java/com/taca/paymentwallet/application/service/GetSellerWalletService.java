package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.exception.WalletNotFoundException;
import com.taca.paymentwallet.application.port.in.GetSellerWalletUseCase;
import com.taca.paymentwallet.application.port.out.SellerWalletReadPort;
import com.taca.paymentwallet.application.port.out.SellerWalletView;
import com.taca.paymentwallet.application.query.GetSellerWalletQuery;
import com.taca.paymentwallet.application.result.GetSellerWalletResult;
import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.util.Objects;

public class GetSellerWalletService implements GetSellerWalletUseCase {

    private static final String CURRENCY = "VND";

    private final SellerWalletReadPort sellerWalletReadPort;

    public GetSellerWalletService(SellerWalletReadPort sellerWalletReadPort) {
        this.sellerWalletReadPort = Objects.requireNonNull(sellerWalletReadPort);
    }

    @Override
    public GetSellerWalletResult execute(GetSellerWalletQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        ShopId shopId = new ShopId(query.shopId());

        SellerWalletView wallet = sellerWalletReadPort
                .findByShopIdAndCurrency(shopId, CURRENCY)
                .orElseThrow(() -> new WalletNotFoundException(shopId, CURRENCY));

        return new GetSellerWalletResult(
                wallet.walletId(),
                wallet.shopId(),
                wallet.availableBalance(),
                wallet.pendingBalance(),
                wallet.currency(),
                wallet.status(),
                wallet.asOf()
        );
    }
}