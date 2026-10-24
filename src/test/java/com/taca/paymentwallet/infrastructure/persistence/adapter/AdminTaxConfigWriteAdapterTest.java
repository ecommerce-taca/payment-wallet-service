package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.AdminTaxConfigView;
import com.taca.paymentwallet.application.port.out.AdminTaxConfigWriteModel;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.infrastructure.persistence.entity.TaxConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.TaxConfigJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminTaxConfigWriteAdapterTest {

    @Test
    void shouldAppendNewTaxConfig() {
        TaxConfigJpaRepository repository =
                mock(TaxConfigJpaRepository.class);

        PersistenceUuidGenerator uuidGenerator =
                mock(PersistenceUuidGenerator.class);

        ClockPort clockPort =
                mock(ClockPort.class);

        Instant now =
                Instant.parse("2026-10-19T15:00:00Z");

        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(now);
        when(uuidGenerator.next(now)).thenReturn(id);
        when(repository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminTaxConfigWriteAdapter adapter =
                new AdminTaxConfigWriteAdapter(
                        repository,
                        uuidGenerator,
                        clockPort
                );

        AdminTaxConfigView result =
                adapter.append(
                        new AdminTaxConfigWriteModel(
                                "PLATFORM",
                                null,
                                100,
                                Instant.parse("2026-11-01T00:00:00Z"),
                                "Platform tax",
                                actorId
                        )
                );

        assertEquals(id, result.id());
        assertEquals("PLATFORM", result.scope());
        assertEquals(100, result.rateBps());
        assertEquals(actorId, result.createdBy());
        assertEquals(now, result.createdAt());

        verify(repository).save(any(TaxConfigJpaEntity.class));
        verify(repository, never()).findById(any());
    }
}