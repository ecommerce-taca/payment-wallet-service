package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.out.AdminFeeConfigReadPort;
import com.taca.paymentwallet.application.port.out.AdminFeeConfigView;
import com.taca.paymentwallet.application.result.GetAdminFeeConfigResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GetAdminFeeConfigsServiceTest {

    @Test
    void shouldReturnFeeConfigs() {
        AdminFeeConfigReadPort readPort = mock(AdminFeeConfigReadPort.class);

        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        AdminFeeConfigView view = new AdminFeeConfigView(
                id,
                "PLATFORM",
                null,
                700,
                Instant.parse("2026-10-01T00:00:00Z"),
                "Platform commission",
                createdBy,
                Instant.parse("2026-09-30T00:00:00Z")
        );

        when(readPort.findAll()).thenReturn(List.of(view));

        GetAdminFeeConfigsService service =
                new GetAdminFeeConfigsService(readPort);

        List<GetAdminFeeConfigResult> result = service.execute();

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).id());
        assertEquals("PLATFORM", result.get(0).scope());
        assertEquals(null, result.get(0).categoryId());
        assertEquals(700, result.get(0).rateBps());
        assertEquals(createdBy, result.get(0).createdBy());

        verify(readPort).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoConfigExists() {
        AdminFeeConfigReadPort readPort = mock(AdminFeeConfigReadPort.class);

        when(readPort.findAll()).thenReturn(List.of());

        GetAdminFeeConfigsService service =
                new GetAdminFeeConfigsService(readPort);

        assertEquals(0, service.execute().size());

        verify(readPort).findAll();
    }
}