package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.LedgerPostingId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.wallet.LedgerEntry;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerPostingJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.LedgerPostingPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.LedgerEntryJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.LedgerPostingJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class LedgerPostingRepositoryAdapterTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-24T16:30:00Z"
            );

    private LedgerPostingJpaRepository postingRepository;
    private LedgerEntryJpaRepository entryRepository;
    private ClockPort clockPort;

    private LedgerPostingRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        postingRepository =
                mock(LedgerPostingJpaRepository.class);

        entryRepository =
                mock(LedgerEntryJpaRepository.class);

        clockPort =
                mock(ClockPort.class);

        when(clockPort.now())
                .thenReturn(NOW);

        adapter =
                new LedgerPostingRepositoryAdapter(
                        postingRepository,
                        entryRepository,
                        new LedgerPostingPersistenceMapper(),
                        clockPort,
                        new PersistenceUuidGenerator()
                );
    }

    @Test
    void shouldPersistPostingAndEntries() {
        LedgerPosting posting =
                posting();

        when(
                postingRepository.existsByBusinessKey(
                        posting.businessKey()
                )
        ).thenReturn(false);

        adapter.save(posting);

        ArgumentCaptor<LedgerPostingJpaEntity>
                postingCaptor =
                ArgumentCaptor.forClass(
                        LedgerPostingJpaEntity.class
                );

        verify(postingRepository)
                .save(
                        postingCaptor.capture()
                );

        LedgerPostingJpaEntity savedPosting =
                postingCaptor.getValue();

        assertEquals(
                posting.id().value(),
                savedPosting.getId()
        );

        assertEquals(
                posting.postingType(),
                savedPosting.getPostingType()
        );

        assertEquals(
                posting.businessKey(),
                savedPosting.getBusinessKey()
        );

        assertEquals(
                posting.referenceType(),
                savedPosting.getReferenceType()
        );

        assertEquals(
                UUID.fromString(
                        posting.referenceId()
                ),
                savedPosting.getReferenceId()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        24,
                        16,
                        30
                ),
                savedPosting.getCreatedAt()
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntryJpaEntity>>
                entriesCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(entryRepository)
                .saveAll(
                        entriesCaptor.capture()
                );

        List<LedgerEntryJpaEntity> entries =
                entriesCaptor.getValue();

        assertEquals(
                2,
                entries.size()
        );

        assertEquals(
                posting.id().value(),
                entries.get(0).getPostingId()
        );

        assertEquals(
                posting.id().value(),
                entries.get(1).getPostingId()
        );

        assertEquals(
                "DEBIT",
                entries.get(0).getEntryType()
        );

        assertEquals(
                "CREDIT",
                entries.get(1).getEntryType()
        );

        assertEquals(
                100_000L,
                entries.get(0).getAmount()
        );

        assertEquals(
                100_000L,
                entries.get(1).getAmount()
        );
    }

    @Test
    void shouldGenerateVersion7IdsForLedgerEntries() {
        LedgerPosting posting = posting();

        when(
                postingRepository.existsByBusinessKey(
                        posting.businessKey()
                )
        ).thenReturn(false);

        adapter.save(posting);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntryJpaEntity>>
                captor =
                ArgumentCaptor.forClass(List.class);

        verify(entryRepository)
                .saveAll(
                        captor.capture()
                );

        List<LedgerEntryJpaEntity> entries =
                captor.getValue();

        assertEquals(
                7,
                entries.get(0).getId().version()
        );

        assertEquals(
                7,
                entries.get(1).getId().version()
        );

        assertNotEquals(
                entries.get(0).getId(),
                entries.get(1).getId()
        );
    }

    @Test
    void shouldUseSameCreatedAtForPostingAndEntries() {
        LedgerPosting posting =
                posting();

        when(
                postingRepository.existsByBusinessKey(
                        posting.businessKey()
                )
        ).thenReturn(false);

        adapter.save(posting);

        ArgumentCaptor<LedgerPostingJpaEntity>
                postingCaptor =
                ArgumentCaptor.forClass(
                        LedgerPostingJpaEntity.class
                );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntryJpaEntity>>
                entriesCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(postingRepository)
                .save(
                        postingCaptor.capture()
                );

        verify(entryRepository)
                .saveAll(
                        entriesCaptor.capture()
                );

        LocalDateTime postingCreatedAt =
                postingCaptor
                        .getValue()
                        .getCreatedAt();

        for (LedgerEntryJpaEntity entry
                : entriesCaptor.getValue()) {

            assertEquals(
                    postingCreatedAt,
                    entry.getCreatedAt()
            );
        }

        verify(clockPort, times(1))
                .now();
    }

    @Test
    void shouldRejectDuplicateBusinessKey() {
        LedgerPosting posting =
                posting();

        when(
                postingRepository.existsByBusinessKey(
                        posting.businessKey()
                )
        ).thenReturn(true);

        DuplicateLedgerPostingException exception =
                assertThrows(
                        DuplicateLedgerPostingException.class,
                        () -> adapter.save(posting)
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                posting.businessKey()
                        )
        );

        verify(postingRepository, never())
                .save(any());

        verify(entryRepository, never())
                .saveAll(anyList());

        verifyNoInteractions(clockPort);
    }

    @Test
    void shouldPersistAllEntriesOfMultiEntryPosting() {
        LedgerPostingId postingId =
                new LedgerPostingId(
                        UUID.randomUUID()
                );

        UUID referenceId =
                UUID.randomUUID();

        LedgerPosting posting =
                new LedgerPosting(
                        postingId,
                        "PAYMENT_CAPTURE",
                        "PAYMENT_CAPTURE:"
                                + referenceId,
                        "PAYMENT",
                        referenceId.toString(),
                        List.of(
                                LedgerEntry.debit(
                                        new LedgerAccountId(
                                                UUID.randomUUID()
                                        ),
                                        Money.vnd(100_000)
                                ),
                                LedgerEntry.credit(
                                        new LedgerAccountId(
                                                UUID.randomUUID()
                                        ),
                                        Money.vnd(7_000)
                                ),
                                LedgerEntry.credit(
                                        new LedgerAccountId(
                                                UUID.randomUUID()
                                        ),
                                        Money.vnd(1_000)
                                ),
                                LedgerEntry.credit(
                                        new LedgerAccountId(
                                                UUID.randomUUID()
                                        ),
                                        Money.vnd(92_000)
                                )
                        )
                );

        when(
                postingRepository.existsByBusinessKey(
                        posting.businessKey()
                )
        ).thenReturn(false);

        adapter.save(posting);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntryJpaEntity>>
                captor =
                ArgumentCaptor.forClass(List.class);

        verify(entryRepository)
                .saveAll(captor.capture());

        assertEquals(
                4,
                captor.getValue().size()
        );
    }

    private LedgerPosting posting() {
        LedgerPostingId postingId =
                new LedgerPostingId(
                        UUID.randomUUID()
                );

        UUID referenceId =
                UUID.randomUUID();

        return new LedgerPosting(
                postingId,
                "PAYMENT_CAPTURE",
                "PAYMENT_CAPTURE:"
                        + referenceId,
                "PAYMENT",
                referenceId.toString(),
                List.of(
                        LedgerEntry.debit(
                                new LedgerAccountId(
                                        UUID.randomUUID()
                                ),
                                Money.vnd(100_000)
                        ),
                        LedgerEntry.credit(
                                new LedgerAccountId(
                                        UUID.randomUUID()
                                ),
                                Money.vnd(100_000)
                        )
                )
        );
    }
}