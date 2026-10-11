package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.in.GetSellerLedgerUseCase;
import com.taca.paymentwallet.application.port.out.SellerLedgerEntryView;
import com.taca.paymentwallet.application.port.out.SellerLedgerPage;
import com.taca.paymentwallet.application.port.out.SellerLedgerReadPort;
import com.taca.paymentwallet.application.query.GetSellerLedgerQuery;
import com.taca.paymentwallet.application.result.GetSellerLedgerEntryResult;
import com.taca.paymentwallet.application.result.GetSellerLedgerResult;
import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.util.Objects;

public class GetSellerLedgerService implements GetSellerLedgerUseCase {

    private final SellerLedgerReadPort sellerLedgerReadPort;

    public GetSellerLedgerService(SellerLedgerReadPort sellerLedgerReadPort) {
        this.sellerLedgerReadPort = Objects.requireNonNull(sellerLedgerReadPort);
    }

    @Override
    public GetSellerLedgerResult execute(GetSellerLedgerQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        SellerLedgerPage page = sellerLedgerReadPort.findByShop(
                new ShopId(query.shopId()),
                query.page(),
                query.size(),
                query.from(),
                query.to(),
                query.type()
        );

        return new GetSellerLedgerResult(
                page.entries().stream().map(this::toResult).toList(),
                query.page(),
                query.size(),
                page.total(),
                page.totalPages()
        );
    }

    private GetSellerLedgerEntryResult toResult(SellerLedgerEntryView entry) {
        return new GetSellerLedgerEntryResult(
                entry.entryId(),
                entry.postingId(),
                entry.entryType(),
                entry.amount(),
                entry.balanceAfter(),
                entry.referenceType(),
                entry.referenceId(),
                entry.description(),
                entry.createdAt()
        );
    }
}