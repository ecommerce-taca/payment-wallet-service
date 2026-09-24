package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.LedgerPostingRepositoryPort;
import com.taca.paymentwallet.domain.wallet.LedgerEntry;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerPostingJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.LedgerPostingPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.LedgerEntryJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.LedgerPostingJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class LedgerPostingRepositoryAdapter
        implements LedgerPostingRepositoryPort {

    private final LedgerPostingJpaRepository postingRepository;
    private final LedgerEntryJpaRepository entryRepository;
    private final LedgerPostingPersistenceMapper mapper;
    private final ClockPort clockPort;
    private final PersistenceUuidGenerator uuidGenerator;

    public LedgerPostingRepositoryAdapter(
            LedgerPostingJpaRepository postingRepository,
            LedgerEntryJpaRepository entryRepository,
            LedgerPostingPersistenceMapper mapper,
            ClockPort clockPort,
            PersistenceUuidGenerator uuidGenerator
    ) {
        this.postingRepository = Objects.requireNonNull(postingRepository);
        this.entryRepository = Objects.requireNonNull(entryRepository);
        this.mapper = Objects.requireNonNull(mapper);
        this.clockPort = Objects.requireNonNull(clockPort);
        this.uuidGenerator = Objects.requireNonNull(uuidGenerator);
    }

    @Override
    public void save(LedgerPosting posting) {
        Objects.requireNonNull(
                posting,
                "posting must not be null"
        );

        ensureBusinessKeyDoesNotExist(
                posting.businessKey()
        );

        Instant now = clockPort.now();

        LocalDateTime createdAt =
                PersistenceTimeMapper
                        .toLocalDateTime(now);

        LedgerPostingJpaEntity postingEntity =
                mapper.toPostingEntity(
                        posting,
                        createdAt
                );

        List<LedgerEntryJpaEntity> entryEntities =
                posting.entries()
                        .stream()
                        .map(entry ->
                                toEntryEntity(
                                        posting,
                                        entry,
                                        now,
                                        createdAt
                                )
                        )
                        .toList();

        postingRepository.save(postingEntity);

        entryRepository.saveAll(entryEntities);
    }

    private LedgerEntryJpaEntity toEntryEntity(
            LedgerPosting posting,
            LedgerEntry entry,
            Instant now,
            LocalDateTime createdAt
    ) {
        return mapper.toEntryEntity(
                posting.id(),
                entry,
                uuidGenerator.next(now),
                createdAt
        );
    }

    private void ensureBusinessKeyDoesNotExist(
            String businessKey
    ) {
        if (postingRepository
                .existsByBusinessKey(businessKey)) {
            throw new DuplicateLedgerPostingException(
                    businessKey
            );
        }
    }
}