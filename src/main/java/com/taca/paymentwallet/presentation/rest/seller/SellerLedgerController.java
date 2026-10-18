package com.taca.paymentwallet.presentation.rest.seller;

import com.taca.paymentwallet.application.port.in.GetSellerLedgerUseCase;
import com.taca.paymentwallet.application.query.GetSellerLedgerQuery;
import com.taca.paymentwallet.application.result.GetSellerLedgerEntryResult;
import com.taca.paymentwallet.application.result.GetSellerLedgerResult;
import com.taca.paymentwallet.application.security.SellerAccessPolicy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
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
            @RequestParam(defaultValue = "1") String page,
            @RequestParam(defaultValue = "20") String size,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String type
    ) {
        UUID shopId = sellerAccessPolicy.requireSellerShopId();

        GetSellerLedgerQuery query = new GetSellerLedgerQuery(
                shopId,
                parseInteger(page, "page"),
                parseInteger(size, "size"),
                parseInstant(from, "from"),
                parseInstant(to, "to"),
                type
        );

        GetSellerLedgerResult result = getSellerLedgerUseCase.execute(query);

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

    private int parseInteger(String value, String name) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a valid integer");
        }
    }

    private Instant parseInstant(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(name + " must be a valid ISO-8601 instant");
        }
    }

    private SellerLedgerData toData(GetSellerLedgerEntryResult entry) {
        return new SellerLedgerData(
                entry.entryId(),
                entry.postingId(),
                entry.entryType(),
                entry.amount(),
                entry.balanceAfter(),
                new SellerLedgerReferenceData(entry.referenceType(), entry.referenceId()),
                entry.description(),
                entry.createdAt()
        );
    }
}