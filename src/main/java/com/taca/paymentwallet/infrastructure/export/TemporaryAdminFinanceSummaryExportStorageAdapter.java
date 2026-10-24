package com.taca.paymentwallet.infrastructure.export;

import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportDownload;
import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportFile;
import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportLocation;
import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryExportDownloadPort;
import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryExportStoragePort;
import com.taca.paymentwallet.application.port.out.ClockPort;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TemporaryAdminFinanceSummaryExportStorageAdapter
        implements AdminFinanceSummaryExportStoragePort,
        AdminFinanceSummaryExportDownloadPort {

    private static final String DOWNLOAD_PATH =
            "/api/v1/admin/finance/summary/exports/";

    private final Map<String, StoredExport> exports = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final ClockPort clockPort;

    public TemporaryAdminFinanceSummaryExportStorageAdapter(
            ClockPort clockPort
    ) {
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    @Override
    public AdminFinanceSummaryExportLocation store(
            AdminFinanceSummaryExportFile file,
            Duration urlTtl
    ) {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(urlTtl, "urlTtl must not be null");

        if (urlTtl.isZero() || urlTtl.isNegative()) {
            throw new IllegalArgumentException("urlTtl must be positive");
        }

        Instant expiresAt = clockPort.now().plus(urlTtl);
        String token = nextToken();

        exports.put(token, new StoredExport(
                file.fileName(),
                file.contentType(),
                file.content(),
                expiresAt
        ));

        removeExpired();

        return new AdminFinanceSummaryExportLocation(
                DOWNLOAD_PATH + token,
                expiresAt
        );
    }

    @Override
    public Optional<AdminFinanceSummaryExportDownload> find(String token) {
        if (token == null || token.isBlank()) return Optional.empty();

        StoredExport stored = exports.get(token);

        if (stored == null) return Optional.empty();

        if (!clockPort.now().isBefore(stored.expiresAt())) {
            exports.remove(token);
            return Optional.empty();
        }

        return Optional.of(new AdminFinanceSummaryExportDownload(
                stored.fileName(),
                stored.contentType(),
                stored.content()
        ));
    }

    private String nextToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void removeExpired() {
        Instant now = clockPort.now();
        exports.entrySet().removeIf(entry ->
                !now.isBefore(entry.getValue().expiresAt())
        );
    }

    private record StoredExport(
            String fileName,
            String contentType,
            byte[] content,
            Instant expiresAt
    ) {
    }
}