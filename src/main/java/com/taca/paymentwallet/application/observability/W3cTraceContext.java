package com.taca.paymentwallet.application.observability;

import java.security.SecureRandom;
import java.util.HexFormat;

public class W3cTraceContext {

    private static final String VERSION = "00";
    private static final String DEFAULT_FLAGS = "01";

    private final SecureRandom random = new SecureRandom();

    public String create() {
        return format(randomHex(16), randomHex(8), DEFAULT_FLAGS);
    }

    public String childOf(String traceparent) {
        if (!isValid(traceparent)) {
            return create();
        }

        String normalized = traceparent.trim().toLowerCase();

        return format(
                normalized.substring(3, 35),
                randomHex(8),
                normalized.substring(53, 55)
        );
    }

    public boolean isValid(String traceparent) {
        if (traceparent == null) {
            return false;
        }

        String value = traceparent.trim().toLowerCase();

        if (value.length() != 55
                || value.charAt(2) != '-'
                || value.charAt(35) != '-'
                || value.charAt(52) != '-') {
            return false;
        }

        String version = value.substring(0, 2);
        String traceId = value.substring(3, 35);
        String parentId = value.substring(36, 52);
        String flags = value.substring(53, 55);

        return VERSION.equals(version)
                && isHex(traceId)
                && isHex(parentId)
                && isHex(flags)
                && !isZero(traceId)
                && !isZero(parentId);
    }

    private String format(String traceId, String spanId, String flags) {
        return "%s-%s-%s-%s".formatted(VERSION, traceId, spanId, flags);
    }

    private String randomHex(int bytes) {
        byte[] value = new byte[bytes];
        do {
            random.nextBytes(value);
        } while (isZero(value));
        return HexFormat.of().formatHex(value);
    }

    private boolean isHex(String value) {
        return value.chars().allMatch(character -> Character.digit(character, 16) >= 0);
    }

    private boolean isZero(String value) {
        return value.chars().allMatch(character -> character == '0');
    }

    private boolean isZero(byte[] value) {
        for (byte item : value) {
            if (item != 0) {
                return false;
            }
        }
        return true;
    }
}
