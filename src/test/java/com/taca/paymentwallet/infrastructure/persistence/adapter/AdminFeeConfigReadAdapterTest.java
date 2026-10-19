package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.AdminFeeConfigView;
import com.taca.paymentwallet.infrastructure.persistence.entity.FeeConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.FeeConfigJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AdminFeeConfigReadAdapterTest {

    @Test
    void shouldReturnFeeConfigsInRepositoryOrder() {
        FeeConfigJpaRepository repository =
                mock(FeeConfigJpaRepository.class);

        FeeConfigJpaEntity newer = feeConfig(
                UUID.randomUUID(),
                "PLATFORM",
                null,
                700,
                LocalDateTime.of(2026, 10, 1, 0, 0)
        );

        UUID categoryId = UUID.randomUUID();

        FeeConfigJpaEntity older = feeConfig(
                UUID.randomUUID(),
                "CATEGORY",
                categoryId,
                500,
                LocalDateTime.of(2026, 9, 1, 0, 0)
        );

        when(repository.findAllByOrderByEffectiveFromDescCreatedAtDesc())
                .thenReturn(List.of(newer, older));

        AdminFeeConfigReadAdapter adapter =
                new AdminFeeConfigReadAdapter(repository);

        List<AdminFeeConfigView> result =
                adapter.findAll();

        assertEquals(2, result.size());

        assertEquals(newer.getId(), result.get(0).id());
        assertEquals("PLATFORM", result.get(0).scope());
        assertEquals(700, result.get(0).rateBps());
        assertEquals(null, result.get(0).categoryId());

        assertEquals(older.getId(), result.get(1).id());
        assertEquals("CATEGORY", result.get(1).scope());
        assertEquals(categoryId, result.get(1).categoryId());
        assertEquals(500, result.get(1).rateBps());

        verify(repository)
                .findAllByOrderByEffectiveFromDescCreatedAtDesc();
    }

    @Test
    void shouldReturnEmptyListWhenNoFeeConfigExists() {
        FeeConfigJpaRepository repository =
                mock(FeeConfigJpaRepository.class);

        when(repository.findAllByOrderByEffectiveFromDescCreatedAtDesc())
                .thenReturn(List.of());

        AdminFeeConfigReadAdapter adapter =
                new AdminFeeConfigReadAdapter(repository);

        List<AdminFeeConfigView> result =
                adapter.findAll();

        assertEquals(0, result.size());

        verify(repository)
                .findAllByOrderByEffectiveFromDescCreatedAtDesc();
    }

    private FeeConfigJpaEntity feeConfig(
            UUID id,
            String scope,
            UUID categoryId,
            int rateBps,
            LocalDateTime effectiveFrom
    ) {
        FeeConfigJpaEntity entity =
                new FeeConfigJpaEntity();

        entity.setId(id);
        entity.setScope(scope);
        entity.setCategoryId(categoryId);
        entity.setRateBps(rateBps);
        entity.setEffectiveFrom(effectiveFrom);
        entity.setNote("Phase 8 fee config");
        entity.setCreatedBy(UUID.randomUUID());
        entity.setCreatedAt(effectiveFrom.minusDays(1));

        return entity;
    }
}