package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.AdminTaxConfigView;
import com.taca.paymentwallet.infrastructure.persistence.entity.TaxConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.TaxConfigJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminTaxConfigReadAdapterTest {

    @Test
    void shouldReturnTaxConfigsInRepositoryOrder() {
        TaxConfigJpaRepository repository =
                mock(TaxConfigJpaRepository.class);

        TaxConfigJpaEntity newer = taxConfig(
                UUID.randomUUID(),
                "PLATFORM",
                null,
                100,
                LocalDateTime.of(2026, 10, 1, 0, 0)
        );

        UUID categoryId = UUID.randomUUID();

        TaxConfigJpaEntity older = taxConfig(
                UUID.randomUUID(),
                "CATEGORY",
                categoryId,
                80,
                LocalDateTime.of(2026, 9, 1, 0, 0)
        );

        when(repository.findAllByOrderByEffectiveFromDescCreatedAtDesc())
                .thenReturn(List.of(newer, older));

        AdminTaxConfigReadAdapter adapter =
                new AdminTaxConfigReadAdapter(repository);

        List<AdminTaxConfigView> result =
                adapter.findAll();

        assertEquals(2, result.size());

        assertEquals(newer.getId(), result.get(0).id());
        assertEquals("PLATFORM", result.get(0).scope());
        assertNull(result.get(0).categoryId());
        assertEquals(100, result.get(0).rateBps());

        assertEquals(older.getId(), result.get(1).id());
        assertEquals("CATEGORY", result.get(1).scope());
        assertEquals(categoryId, result.get(1).categoryId());
        assertEquals(80, result.get(1).rateBps());

        verify(repository)
                .findAllByOrderByEffectiveFromDescCreatedAtDesc();
    }

    @Test
    void shouldReturnEmptyListWhenNoTaxConfigExists() {
        TaxConfigJpaRepository repository =
                mock(TaxConfigJpaRepository.class);

        when(repository.findAllByOrderByEffectiveFromDescCreatedAtDesc())
                .thenReturn(List.of());

        AdminTaxConfigReadAdapter adapter =
                new AdminTaxConfigReadAdapter(repository);

        List<AdminTaxConfigView> result =
                adapter.findAll();

        assertTrue(result.isEmpty());

        verify(repository)
                .findAllByOrderByEffectiveFromDescCreatedAtDesc();
    }

    private TaxConfigJpaEntity taxConfig(
            UUID id,
            String scope,
            UUID categoryId,
            int rateBps,
            LocalDateTime effectiveFrom
    ) {
        TaxConfigJpaEntity entity =
                new TaxConfigJpaEntity();

        entity.setId(id);
        entity.setScope(scope);
        entity.setCategoryId(categoryId);
        entity.setRateBps(rateBps);
        entity.setEffectiveFrom(effectiveFrom);
        entity.setNote("Phase 8 tax config");
        entity.setCreatedBy(UUID.randomUUID());
        entity.setCreatedAt(effectiveFrom.minusDays(1));

        return entity;
    }
}