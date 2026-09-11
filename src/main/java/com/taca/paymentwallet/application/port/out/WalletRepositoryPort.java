package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import com.taca.paymentwallet.domain.wallet.Wallet;

import java.util.Optional;

public interface WalletRepositoryPort {

    Optional<Wallet> findById(WalletId walletId);

    Optional<Wallet> findByShopIdAndCurrencyForUpdate(
            ShopId shopId,
            String currency
    );

    Wallet save(Wallet wallet);
}
