package com.taca.paymentwallet.presentation.rest.seller;

import com.taca.paymentwallet.application.port.in.GetSellerLedgerUseCase;
import com.taca.paymentwallet.application.query.GetSellerLedgerQuery;
import com.taca.paymentwallet.application.result.GetSellerLedgerEntryResult;
import com.taca.paymentwallet.application.result.GetSellerLedgerResult;
import com.taca.paymentwallet.application.security.SellerAccessPolicy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller/wallet")
public class SellerLedgerController {

    private final GetSellerLedgerUseCase getSellerLedgerUseCase;
    private final SellerAccessPolicy sellerAccessPolicy;

    public SellerLedgerController(
            GetSellerLedgerUseCase getSellerLedgerUseCase,
            SellerAccessPolicy sellerAccessPolicy
    ) {
        this.getSellerLedgerUseCase = Objects.requireNonNull(getSellerLedgerUseCase);
        this.sellerAccessPolicy = Objects.requireNonNull(sellerAccessPolicy);
    }

    @GetMapping("/ledger")
    public ResponseEntity<SellerLedgerResponse> getLedger(
            @RequestHeader("X-Request-ID") String requestId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID shopId = sellerAccessPolicy.requireSellerShopId();

        GetSellerLedgerResult result = getSellerLedgerUseCase.execute(
                new GetSellerLedgerQuery(shopId, page, size, null, null, null)
        );

        return ResponseEntity.ok(
                new SellerLedgerResponse(
                        result.entries().stream().map(this::toData).toList(),
                        new SellerLedgerMeta(
                                requestId,
                                result.page(),
                                result.size(),
                                result.total(),
                                result.totalPages()
                        )
                )
        );
    }

    private SellerLedgerData toData(GetSellerLedgerEntryResult entry) {
        return new SellerLedgerData(
                entry.entryId(),
                entry.postingId(),
                entry.entryType(),
                entry.amount(),
                entry.balanceAfter(),
                new SellerLedgerReferenceData(
                        entry.referenceType(),
                        entry.referenceId()
                ),
                entry.description(),
                entry.createdAt()
        );
    }
}