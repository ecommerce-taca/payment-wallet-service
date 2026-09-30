package com.taca.paymentwallet.infrastructure.vnpay;

import com.taca.paymentwallet.application.gateway.vnpay.CreateVnpayPaymentUrlRequest;
import com.taca.paymentwallet.application.gateway.vnpay.CreateVnpayPaymentUrlResult;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.VnpayGatewayPort;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class VnpayGatewayAdapter
        implements VnpayGatewayPort {

    private static final String VERSION = "2.1.0";

    private static final String COMMAND = "pay";

    private static final String LOCALE = "vn";

    private static final String ORDER_TYPE = "other";

    private static final ZoneId VNPAY_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter VNPAY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyyMMddHHmmss"
            );

    private final VnpayProperties properties;

    private final VnpaySigner signer;

    private final ClockPort clockPort;

    public VnpayGatewayAdapter(
            VnpayProperties properties,
            VnpaySigner signer,
            ClockPort clockPort
    ) {
        this.properties =
                Objects.requireNonNull(
                        properties
                );

        this.signer =
                Objects.requireNonNull(
                        signer
                );

        this.clockPort =
                Objects.requireNonNull(
                        clockPort
                );
    }

    @Override
    public CreateVnpayPaymentUrlResult createPaymentUrl(
            CreateVnpayPaymentUrlRequest request
    ) {
        Objects.requireNonNull(
                request,
                "request must not be null"
        );

        validateConfiguration();

        String providerTransactionRef =
                toTransactionRef(
                        request
                );

        Map<String, String> parameters =
                buildParameters(
                        request,
                        providerTransactionRef
                );

        String secureHash =
                signer.sign(
                        parameters,
                        properties.hashSecret()
                );

        String paymentUrl =
                buildPaymentUrl(
                        parameters,
                        secureHash
                );

        return new CreateVnpayPaymentUrlResult(
                providerTransactionRef,
                paymentUrl,
                request.expiresAt()
        );
    }

    private Map<String, String> buildParameters(
            CreateVnpayPaymentUrlRequest request,
            String providerTransactionRef
    ) {
        Map<String, String> parameters =
                new LinkedHashMap<>();

        parameters.put(
                "vnp_Version",
                VERSION
        );

        parameters.put(
                "vnp_Command",
                COMMAND
        );

        parameters.put(
                "vnp_TmnCode",
                properties.tmnCode()
        );

        parameters.put(
                "vnp_Amount",
                toVnpayAmount(
                        request
                )
        );

        parameters.put(
                "vnp_CurrCode",
                request.amount().currency()
        );

        parameters.put(
                "vnp_TxnRef",
                providerTransactionRef
        );

        parameters.put(
                "vnp_OrderInfo",
                buildOrderInfo(
                        request
                )
        );

        parameters.put(
                "vnp_OrderType",
                ORDER_TYPE
        );

        parameters.put(
                "vnp_Locale",
                LOCALE
        );

        parameters.put(
                "vnp_ReturnUrl",
                properties.returnUrl()
        );

        parameters.put(
                "vnp_IpAddr",
                request.clientIp()
        );

        parameters.put(
                "vnp_CreateDate",
                formatVnpayDate(
                        clockPort.now()
                )
        );

        parameters.put(
                "vnp_ExpireDate",
                formatVnpayDate(
                        request.expiresAt()
                )
        );

        return parameters;
    }

    private String buildPaymentUrl(
            Map<String, String> parameters,
            String secureHash
    ) {
        StringBuilder query =
                new StringBuilder();

        parameters.entrySet()
                .stream()
                .filter(entry ->
                        entry.getKey() != null
                                && !entry.getKey().isBlank()
                )
                .filter(entry ->
                        entry.getValue() != null
                                && !entry.getValue().isBlank()
                )
                .sorted(
                        Map.Entry.comparingByKey()
                )
                .forEach(entry -> {
                    if (!query.isEmpty()) {
                        query.append("&");
                    }

                    query.append(
                            encode(
                                    entry.getKey()
                            )
                    );

                    query.append("=");

                    query.append(
                            encode(
                                    entry.getValue()
                            )
                    );
                });

        query.append(
                "&vnp_SecureHash="
        );

        query.append(
                encode(
                        secureHash
                )
        );

        return properties.paymentUrl()
                + "?"
                + query;
    }

    private String toVnpayAmount(
            CreateVnpayPaymentUrlRequest request
    ) {
        long amount =
                Math.multiplyExact(
                        request.amount().amount(),
                        100L
                );

        return Long.toString(
                amount
        );
    }

    private String toTransactionRef(
            CreateVnpayPaymentUrlRequest request
    ) {
        return request.paymentId()
                .value()
                .toString()
                .replace(
                        "-",
                        ""
                );
    }

    private String buildOrderInfo(
            CreateVnpayPaymentUrlRequest request
    ) {
        String checkoutGroupId =
                request.checkoutGroupId()
                        .value()
                        .toString()
                        .replace(
                                "-",
                                ""
                        );

        return "Thanh toan don hang "
                + checkoutGroupId;
    }

    private String formatVnpayDate(
            Instant instant
    ) {
        return VNPAY_DATE_FORMATTER
                .format(
                        instant.atZone(
                                VNPAY_ZONE
                        )
                );
    }

    private String encode(
            String value
    ) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    private void validateConfiguration() {
        if (properties.tmnCode() == null
                || properties.tmnCode().isBlank()) {
            throw new IllegalStateException(
                    "VNPAY tmnCode must be configured"
            );
        }

        if (properties.hashSecret() == null
                || properties.hashSecret().isBlank()) {
            throw new IllegalStateException(
                    "VNPAY hashSecret must be configured"
            );
        }

        if (properties.paymentUrl() == null
                || properties.paymentUrl().isBlank()) {
            throw new IllegalStateException(
                    "VNPAY paymentUrl must be configured"
            );
        }

        if (properties.returnUrl() == null
                || properties.returnUrl().isBlank()) {
            throw new IllegalStateException(
                    "VNPAY returnUrl must be configured"
            );
        }
    }
}