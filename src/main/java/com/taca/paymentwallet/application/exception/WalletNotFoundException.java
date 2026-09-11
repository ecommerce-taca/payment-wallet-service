package com.taca.paymentwallet.application.exception;

import com.taca.paymentwallet.domain.valueobject.ShopId;

public class WalletNotFoundException extends ApplicationException {

    public WalletNotFoundException(ShopId shopId, String currency) {
        super("Wallet not found for shop " + shopId.value() + " and currency " + currency);
    }
}
