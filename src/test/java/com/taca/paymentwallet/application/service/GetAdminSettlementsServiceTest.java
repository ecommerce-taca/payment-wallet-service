package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.out.AdminSettlementBatchView;
import com.taca.paymentwallet.application.port.out.AdminSettlementReadPort;
import com.taca.paymentwallet.application.result.GetAdminSettlementBatchResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GetAdminSettlementsServiceTest {

    @Test
    void shouldReturnSettlementBatches() {
        AdminSettlementReadPort readPort = mock(AdminSettlementReadPort.class);

        UUID id = UUID.randomUUID();

        AdminSettlementBatchView view =
                new AdminSettlementBatchView(
                        id,
                        Instant.parse("2026-10-01T00:00:00Z"),
                        Instant.parse("2026-10-08T00:00:00Z"),
                        "COMPLETED",
                        2,
                        1_000_000L,
                        100_000L,
                        50_000L,
                        850_000L,
                        800_000L,
                        50_000L,
                        Instant.parse("2026-10-08T01:00:00Z"),
                        Instant.parse("2026-10-08T01:05:00Z"),
                        null
                );

        when(readPort.findAll()).thenReturn(List.of(view));

        GetAdminSettlementsService service =
                new GetAdminSettlementsService(readPort);

        List<GetAdminSettlementBatchResult> result = service.execute();

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).id());
        assertEquals("COMPLETED", result.get(0).status());
        assertEquals(2, result.get(0).shopCount());
        assertEquals(1_000_000L, result.get(0).totalGross());
        assertEquals(850_000L, result.get(0).totalNet());
        assertEquals(800_000L, result.get(0).totalReleased());
        assertEquals(50_000L, result.get(0).totalHeld());

        verify(readPort).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoSettlementExists() {
        AdminSettlementReadPort readPort = mock(AdminSettlementReadPort.class);

        when(readPort.findAll()).thenReturn(List.of());

        GetAdminSettlementsService service =
                new GetAdminSettlementsService(readPort);

        assertEquals(0, service.execute().size());

        verify(readPort).findAll();
    }
}