package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.BuyerUserId;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentOrderJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PaymentPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentOrderJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentRepositoryAdapterTest {

    private PaymentJpaRepository paymentRepository;
    private PaymentOrderJpaRepository orderRepository;
    private ClockPort clockPort;

    private PaymentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        paymentRepository =
                mock(PaymentJpaRepository.class);

        orderRepository =
                mock(PaymentOrderJpaRepository.class);

        clockPort =
                mock(ClockPort.class);

        when(clockPort.now())
                .thenReturn(
                        Instant.parse(
                                "2026-09-24T12:00:00Z"
                        )
                );

        adapter = new PaymentRepositoryAdapter(
                paymentRepository,
                orderRepository,
                new PaymentPersistenceMapper(),
                clockPort,
                new PersistenceUuidGenerator()
        );
    }

    @Test
    void shouldInsertPaymentAndOrdersWhenPaymentIsNew() {
        Payment payment =
                codPayment();

        when(
                paymentRepository.findById(
                        payment.id().value()
                )
        ).thenReturn(Optional.empty());

        when(
                paymentRepository.save(any())
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        Payment result =
                adapter.save(payment);

        assertSame(
                payment,
                result
        );

        ArgumentCaptor<PaymentJpaEntity> paymentCaptor =
                ArgumentCaptor.forClass(
                        PaymentJpaEntity.class
                );

        verify(paymentRepository)
                .save(
                        paymentCaptor.capture()
                );

        PaymentJpaEntity savedPayment =
                paymentCaptor.getValue();

        assertEquals(
                payment.id().value(),
                savedPayment.getId()
        );

        assertEquals(
                "PENDING_COD",
                savedPayment.getStatus()
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PaymentOrderJpaEntity>>
                ordersCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(orderRepository)
                .saveAll(
                        ordersCaptor.capture()
                );

        List<PaymentOrderJpaEntity> savedOrders =
                ordersCaptor.getValue();

        assertEquals(
                1,
                savedOrders.size()
        );

        PaymentOrderJpaEntity order =
                savedOrders.getFirst();

        assertEquals(
                payment.id().value(),
                order.getPaymentId()
        );

        assertEquals(
                payment.orders()
                        .getFirst()
                        .orderId()
                        .value(),
                order.getOrderId()
        );

        assertEquals(
                7,
                order.getId().version()
        );
    }

    @Test
    void shouldUpdateExistingPaymentWithoutReinsertingOrders() {
        Payment payment =
                codPayment();

        PaymentJpaEntity existing =
                new PaymentPersistenceMapper()
                        .toNewEntity(
                                payment,
                                java.time.LocalDateTime.of(
                                        2026,
                                        9,
                                        24,
                                        10,
                                        0
                                )
                        );

        existing.setVersion(5L);

        when(
                paymentRepository.findById(
                        payment.id().value()
                )
        ).thenReturn(
                Optional.of(existing)
        );

        payment.markSucceeded(
                Instant.parse(
                        "2026-09-24T12:00:00Z"
                )
        );

        adapter.save(payment);

        assertEquals(
                "SUCCESS",
                existing.getStatus()
        );

        assertEquals(
                5L,
                existing.getVersion()
        );

        verify(orderRepository, never())
                .saveAll(any());
    }

    @Test
    void shouldLoadPaymentWithOrdersById() {
        Payment payment =
                codPayment();

        PaymentPersistenceMapper mapper =
                new PaymentPersistenceMapper();

        PaymentJpaEntity paymentEntity =
                mapper.toNewEntity(
                        payment,
                        java.time.LocalDateTime.of(
                                2026,
                                9,
                                24,
                                12,
                                0
                        )
                );

        PaymentOrderJpaEntity orderEntity =
                mapper.toOrderEntity(
                        payment.id(),
                        payment.orders().getFirst(),
                        UUID.randomUUID(),
                        java.time.LocalDateTime.of(
                                2026,
                                9,
                                24,
                                12,
                                0
                        )
                );

        when(
                paymentRepository.findById(
                        payment.id().value()
                )
        ).thenReturn(
                Optional.of(paymentEntity)
        );

        when(
                orderRepository.findByPaymentId(
                        payment.id().value()
                )
        ).thenReturn(
                List.of(orderEntity)
        );

        Optional<Payment> result =
                adapter.findById(
                        payment.id()
                );

        assertTrue(
                result.isPresent()
        );

        assertEquals(
                payment.id(),
                result.get().id()
        );

        assertEquals(
                payment.orders(),
                result.get().orders()
        );

        assertTrue(
                result.get()
                        .domainEvents()
                        .isEmpty()
        );
    }

    @Test
    void shouldUsePessimisticLookupForFindByIdForUpdate() {
        Payment payment =
                codPayment();

        PaymentPersistenceMapper mapper =
                new PaymentPersistenceMapper();

        PaymentJpaEntity entity =
                mapper.toNewEntity(
                        payment,
                        java.time.LocalDateTime.of(
                                2026,
                                9,
                                24,
                                12,
                                0
                        )
                );

        when(
                paymentRepository.findByIdForUpdate(
                        payment.id().value()
                )
        ).thenReturn(
                Optional.of(entity)
        );

        when(
                orderRepository.findByPaymentId(
                        payment.id().value()
                )
        ).thenReturn(
                List.of(
                        mapper.toOrderEntity(
                                payment.id(),
                                payment.orders().getFirst(),
                                UUID.randomUUID(),
                                java.time.LocalDateTime.of(
                                        2026,
                                        9,
                                        24,
                                        12,
                                        0
                                )
                        )
                )
        );

        adapter.findByIdForUpdate(
                payment.id()
        );

        verify(paymentRepository)
                .findByIdForUpdate(
                        payment.id().value()
                );
    }

    private Payment codPayment() {
        return Payment.create(
                new PaymentId(UUID.randomUUID()),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.COD,
                Money.vnd(100_000),
                List.of(
                        new PaymentOrder(
                                new OrderId(UUID.randomUUID()),
                                new ShopId(UUID.randomUUID()),
                                Money.vnd(100_000)
                        )
                )
        );
    }
}