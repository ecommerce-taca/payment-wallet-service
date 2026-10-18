package com.taca.paymentwallet.presentation.rest.seller;

import com.taca.paymentwallet.application.port.in.GetSellerRevenueUseCase;
import com.taca.paymentwallet.application.query.GetSellerRevenueQuery;
import com.taca.paymentwallet.application.query.RevenueGranularity;
import com.taca.paymentwallet.application.result.GetSellerRevenueBucketResult;
import com.taca.paymentwallet.application.result.GetSellerRevenueResult;
import com.taca.paymentwallet.application.security.SellerAccessPolicy;
import com.taca.paymentwallet.presentation.rest.ApiMeta;
import com.taca.paymentwallet.presentation.rest.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller")
public class SellerRevenueController {

    private final GetSellerRevenueUseCase getSellerRevenueUseCase;
    private final SellerAccessPolicy sellerAccessPolicy;

    public SellerRevenueController(
            GetSellerRevenueUseCase getSellerRevenueUseCase,
            SellerAccessPolicy sellerAccessPolicy
    ) {
        this.getSellerRevenueUseCase = Objects.requireNonNull(getSellerRevenueUseCase);
        this.sellerAccessPolicy = Objects.requireNonNull(sellerAccessPolicy);
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<SellerRevenueData>> getRevenue(
            @RequestHeader("X-Request-ID") String requestId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String granularity
    ) {
        UUID shopId = sellerAccessPolicy.requireSellerShopId();

        GetSellerRevenueResult result = getSellerRevenueUseCase.execute(
                new GetSellerRevenueQuery(
                        shopId,
                        parseDate(from, "from"),
                        parseDate(to, "to"),
                        parseGranularity(granularity)
                )
        );

        return ResponseEntity.ok(
                new ApiResponse<>(toData(result), new ApiMeta(requestId))
        );
    }

    private LocalDate parseDate(String value, String name) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(name + " must be a valid ISO-8601 date");
        }
    }

    private RevenueGranularity parseGranularity(String value) {
        try {
            return RevenueGranularity.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("granularity must be DAY, WEEK or MONTH");
        }
    }

    private SellerRevenueData toData(GetSellerRevenueResult result) {
        return new SellerRevenueData(
                new SellerRevenueRangeData(
                        result.range().from(),
                        result.range().to(),
                        result.range().granularity()
                ),
                result.currency(),
                new SellerRevenueSummaryData(
                        result.summary().gross(),
                        result.summary().commission(),
                        result.summary().tax(),
                        result.summary().net(),
                        result.summary().refunded(),
                        result.summary().orderCount()
                ),
                result.buckets().stream().map(this::toBucketData).toList()
        );
    }

    private SellerRevenueBucketData toBucketData(GetSellerRevenueBucketResult bucket) {
        return new SellerRevenueBucketData(
                bucket.period(),
                bucket.gross(),
                bucket.commission(),
                bucket.tax(),
                bucket.net(),
                bucket.orderCount()
        );
    }
}