package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.infrastructure.persistence.projection.SellerLedgerEntryProjection;
import com.taca.paymentwallet.infrastructure.persistence.repository.LedgerEntryJpaRepository;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SellerLedgerReadAdapterTest {

    @Test
    void shouldMapSellerLedgerPage() {
        LedgerEntryJpaRepository repository = mock(LedgerEntryJpaRepository.class);
        SellerLedgerEntryProjection row = mock(SellerLedgerEntryProjection.class);

        UUID entryId = UUID.randomUUID();
        UUID postingId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();

        when(row.getEntryId()).thenReturn(entryId.toString());
        when(row.getPostingId()).thenReturn(postingId.toString());
        when(row.getEntryType()).thenReturn("CREDIT");
        when(row.getAmount()).thenReturn(92_000L);
        when(row.getBalanceAfter()).thenReturn(92_000L);
        when(row.getReferenceType()).thenReturn("PAYMENT");
        when(row.getReferenceId()).thenReturn(referenceId.toString());
        when(row.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 10, 11, 12, 0));

        when(repository.findSellerLedger(any(), any(), any(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(row)));

        SellerLedgerReadAdapter adapter = new SellerLedgerReadAdapter(repository);

        var result = adapter.findByShop(
                new ShopId(UUID.randomUUID()),
                1,
                20,
                null,
                null,
                null
        );

        assertThat(result.entries()).hasSize(1);
        assertThat(result.entries().getFirst().entryId()).isEqualTo(entryId);
        assertThat(result.entries().getFirst().balanceAfter()).isEqualTo(92_000L);
        assertThat(result.total()).isEqualTo(1);
    }
}