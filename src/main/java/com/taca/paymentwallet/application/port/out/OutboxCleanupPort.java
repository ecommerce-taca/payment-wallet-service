package com.taca.paymentwallet.application.port.out;

import java.time.Instant;

public interface OutboxCleanupPort {

    int deleteCompletedBefore(
            Instant cutoff,
            int batchSize
    );
}