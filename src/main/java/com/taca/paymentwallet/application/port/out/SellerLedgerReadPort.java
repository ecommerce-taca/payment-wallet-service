package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.time.Instant;

public interface SellerLedgerReadPort {

    SellerLedgerPage findByShop(
            ShopId shopId,
            int page,
            int size,
            Instant from,
            Instant to,
            String type
    );
}