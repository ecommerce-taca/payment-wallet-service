package com.taca.paymentwallet.presentation.rest.seller;

import java.util.UUID;

public record SellerLedgerReferenceData(
        String type,
        UUID id
) {
}