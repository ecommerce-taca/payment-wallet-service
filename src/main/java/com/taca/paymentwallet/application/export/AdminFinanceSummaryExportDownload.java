package com.taca.paymentwallet.application.export;

import java.util.Arrays;
import java.util.Objects;

public record AdminFinanceSummaryExportDownload(
        String fileName,
        String contentType,
        byte[] content
) {

    public AdminFinanceSummaryExportDownload {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
        Objects.requireNonNull(content, "content must not be null");
        content = Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}