package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.inbox.InboxEvent;
import com.taca.paymentwallet.application.inbox.InboxEventKey;
import com.taca.paymentwallet.application.inbox.InboxEventStatus;
import com.taca.paymentwallet.application.paymentevent.PaymentProviderEvent;
import com.taca.paymentwallet.application.paymentevent.PaymentProviderEventStatus;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.infrastructure.persistence.entity.InboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.InboxEventJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentEventJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class EventDedupePersistenceIntegrationTest {

    private static final Instant RECEIVED_AT =
            Instant.parse(
                    "2026-09-25T07:00:00Z"
            );

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4.0")
                    .withDatabaseName(
                            "payment_wallet_event_dedupe_test"
                    )
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                MYSQL::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                MYSQL::getUsername
        );

        registry.add(
                "spring.datasource.password",
                MYSQL::getPassword
        );

        registry.add(
                "spring.flyway.enabled",
                () -> "true"
        );
    }

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private PaymentEventJpaRepository paymentEventRepository;

    @Autowired
    private InboxEventJpaRepository inboxEventRepository;

    @Test
    void shouldAtomicallyDeduplicatePaymentProviderEvent() {
        PaymentJpaEntity payment =
                paymentRepository.saveAndFlush(
                        newPayment()
                );

        PaymentProviderEventAdapter adapter =
                new PaymentProviderEventAdapter(
                        paymentEventRepository,
                        fixedClock(),
                        new PersistenceUuidGenerator()
                );

        PaymentProviderEvent first =
                providerEvent(
                        payment.getId(),
                        "provider-event-001",
                        "payload-hash-first"
                );

        PaymentProviderEvent duplicate =
                providerEvent(
                        payment.getId(),
                        "provider-event-001",
                        "payload-hash-duplicate"
                );

        boolean firstInserted =
                adapter.recordIfAbsent(first);

        boolean duplicateInserted =
                adapter.recordIfAbsent(duplicate);

        assertThat(firstInserted)
                .isTrue();

        assertThat(duplicateInserted)
                .isFalse();

        Optional<PaymentEventJpaEntity> stored =
                paymentEventRepository
                        .findByProviderAndProviderEventId(
                                "VNPAY",
                                "provider-event-001"
                        );

        assertThat(stored)
                .isPresent();

        /*
         * Quan trọng:
         * duplicate không được overwrite row đầu tiên.
         */
        assertThat(
                stored.orElseThrow()
                        .getPayloadHash()
        ).isEqualTo(
                "payload-hash-first"
        );
    }

    @Test
    void shouldAllowDifferentProviderEventIds() {
        PaymentJpaEntity payment =
                paymentRepository.saveAndFlush(
                        newPayment()
                );

        PaymentProviderEventAdapter adapter =
                new PaymentProviderEventAdapter(
                        paymentEventRepository,
                        fixedClock(),
                        new PersistenceUuidGenerator()
                );

        boolean first =
                adapter.recordIfAbsent(
                        providerEvent(
                                payment.getId(),
                                "provider-event-A",
                                "hash-A"
                        )
                );

        boolean second =
                adapter.recordIfAbsent(
                        providerEvent(
                                payment.getId(),
                                "provider-event-B",
                                "hash-B"
                        )
                );

        assertThat(first).isTrue();
        assertThat(second).isTrue();

        assertThat(
                paymentEventRepository
                        .findByProviderAndProviderEventId(
                                "VNPAY",
                                "provider-event-A"
                        )
        ).isPresent();

        assertThat(
                paymentEventRepository
                        .findByProviderAndProviderEventId(
                                "VNPAY",
                                "provider-event-B"
                        )
        ).isPresent();
    }

    @Test
    void shouldAtomicallyDeduplicateInboxEvent() {
        InboxEventAdapter adapter =
                new InboxEventAdapter(
                        inboxEventRepository,
                        new PersistenceUuidGenerator()
                );

        InboxEvent first =
                inboxEvent(
                        "shipment-event-001",
                        "payload-first"
                );

        InboxEvent duplicate =
                inboxEvent(
                        "shipment-event-001",
                        "payload-duplicate"
                );

        boolean firstInserted =
                adapter.recordIfAbsent(first);

        boolean duplicateInserted =
                adapter.recordIfAbsent(duplicate);

        assertThat(firstInserted)
                .isTrue();

        assertThat(duplicateInserted)
                .isFalse();

        Optional<InboxEventJpaEntity> stored =
                inboxEventRepository
                        .findByConsumerNameAndSourceAndEventId(
                                "payment-wallet-shipment-consumer",
                                "shipment-service",
                                "shipment-event-001"
                        );

        assertThat(stored)
                .isPresent();

        /*
         * INSERT IGNORE phải giữ row đầu tiên,
         * không biến duplicate thành update.
         */
        assertThat(
                stored.orElseThrow()
                        .getPayloadHash()
        ).isEqualTo(
                "payload-first"
        );
    }

    @Test
    void shouldScopeInboxDedupeByConsumerSourceAndEventId() {
        InboxEventAdapter adapter =
                new InboxEventAdapter(
                        inboxEventRepository,
                        new PersistenceUuidGenerator()
                );

        InboxEvent firstConsumer =
                new InboxEvent(
                        new InboxEventKey(
                                "consumer-A",
                                "shipment-service",
                                "same-event-id"
                        ),
                        "shipment.delivered",
                        "payload-A",
                        RECEIVED_AT,
                        InboxEventStatus.RECEIVED
                );

        InboxEvent secondConsumer =
                new InboxEvent(
                        new InboxEventKey(
                                "consumer-B",
                                "shipment-service",
                                "same-event-id"
                        ),
                        "shipment.delivered",
                        "payload-B",
                        RECEIVED_AT,
                        InboxEventStatus.RECEIVED
                );

        assertThat(
                adapter.recordIfAbsent(
                        firstConsumer
                )
        ).isTrue();

        assertThat(
                adapter.recordIfAbsent(
                        secondConsumer
                )
        ).isTrue();
    }

    @Test
    void shouldScopeInboxDedupeBySource() {
        InboxEventAdapter adapter =
                new InboxEventAdapter(
                        inboxEventRepository,
                        new PersistenceUuidGenerator()
                );

        InboxEvent firstSource =
                new InboxEvent(
                        new InboxEventKey(
                                "payment-wallet-consumer",
                                "shipment-service",
                                "same-event-id"
                        ),
                        "shipment.delivered",
                        "payload-A",
                        RECEIVED_AT,
                        InboxEventStatus.RECEIVED
                );

        InboxEvent secondSource =
                new InboxEvent(
                        new InboxEventKey(
                                "payment-wallet-consumer",
                                "order-service",
                                "same-event-id"
                        ),
                        "order.updated",
                        "payload-B",
                        RECEIVED_AT,
                        InboxEventStatus.RECEIVED
                );

        assertThat(
                adapter.recordIfAbsent(
                        firstSource
                )
        ).isTrue();

        assertThat(
                adapter.recordIfAbsent(
                        secondSource
                )
        ).isTrue();
    }

    private ClockPort fixedClock() {
        return () ->
                Instant.parse(
                        "2026-09-25T07:01:00Z"
                );
    }

    private PaymentProviderEvent providerEvent(
            UUID paymentId,
            String providerEventId,
            String payloadHash
    ) {
        return new PaymentProviderEvent(
                "VNPAY",
                providerEventId,
                "VNPAY-TXN-" + providerEventId,
                new PaymentId(paymentId),
                "00",
                "00",
                Money.vnd(100_000),
                payloadHash,
                RECEIVED_AT,
                PaymentProviderEventStatus.RECEIVED
        );
    }

    private InboxEvent inboxEvent(
            String eventId,
            String payloadHash
    ) {
        return new InboxEvent(
                new InboxEventKey(
                        "payment-wallet-shipment-consumer",
                        "shipment-service",
                        eventId
                ),
                "shipment.delivered",
                payloadHash,
                RECEIVED_AT,
                InboxEventStatus.RECEIVED
        );
    }

    private PaymentJpaEntity newPayment() {
        LocalDateTime now =
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        7,
                        0
                );

        PaymentJpaEntity payment =
                new PaymentJpaEntity();

        payment.setId(
                UUID.randomUUID()
        );

        payment.setCheckoutGroupId(
                UUID.randomUUID()
        );

        payment.setBuyerUserId(
                UUID.randomUUID()
        );

        payment.setMethod("VNPAY");

        payment.setAmount(
                100_000L
        );

        payment.setCurrency("VND");

        payment.setStatus("PENDING");

        payment.setCapturedAmount(0L);
        payment.setRefundedAmount(0L);

        payment.setExpiresAt(
                now.plusMinutes(15)
        );

        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        return payment;
    }
}