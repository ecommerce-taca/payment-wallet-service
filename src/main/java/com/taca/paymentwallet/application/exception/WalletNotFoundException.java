package com.taca.paymentwallet.application.exception;

import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;

public class WalletNotFoundException extends ApplicationException {

    public WalletNotFoundException(WalletId walletId) {
        super("Wallet not found: " + walletId.value());
    }

    public WalletNotFoundException(ShopId shopId, String currency) {
        super("Wallet not found for shop "
                + shopId.value()
                + " and currency "
                + currency);
    }
}
