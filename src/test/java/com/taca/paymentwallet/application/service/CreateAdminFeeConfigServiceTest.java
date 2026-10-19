package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.CreateAdminFeeConfigCommand;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.application.result.CreateAdminFeeConfigResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CreateAdminFeeConfigServiceTest {

    @Test
    void shouldAppendFeeConfigAndAuditMutation() {
        AdminFeeConfigWritePort writePort =
                mock(AdminFeeConfigWritePort.class);

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
                Instant.parse("2026-10-19T12:00:00Z");

        when(clockPort.now()).thenReturn(createdAt);

        when(writePort.append(any())).thenReturn(
                new AdminFeeConfigView(
                        configId,
                        "PLATFORM",
                        null,
                        700,
                        effectiveFrom,
                        "Platform commission",
                        actorId,
                        createdAt
                )
        );

        CreateAdminFeeConfigService service =
                new CreateAdminFeeConfigService(
                        writePort,
                        auditPort,
                        transactionPort,
                        clockPort
                );

        CreateAdminFeeConfigResult result =
                service.execute(
                        new CreateAdminFeeConfigCommand(
                                "PLATFORM",
                                null,
                                700,
                                effectiveFrom,
                                "Platform commission",
                                actorId
                        )
                );

        assertEquals(configId, result.id());
        assertEquals(700, result.rateBps());
        assertEquals(actorId, result.createdBy());

        verify(writePort).append(any(AdminFeeConfigWriteModel.class));

        verify(auditPort).append(
                actorId,
                "UPDATE_FEE_CONFIG",
                "FEE_CONFIG",
                configId,
                "Platform commission",
                createdAt
        );
    }
}