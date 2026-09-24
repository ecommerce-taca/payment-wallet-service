package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.PaymentRepositoryPort;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentOrderJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PaymentPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentOrderJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PaymentRepositoryAdapter
        implements PaymentRepositoryPort {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentOrderJpaRepository paymentOrderJpaRepository;
    private final PaymentPersistenceMapper mapper;
    private final ClockPort clockPort;
    private final PersistenceUuidGenerator uuidGenerator;

    public PaymentRepositoryAdapter(
            PaymentJpaRepository paymentJpaRepository,
            PaymentOrderJpaRepository paymentOrderJpaRepository,
            PaymentPersistenceMapper mapper,
            ClockPort clockPort,
            PersistenceUuidGenerator uuidGenerator
    ) {
        this.paymentJpaRepository =
                Objects.requireNonNull(paymentJpaRepository);

        this.paymentOrderJpaRepository =
                Objects.requireNonNull(paymentOrderJpaRepository);

        this.mapper =
                Objects.requireNonNull(mapper);

        this.clockPort =
                Objects.requireNonNull(clockPort);

        this.uuidGenerator =
                Objects.requireNonNull(uuidGenerator);
    }

    @Override
    public Optional<Payment> findById(
            PaymentId paymentId
    ) {
        Objects.requireNonNull(
                paymentId,
                "paymentId must not be null"
        );

        return paymentJpaRepository
                .findById(paymentId.value())
                .map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByCheckoutGroupId(
            CheckoutGroupId checkoutGroupId
    ) {
        Objects.requireNonNull(
                checkoutGroupId,
                "checkoutGroupId must not be null"
        );

        return paymentJpaRepository
                .findByCheckoutGroupId(
                        checkoutGroupId.value()
                )
                .map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByIdForUpdate(
            PaymentId paymentId
    ) {
        Objects.requireNonNull(
                paymentId,
                "paymentId must not be null"
        );

        return paymentJpaRepository
                .findByIdForUpdate(
                        paymentId.value()
                )
                .map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByCheckoutGroupIdForUpdate(
            CheckoutGroupId checkoutGroupId
    ) {
        Objects.requireNonNull(
                checkoutGroupId,
                "checkoutGroupId must not be null"
        );

        return paymentJpaRepository
                .findByCheckoutGroupIdForUpdate(
                        checkoutGroupId.value()
                )
                .map(this::toDomain);
    }

    @Override
    public Payment save(
            Payment payment
    ) {
        Objects.requireNonNull(
                payment,
                "payment must not be null"
        );

        Optional<PaymentJpaEntity> existing =
                paymentJpaRepository.findById(
                        payment.id().value()
                );

        if (existing.isPresent()) {
            return updateExisting(
                    payment,
                    existing.get()
            );
        }

        return insertNew(payment);
    }

    private Payment insertNew(
            Payment payment
    ) {
        LocalDateTime now =
                currentPersistenceTime();

        PaymentJpaEntity entity =
                mapper.toNewEntity(
                        payment,
                        now
                );

        paymentJpaRepository.save(entity);

        saveNewOrders(
                payment,
                now
        );

        return payment;
    }

    private Payment updateExisting(
            Payment payment,
            PaymentJpaEntity entity
    ) {
        LocalDateTime now =
                currentPersistenceTime();

        mapper.updateEntity(
                payment,
                entity,
                now
        );

        paymentJpaRepository.save(entity);

        return payment;
    }

    private void saveNewOrders(
            Payment payment,
            LocalDateTime createdAt
    ) {
        List<PaymentOrderJpaEntity> entities =
                payment.orders()
                        .stream()
                        .map(order -> toOrderEntity(
                                payment,
                                order,
                                createdAt
                        ))
                        .toList();

        paymentOrderJpaRepository.saveAll(
                entities
        );
    }

    private PaymentOrderJpaEntity toOrderEntity(
            Payment payment,
            PaymentOrder order,
            LocalDateTime createdAt
    ) {
        return mapper.toOrderEntity(
                payment.id(),
                order,
                uuidGenerator.next(
                        clockPort.now()
                ),
                createdAt
        );
    }

    private Payment toDomain(
            PaymentJpaEntity entity
    ) {
        List<PaymentOrderJpaEntity> orders =
                paymentOrderJpaRepository
                        .findByPaymentId(
                                entity.getId()
                        );

        return mapper.toDomain(
                entity,
                orders
        );
    }

    private LocalDateTime currentPersistenceTime() {
        return PersistenceTimeMapper
                .toLocalDateTime(
                        clockPort.now()
                );
    }
}