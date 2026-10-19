package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.CreateAdminTaxConfigCommand;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.application.result.CreateAdminTaxConfigResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateAdminTaxConfigServiceTest {

    @Test
    void shouldAppendTaxConfigAndAuditMutation() {
        AdminTaxConfigWritePort writePort =
                mock(AdminTaxConfigWritePort.class);

        AdminAuditPort auditPort =
                mock(AdminAuditPort.class);

        ClockPort clockPort =
                mock(ClockPort.class);

        TransactionPort transactionPort =
                new TransactionPort() {
                    @Override
                    public <T> T execute(java.util.function.Supplier<T> action) {
                        return action.get();
                    }
                };

        UUID actorId = UUID.randomUUID();
        UUID configId = UUID.randomUUID();

        Instant effectiveFrom =
                Instant.parse("2026-11-01T00:00:00Z");

        Instant createdAt =
                Instant.parse("2026-10-19T15:00:00Z");

        when(clockPort.now()).thenReturn(createdAt);

        when(writePort.append(any())).thenReturn(
                new AdminTaxConfigView(
                        configId,
                        "PLATFORM",
                        null,
                        100,
                        effectiveFrom,
                        "Platform tax",
                        actorId,
                        createdAt
                )
        );

        CreateAdminTaxConfigService service =
                new CreateAdminTaxConfigService(
                        writePort,
                        auditPort,
                        transactionPort,
                        clockPort
                );

        CreateAdminTaxConfigResult result =
                service.execute(
                        new CreateAdminTaxConfigCommand(
                                "PLATFORM",
                                null,
                                100,
                                effectiveFrom,
                                "Platform tax",
                                actorId
                        )
                );

        assertEquals(configId, result.id());
        assertEquals(100, result.rateBps());
        assertEquals(actorId, result.createdBy());

        verify(writePort).append(any(AdminTaxConfigWriteModel.class));

        verify(auditPort).append(
                actorId,
                "UPDATE_TAX_CONFIG",
                "TAX_CONFIG",
                configId,
                "Platform tax",
                createdAt
        );
    }
}