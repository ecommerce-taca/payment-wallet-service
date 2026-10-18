package com.taca.paymentwallet.application.query;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetSellerWalletQueryTest {

    @Test
    void shouldCreateQuery() {
        UUID shopId = UUID.randomUUID();

        GetSellerWalletQuery query = new GetSellerWalletQuery(shopId);

        assertEquals(shopId, query.shopId());
    }

    @Test
    void shouldRejectNullShopId() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new GetSellerWalletQuery(
                                null
                        )
        );
    }
}