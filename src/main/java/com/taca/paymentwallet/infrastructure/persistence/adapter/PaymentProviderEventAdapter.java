package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.exception.PaymentProviderEventNotFoundException;
import com.taca.paymentwallet.application.paymentevent.PaymentProviderEvent;
import com.taca.paymentwallet.application.paymentevent.PaymentProviderEventStatus;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.PaymentProviderEventPort;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentEventJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;

import java.util.Locale;
import java.util.Objects;

public class PaymentProviderEventAdapter
        implements PaymentProviderEventPort {

    private final PaymentEventJpaRepository repository;
    private final ClockPort clockPort;
    private final PersistenceUuidGenerator uuidGenerator;

    public PaymentProviderEventAdapter(
            PaymentEventJpaRepository repository,
            ClockPort clockPort,
            PersistenceUuidGenerator uuidGenerator
    ) {
        this.repository =
                Objects.requireNonNull(repository);

        this.clockPort =
                Objects.requireNonNull(clockPort);

        this.uuidGenerator =
                Objects.requireNonNull(uuidGenerator);
    }

    @Override
    public boolean recordIfAbsent(
            PaymentProviderEvent event
    ) {
        Objects.requireNonNull(
                event,
                "event must not be null"
        );

        int inserted =
                repository.insertIgnoreProviderEvent(
                        uuidGenerator
                                .next(event.receivedAt())
                                .toString(),

                        event.paymentId()
                                .value()
                                .toString(),

                        event.provider(),
                        event.providerEventId(),
                        event.providerTransactionRef(),
                        event.responseCode(),
                        event.transactionStatus(),

                        event.amount().amount(),
                        event.amount().currency(),

                        event.payloadHash(),

                        PersistenceTimeMapper
                                .toLocalDateTime(
                                        event.receivedAt()
                                ),

                        event.status().name()
                );

        return inserted == 1;
    }

    @Override
    public void markApplied(
            String provider,
            String providerEventId
    ) {
        String normalizedProvider =
                normalizeProvider(provider);

        String normalizedEventId =
                normalizeProviderEventId(
                        providerEventId
                );

        PaymentEventJpaEntity entity =
                repository
                        .findByProviderAndProviderEventId(
                                normalizedProvider,
                                normalizedEventId
                        )
                        .orElseThrow(
                                () ->
                                        new PaymentProviderEventNotFoundException(
                                                normalizedProvider,
                                                normalizedEventId
                                        )
                        );

        if (PaymentProviderEventStatus.APPLIED.name()
                .equals(entity.getStatus())) {
            return;
        }

        entity.setStatus(
                PaymentProviderEventStatus.APPLIED.name()
        );

        entity.setAppliedAt(
                PersistenceTimeMapper
                        .toLocalDateTime(
                                clockPort.now()
                        )
        );

        entity.setFailureCode(null);

        repository.save(entity);
    }

    private String normalizeProvider(
            String provider
    ) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException(
                    "provider must not be blank"
            );
        }

        return provider
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeProviderEventId(
            String providerEventId
    ) {
        if (providerEventId == null || providerEventId.isBlank()) {

            throw new IllegalArgumentException(
                    "providerEventId must not be blank"
            );
        }

        return providerEventId.trim();
    }
}