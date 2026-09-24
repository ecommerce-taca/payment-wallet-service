package com.taca.paymentwallet.infrastructure.persistence.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class PersistenceTimeMapper {

    private PersistenceTimeMapper() {
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }

        return LocalDateTime.ofInstant(
                instant,
                ZoneOffset.UTC
        );
    }

    public static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }

        return localDateTime.toInstant(
                ZoneOffset.UTC
        );
    }
}
