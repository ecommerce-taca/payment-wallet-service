package com.taca.paymentwallet.infrastructure.persistence.support;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PersistenceUuidGenerator {

    public UUID next(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException(
                    "now must not be null"
            );
        }

        long unixMillis =
                now.toEpochMilli() & 0x0000FFFFFFFFFFFFL;

        long randomA =
                ThreadLocalRandom.current().nextLong()
                        & 0x0FFFL;

        long mostSignificantBits =
                (unixMillis << 16)
                        | 0x7000L
                        | randomA;

        long randomB =
                ThreadLocalRandom.current().nextLong()
                        & 0x3FFFFFFFFFFFFFFFL;

        long leastSignificantBits =
                0x8000000000000000L
                        | randomB;

        return new UUID(
                mostSignificantBits,
                leastSignificantBits
        );
    }
}