package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.application.port.out.PaymentRepositoryPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.infrastructure.messaging.kafka.shipment.ShipmentEventListener;
import com.taca.paymentwallet.infrastructure.persistence.entity.InboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentOrderJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.WalletJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import(ShipmentCodKafkaIntegrationTest.TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ShipmentCodKafkaIntegrationTest {

    private static final String TOPIC = "shipment.events.v1";
    private static final String CONSUMER_NAME =
            ShipmentEventListener.CONSUMER_NAME;
    private static final String SOURCE =
            ShipmentEventListener.SOURCE;

    private static final UUID SHOP_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");

    private static final UUID WALLET_ID =
            UUID.fromString("21000000-0000-0000-0000-000000000001");

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("shipment_cod_kafka_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(
                    DockerImageName.parse("apache/kafka:3.8.0")
            );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add(
                "spring.kafka.consumer.group-id",
                () -> "payment-wallet-shipment-integration"
        );
        registry.add(
                "spring.kafka.consumer.auto-offset-reset",
                () -> "earliest"
        );
        registry.add(
                "spring.kafka.consumer.enable-auto-commit",
                () -> "false"
        );
        registry.add(
                "spring.kafka.listener.ack-mode",
                () -> "record"
        );
        registry.add(
                "app.kafka.topics.shipment-events",
                () -> TOPIC
        );
        registry.add(
                "app.outbox.publisher.enabled",
                () -> "false"
        );
        registry.add("vnpay.tmn-code", () -> "TEST_TMN");
        registry.add("vnpay.hash-secret", () -> "TEST_SECRET");
        registry.add(
                "vnpay.payment-url",
                () -> "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
        );
        registry.add(
                "vnpay.return-url",
                () -> "http://localhost/payment-return"
        );
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PaymentRepositoryPort paymentRepository;

    @Autowired
    private TransactionPort transactionPort;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private PaymentOrderJpaRepository paymentOrderJpaRepository;

    @Autowired
    private PaymentAllocationJpaRepository paymentAllocationJpaRepository;

    @Autowired
    private LedgerPostingJpaRepository ledgerPostingJpaRepository;

    @Autowired
    private WalletJpaRepository walletJpaRepository;

    @Autowired
    private InboxEventJpaRepository inboxEventJpaRepository;

    @BeforeEach
    void setUp() {
        inboxEventJpaRepository.deleteAll();
        inboxEventJpaRepository.flush();

        WalletJpaEntity wallet =
                walletJpaRepository.findById(WALLET_ID).orElseThrow();

        wallet.setAvailableBalance(31_000L);
        wallet.setPendingBalance(0L);

        walletJpaRepository.saveAndFlush(wallet);
    }

    @Test
    void shouldCaptureCodPaymentFromShipmentDeliveredEvent()
            throws Exception {

        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String eventId = UUID.randomUUID().toString();

        createCodPayment(paymentId, orderId);

        Instant occurredAt =
                Instant.parse("2026-10-04T06:00:00Z");

        Instant deliveredAt =
                Instant.parse("2026-10-04T05:55:00Z");

        send(deliveredEvent(
                eventId,
                orderId,
                occurredAt,
                deliveredAt
        ));

        await(() ->
                paymentOrderJpaRepository
                        .findByOrderId(orderId)
                        .map(order -> "CAPTURED".equals(order.getCodStatus()))
                        .orElse(false)
        );

        PaymentJpaEntity payment =
                paymentJpaRepository.findById(paymentId).orElseThrow();

        PaymentOrderJpaEntity order =
                paymentOrderJpaRepository
                        .findByOrderId(orderId)
                        .orElseThrow();

        WalletJpaEntity wallet =
                walletJpaRepository.findById(WALLET_ID).orElseThrow();

        assertThat(payment.getStatus())
                .isEqualTo("SUCCESS");

        assertThat(payment.getCapturedAmount())
                .isEqualTo(100_000L);

        assertThat(order.getCodStatus())
                .isEqualTo("CAPTURED");

        assertThat(order.getCodProcessedAt())
                .isEqualTo(
                        PersistenceTimeMapper.toLocalDateTime(deliveredAt)
                );

        assertThat(
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(paymentId)
        ).hasSize(1);

        assertThat(
                ledgerPostingJpaRepository.findByBusinessKey(
                        "COD_CAPTURE:" + paymentId + ":" + orderId
                )
        ).isPresent();

        assertThat(wallet.getPendingBalance())
                .isEqualTo(90_000L);

        assertInboxProcessed(
                eventId,
                "shipment.delivered"
        );
    }

    @Test
    void shouldFailCodPaymentFromShipmentFailedEvent()
            throws Exception {

        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String eventId = UUID.randomUUID().toString();

        createCodPayment(paymentId, orderId);

        Instant occurredAt =
                Instant.parse("2026-10-04T06:10:00Z");

        send(failedEvent(
                eventId,
                orderId,
                occurredAt
        ));

        await(() ->
                paymentOrderJpaRepository
                        .findByOrderId(orderId)
                        .map(order -> "FAILED".equals(order.getCodStatus()))
                        .orElse(false)
        );

        PaymentJpaEntity payment =
                paymentJpaRepository.findById(paymentId).orElseThrow();

        PaymentOrderJpaEntity order =
                paymentOrderJpaRepository
                        .findByOrderId(orderId)
                        .orElseThrow();

        assertThat(payment.getStatus())
                .isEqualTo("FAILED");

        assertThat(payment.getCapturedAmount())
                .isZero();

        assertThat(payment.getFailureCode())
                .isEqualTo("SHIPMENT_FAILED");

        assertThat(order.getCodStatus())
                .isEqualTo("FAILED");

        assertThat(order.getCodFailureCode())
                .isEqualTo("SHIPMENT_FAILED");

        assertThat(order.getCodProcessedAt())
                .isEqualTo(
                        PersistenceTimeMapper.toLocalDateTime(occurredAt)
                );

        assertThat(
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(paymentId)
        ).isEmpty();

        assertThat(
                ledgerPostingJpaRepository.findByBusinessKey(
                        "COD_CAPTURE:" + paymentId + ":" + orderId
                )
        ).isEmpty();

        assertInboxProcessed(
                eventId,
                "shipment.failed"
        );
    }

    @Test
    void shouldNotRegressFailedOrderWhenLateDeliveredEventArrives()
            throws Exception {

        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        createCodPayment(paymentId, orderId);

        String failedEventId =
                UUID.randomUUID().toString();

        send(failedEvent(
                failedEventId,
                orderId,
                Instant.parse("2026-10-04T06:20:00Z")
        ));

        await(() ->
                paymentOrderJpaRepository
                        .findByOrderId(orderId)
                        .map(order -> "FAILED".equals(order.getCodStatus()))
                        .orElse(false)
        );

        String deliveredEventId =
                UUID.randomUUID().toString();

        send(deliveredEvent(
                deliveredEventId,
                orderId,
                Instant.parse("2026-10-04T06:30:00Z"),
                Instant.parse("2026-10-04T06:25:00Z")
        ));

        await(() ->
                inboxEventJpaRepository
                        .findByConsumerNameAndSourceAndEventId(
                                CONSUMER_NAME,
                                SOURCE,
                                deliveredEventId
                        )
                        .map(event -> "PROCESSED".equals(event.getStatus()))
                        .orElse(false)
        );

        PaymentJpaEntity payment =
                paymentJpaRepository.findById(paymentId).orElseThrow();

        PaymentOrderJpaEntity order =
                paymentOrderJpaRepository
                        .findByOrderId(orderId)
                        .orElseThrow();

        assertThat(payment.getStatus())
                .isEqualTo("FAILED");

        assertThat(payment.getCapturedAmount())
                .isZero();

        assertThat(order.getCodStatus())
                .isEqualTo("FAILED");

        assertThat(order.getCodFailureCode())
                .isEqualTo("SHIPMENT_FAILED");

        assertThat(
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(paymentId)
        ).isEmpty();

        assertThat(
                ledgerPostingJpaRepository.findByBusinessKey(
                        "COD_CAPTURE:" + paymentId + ":" + orderId
                )
        ).isEmpty();

        assertInboxProcessed(
                failedEventId,
                "shipment.failed"
        );

        assertInboxProcessed(
                deliveredEventId,
                "shipment.delivered"
        );
    }

    private void createCodPayment(
            UUID paymentId,
            UUID orderId
    ) {
        Payment payment = Payment.create(
                new PaymentId(paymentId),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.COD,
                Money.vnd(100_000),
                List.of(new PaymentOrder(
                        new OrderId(orderId),
                        new ShopId(SHOP_ID),
                        Money.vnd(100_000)
                ))
        );

        payment.clearDomainEvents();

        transactionPort.execute(
                () -> paymentRepository.save(payment)
        );
    }

    private void send(String payload)
            throws Exception {

        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        TOPIC,
                        UUID.randomUUID().toString(),
                        payload
                );

        kafkaTemplate.send(record)
                .get(10, TimeUnit.SECONDS);
    }

    private String deliveredEvent(
            String eventId,
            UUID orderId,
            Instant occurredAt,
            Instant deliveredAt
    ) {
        UUID shipmentId = UUID.randomUUID();

        return """
                {
                  "event_id":"%s",
                  "schema_version":1,
                  "event_type":"shipment.delivered",
                  "occurred_at":"%s",
                  "aggregate_type":"SHIPMENT",
                  "aggregate_id":"%s",
                  "payload":{
                    "shipment_id":"%s",
                    "order_id":"%s",
                    "delivered_at":"%s"
                  }
                }
                """.formatted(
                eventId,
                occurredAt,
                shipmentId,
                shipmentId,
                orderId,
                deliveredAt
        );
    }

    private String failedEvent(
            String eventId,
            UUID orderId,
            Instant occurredAt
    ) {
        UUID shipmentId = UUID.randomUUID();

        return """
                {
                  "event_id":"%s",
                  "schema_version":1,
                  "event_type":"shipment.failed",
                  "occurred_at":"%s",
                  "aggregate_type":"SHIPMENT",
                  "aggregate_id":"%s",
                  "payload":{
                    "shipment_id":"%s",
                    "order_id":"%s",
                    "reason":"DELIVERY_FAILED",
                    "source":"GHN"
                  }
                }
                """.formatted(
                eventId,
                occurredAt,
                shipmentId,
                shipmentId,
                orderId
        );
    }

    private void assertInboxProcessed(
            String eventId,
            String eventType
    ) {
        InboxEventJpaEntity inbox =
                inboxEventJpaRepository
                        .findByConsumerNameAndSourceAndEventId(
                                CONSUMER_NAME,
                                SOURCE,
                                eventId
                        )
                        .orElseThrow();

        assertThat(inbox.getStatus())
                .isEqualTo("PROCESSED");

        assertThat(inbox.getEventType())
                .isEqualTo(eventType);

        assertThat(inbox.getProcessedAt())
                .isNotNull();
    }

    private void await(BooleanSupplier condition)
            throws InterruptedException {

        long deadline =
                System.nanoTime()
                        + TimeUnit.SECONDS.toNanos(15);

        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }

            Thread.sleep(100);
        }

        throw new AssertionError(
                "Condition was not satisfied within 15 seconds"
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        NewTopic shipmentEventsIntegrationTopic() {
            return TopicBuilder
                    .name(TOPIC)
                    .partitions(1)
                    .replicas(1)
                    .build();
        }
    }
}