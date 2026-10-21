package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.exception.SettlementBatchNotFoundException;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.application.query.GetAdminSettlementDetailQuery;
import com.taca.paymentwallet.application.result.GetAdminSettlementDetailResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetAdminSettlementDetailServiceTest {

    @Test
    void shouldReturnSettlementDetail() {
        AdminSettlementReadPort readPort =
                mock(AdminSettlementReadPort.class);

        UUID batchId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        AdminSettlementBatchView batch =
                new AdminSettlementBatchView(
                        batchId,
                        Instant.parse("2026-10-01T00:00:00Z"),
                        Instant.parse("2026-10-08T00:00:00Z"),
                        "COMPLETED",
                        1,
                        1_000_000L,
                        100_000L,
                        50_000L,
                        850_000L,
                        850_000L,
                        0L,
                        Instant.parse("2026-10-08T01:00:00Z"),
                        Instant.parse("2026-10-08T01:05:00Z"),
                        null
                );

        AdminSettlementBatchItemView item =
                new AdminSettlementBatchItemView(
                        itemId,
                        batchId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        1_000_000L,
                        100_000L,
                        50_000L,
                        850_000L,
                        850_000L,
                        0L,
                        "COMPLETED",
                        UUID.randomUUID(),
                        Instant.parse("2026-10-08T01:00:00Z")
                );

        AdminSettlementLineView line =
                new AdminSettlementLineView(
                        UUID.randomUUID(),
                        itemId,
                        UUID.randomUUID(),
                        850_000L,
                        Instant.parse("2026-10-08T01:00:00Z")
                );

        when(readPort.findDetailById(batchId))
                .thenReturn(
                        Optional.of(
                                new AdminSettlementBatchDetailView(
                                        batch,
                                        List.of(
                                                new AdminSettlementBatchItemDetailView(
                                                        item,
                                                        List.of(line)
                                                )
                                        )
                                )
                        )
                );

        GetAdminSettlementDetailService service =
                new GetAdminSettlementDetailService(readPort);

        GetAdminSettlementDetailResult result =
                service.execute(
                        new GetAdminSettlementDetailQuery(batchId)
                );

        assertEquals(batchId, result.id());
        assertEquals(1, result.items().size());
        assertEquals(itemId, result.items().get(0).id());
        assertEquals(1, result.items().get(0).lines().size());

        verify(readPort).findDetailById(batchId);
    }

    @Test
    void shouldThrowWhenBatchDoesNotExist() {
        AdminSettlementReadPort readPort =
                mock(AdminSettlementReadPort.class);

        UUID batchId = UUID.randomUUID();

        when(readPort.findDetailById(batchId))
                .thenReturn(Optional.empty());

        GetAdminSettlementDetailService service =
                new GetAdminSettlementDetailService(readPort);

        assertThrows(
                SettlementBatchNotFoundException.class,
                () -> service.execute(
                        new GetAdminSettlementDetailQuery(batchId)
                )
        );

        verify(readPort).findDetailById(batchId);
    }
}