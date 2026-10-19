package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.out.AdminTaxConfigReadPort;
import com.taca.paymentwallet.application.port.out.AdminTaxConfigView;
import com.taca.paymentwallet.application.result.GetAdminTaxConfigResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GetAdminTaxConfigsServiceTest {

    @Test
    void shouldReturnTaxConfigs() {
        AdminTaxConfigReadPort readPort = mock(AdminTaxConfigReadPort.class);

        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        AdminTaxConfigView view = new AdminTaxConfigView(
                id,
                "PLATFORM",
                null,
                100,
                Instant.parse("2026-10-01T00:00:00Z"),
                "Platform tax",
                createdBy,
                Instant.parse("2026-09-30T00:00:00Z")
        );

        when(readPort.findAll()).thenReturn(List.of(view));

        GetAdminTaxConfigsService service =
                new GetAdminTaxConfigsService(readPort);

        List<GetAdminTaxConfigResult> result = service.execute();

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).id());
        assertEquals("PLATFORM", result.get(0).scope());
        assertEquals(null, result.get(0).categoryId());
        assertEquals(100, result.get(0).rateBps());
        assertEquals(createdBy, result.get(0).createdBy());

        verify(readPort).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoConfigExists() {
        AdminTaxConfigReadPort readPort = mock(AdminTaxConfigReadPort.class);

        when(readPort.findAll()).thenReturn(List.of());

        GetAdminTaxConfigsService service =
                new GetAdminTaxConfigsService(readPort);

        assertEquals(0, service.execute().size());

        verify(readPort).findAll();
    }
}