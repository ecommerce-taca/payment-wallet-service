package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.AdminFeeConfigView;
import com.taca.paymentwallet.application.port.out.AdminFeeConfigWriteModel;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.infrastructure.persistence.entity.FeeConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.FeeConfigJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminFeeConfigWriteAdapterTest {

    @Test
    void shouldAppendNewFeeConfig() {
        FeeConfigJpaRepository repository =
                mock(FeeConfigJpaRepository.class);

        PersistenceUuidGenerator uuidGenerator =
                mock(PersistenceUuidGenerator.class);

        ClockPort clockPort =
                mock(ClockPort.class);

        Instant now =
                Instant.parse("2026-10-19T12:00:00Z");

        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(now);
        when(uuidGenerator.next(now)).thenReturn(id);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminFeeConfigWriteAdapter adapter =
                new AdminFeeConfigWriteAdapter(
                        repository,
                        uuidGenerator,
                        clockPort
                );

        AdminFeeConfigView result =
                adapter.append(
                        new AdminFeeConfigWriteModel(
                                "PLATFORM",
                                null,
                                700,
                                Instant.parse("2026-11-01T00:00:00Z"),
                                "Platform commission",
                                actorId
                        )
                );

        assertEquals(id, result.id());
        assertEquals("PLATFORM", result.scope());
        assertEquals(700, result.rateBps());
        assertEquals(actorId, result.createdBy());
        assertEquals(now, result.createdAt());

        verify(repository).save(any(FeeConfigJpaEntity.class));
        verify(repository, never()).findById(any());
    }
}