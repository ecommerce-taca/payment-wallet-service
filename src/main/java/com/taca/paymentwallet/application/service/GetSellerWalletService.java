package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.exception.WalletNotFoundException;
import com.taca.paymentwallet.application.port.in.GetSellerWalletUseCase;
import com.taca.paymentwallet.application.port.out.WalletRepositoryPort;
import com.taca.paymentwallet.application.query.GetSellerWalletQuery;
import com.taca.paymentwallet.application.result.GetSellerWalletResult;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.wallet.Wallet;

import java.util.Objects;

public class GetSellerWalletService implements GetSellerWalletUseCase {

    private static final String CURRENCY = "VND";

    private final WalletRepositoryPort walletRepository;

    public GetSellerWalletService(
            WalletRepositoryPort walletRepository
    ) {
        this.walletRepository = Objects.requireNonNull(walletRepository);
    }

    @Override
    public GetSellerWalletResult execute(
            GetSellerWalletQuery query
    ) {
        Objects.requireNonNull(
                query,
                "query must not be null"
        );

        ShopId shopId = new ShopId(query.shopId());

        Wallet wallet =
                walletRepository
                        .findByShopIdAndCurrency(
                                shopId,
                                CURRENCY
                        )
                        .orElseThrow(
                                () ->
                                        new WalletNotFoundException(
                                                shopId,
                                                CURRENCY
                                        )
                        );

        return toResult(wallet);
    }

    private GetSellerWalletResult toResult(
            Wallet wallet
    ) {
        return new GetSellerWalletResult(
                wallet.id().value(),
                wallet.shopId().value(),
                wallet.availableBalance().amount(),
                wallet.pendingBalance().amount(),
                wallet.currency(),
                wallet.status().name()
        );
    }
}