package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.PaymentAttemptRepositoryPort;
import com.taca.paymentwallet.domain.payment.PaymentAttempt;
import com.taca.paymentwallet.domain.payment.PaymentAttemptStatus;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentAttemptJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PaymentAttemptPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentAttemptJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class PaymentAttemptRepositoryAdapter
        implements PaymentAttemptRepositoryPort {

    private final PaymentAttemptJpaRepository repository;
    private final PaymentAttemptPersistenceMapper mapper;
    private final ClockPort clockPort;

    public PaymentAttemptRepositoryAdapter(
            PaymentAttemptJpaRepository repository,
            PaymentAttemptPersistenceMapper mapper,
            ClockPort clockPort
    ) {
        this.repository =
                Objects.requireNonNull(
                        repository
                );

        this.mapper =
                Objects.requireNonNull(
                        mapper
                );

        this.clockPort =
                Objects.requireNonNull(
                        clockPort
                );
    }

    @Override
    public PaymentAttempt save(
            PaymentAttempt attempt
    ) {
        Objects.requireNonNull(
                attempt,
                "attempt must not be null"
        );

        Optional<PaymentAttemptJpaEntity> existing =
                repository.findById(
                        attempt.id().value()
                );

        if (existing.isPresent()) {
            return updateExisting(
                    attempt,
                    existing.get()
            );
        }

        return insertNew(
                attempt
        );
    }

    @Override
    public Optional<PaymentAttempt>
    findByProviderAndProviderTransactionRef(
            String provider,
            String providerTransactionRef
    ) {
        String normalizedProvider =
                normalizeProvider(
                        provider
                );

        String normalizedReference =
                normalizeTransactionRef(
                        providerTransactionRef
                );

        return repository
                .findByProviderAndProviderTransactionRef(
                        normalizedProvider,
                        normalizedReference
                )
                .map(
                        mapper::toDomain
                );
    }

    @Override
    public List<PaymentAttempt> findByPaymentId(
            PaymentId paymentId
    ) {
        Objects.requireNonNull(
                paymentId,
                "paymentId must not be null"
        );

        return repository
                .findByPaymentIdOrderByCreatedAtDesc(
                        paymentId.value()
                )
                .stream()
                .map(
                        mapper::toDomain
                )
                .toList();
    }

    @Override
    public boolean existsPendingByPaymentId(
            PaymentId paymentId
    ) {
        Objects.requireNonNull(
                paymentId,
                "paymentId must not be null"
        );

        return repository
                .existsByPaymentIdAndStatus(
                        paymentId.value(),
                        PaymentAttemptStatus
                                .PENDING
                                .name()
                );
    }

    private PaymentAttempt insertNew(
            PaymentAttempt attempt
    ) {
        LocalDateTime now =
                currentPersistenceTime();

        PaymentAttemptJpaEntity entity =
                mapper.toNewEntity(
                        attempt,
                        now
                );

        repository.save(
                entity
        );

        return attempt;
    }

    private PaymentAttempt updateExisting(
            PaymentAttempt attempt,
            PaymentAttemptJpaEntity entity
    ) {
        mapper.updateEntity(
                attempt,
                entity,
                currentPersistenceTime()
        );

        repository.save(
                entity
        );

        return attempt;
    }

    private String normalizeProvider(
            String provider
    ) {
        if (provider == null
                || provider.isBlank()) {
            throw new IllegalArgumentException(
                    "provider must not be blank"
            );
        }

        return provider
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }

    private String normalizeTransactionRef(
            String providerTransactionRef
    ) {
        if (providerTransactionRef == null
                || providerTransactionRef.isBlank()) {
            throw new IllegalArgumentException(
                    "providerTransactionRef must not be blank"
            );
        }

        return providerTransactionRef.trim();
    }

    private LocalDateTime currentPersistenceTime() {
        return PersistenceTimeMapper
                .toLocalDateTime(
                        clockPort.now()
                );
    }
}