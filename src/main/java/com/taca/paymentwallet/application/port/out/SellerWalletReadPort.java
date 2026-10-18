package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.util.Optional;

public interface SellerWalletReadPort {

    Optional<SellerWalletView> findByShopIdAndCurrency(
            ShopId shopId,
            String currency
    );
}