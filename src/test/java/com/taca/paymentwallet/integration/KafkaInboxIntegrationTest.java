package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.application.metadata.RequestMetadata;
import com.taca.paymentwallet.application.metadata.RequestMetadataContext;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaHeaderNames;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaInboxProcessor;
import com.taca.paymentwallet.infrastructure.persistence.entity.InboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.InboxEventJpaRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import(KafkaInboxIntegrationTest.TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KafkaInboxIntegrationTest {

    private static final String TOPIC =
            "payment-wallet.inbox.integration-test";

    private static final String GROUP_ID =
            "payment-wallet-inbox-integration-test";

    private static final String CONSUMER_NAME =
            "payment-wallet-integration-consumer";

    private static final String SOURCE =
            "integration-test-upstream";

    private static final String EVENT_TYPE =
            "integration.test.event";

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("kafka_inbox_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(
                    DockerImageName.parse(
                            "apache/kafka:3.8.0"
                    )
            );

    @DynamicPropertySource
    static void configureProperties(
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
                "spring.kafka.bootstrap-servers",
                KAFKA::getBootstrapServers
        );

        registry.add(
                "spring.kafka.consumer.group-id",
                () -> GROUP_ID
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
                "app.outbox.publisher.enabled",
                () -> "false"
        );

        registry.add(
                "vnpay.tmn-code",
                () -> "TEST_TMN"
        );

        registry.add(
                "vnpay.hash-secret",
                () -> "TEST_SECRET"
        );

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
    private InboxEventJpaRepository inboxRepository;

    @Autowired
    private TestInboxListener listener;

    @Autowired
    private com.taca.paymentwallet.application.port.out.OutboxMessagePublisherPort
            outboxMessagePublisherPort;

    @BeforeEach
    void setUp() {
        inboxRepository.deleteAll();
        inboxRepository.flush();

        listener.reset();
    }

    @Test
    void shouldConsumeKafkaRecordAndPersistProcessedInboxEvent()
            throws Exception {

        String eventId = UUID.randomUUID().toString();

        listener.expectRecords(1);

        send(
                eventId,
                "{\"value\":1}",
                "req-kafka-001",
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                "vendor=value"
        );

        assertThat(
                listener.await(Duration.ofSeconds(15))
        ).isTrue();

        assertThat(
                listener.actionCount()
        ).isEqualTo(1);

        InboxEventJpaEntity inboxEvent =
                inboxRepository
                        .findByConsumerNameAndSourceAndEventId(
                                CONSUMER_NAME,
                                SOURCE,
                                eventId
                        )
                        .orElseThrow();

        assertThat(inboxEvent.getEventType())
                .isEqualTo(EVENT_TYPE);

        assertThat(inboxEvent.getStatus())
                .isEqualTo("PROCESSED");

        assertThat(inboxEvent.getProcessedAt())
                .isNotNull();

        assertThat(inboxEvent.getPayloadHash())
                .hasSize(64);

        assertThat(inboxEvent.getFailureCode())
                .isNull();
    }

    @Test
    void shouldIgnoreDuplicateKafkaDeliveryByEventId()
            throws Exception {

        String eventId = UUID.randomUUID().toString();

        String payload = "{\"value\":2}";

        listener.expectRecords(2);

        send(
                eventId,
                payload,
                null,
                null,
                null
        );

        send(
                eventId,
                payload,
                null,
                null,
                null
        );

        assertThat(
                listener.await(Duration.ofSeconds(15))
        ).isTrue();

        assertThat(
                listener.actionCount()
        ).isEqualTo(1);

        InboxEventJpaEntity inboxEvent =
                inboxRepository
                        .findByConsumerNameAndSourceAndEventId(
                                CONSUMER_NAME,
                                SOURCE,
                                eventId
                        )
                        .orElseThrow();

        assertThat(inboxEvent.getStatus())
                .isEqualTo("PROCESSED");

        assertThat(
                inboxRepository.count()
        ).isEqualTo(1);
    }

    @Test
    void shouldPropagateKafkaTracingMetadataIntoAction()
            throws Exception {

        String eventId = UUID.randomUUID().toString();

        listener.expectRecords(1);

        send(
                eventId,
                "{\"value\":3}",
                "req-kafka-trace-001",
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                "vendor=value"
        );

        assertThat(
                listener.await(Duration.ofSeconds(15))
        ).isTrue();

        RequestMetadata metadata =
                listener.capturedMetadata();

        assertThat(metadata)
                .isNotNull();

        assertThat(metadata.requestId())
                .isEqualTo(
                        "req-kafka-trace-001"
                );

        assertThat(metadata.traceparent())
                .isEqualTo(
                        "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
                );

        assertThat(metadata.tracestate())
                .isEqualTo("vendor=value");
    }

    private void send(
            String eventId,
            String payload,
            String requestId,
            String traceparent,
            String tracestate
    ) throws Exception {

        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        TOPIC,
                        eventId,
                        payload
                );

        addHeader(
                record,
                KafkaHeaderNames.EVENT_ID,
                eventId
        );

        addHeader(
                record,
                KafkaHeaderNames.REQUEST_ID,
                requestId
        );

        addHeader(
                record,
                KafkaHeaderNames.TRACEPARENT,
                traceparent
        );

        addHeader(
                record,
                KafkaHeaderNames.TRACESTATE,
                tracestate
        );

        kafkaTemplate.send(record)
                .get(10, TimeUnit.SECONDS);
    }

    private void addHeader(
            ProducerRecord<String, String> record,
            String name,
            String value
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        record.headers().add(
                name,
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    @TestConfiguration
    @EnableKafka
    static class TestConfig {

        @Bean
        NewTopic kafkaInboxIntegrationTopic() {
            return TopicBuilder
                    .name(TOPIC)
                    .partitions(1)
                    .replicas(1)
                    .build();
        }

        @Bean
        TestInboxListener testInboxListener(
                KafkaInboxProcessor processor
        ) {
            return new TestInboxListener(
                    processor
            );
        }
    }

    static class TestInboxListener {

        private final KafkaInboxProcessor processor;

        private final AtomicInteger actionCount =
                new AtomicInteger();

        private final AtomicReference<RequestMetadata>
                capturedMetadata =
                new AtomicReference<>();

        private volatile CountDownLatch latch =
                new CountDownLatch(0);

        TestInboxListener(
                KafkaInboxProcessor processor
        ) {
            this.processor = processor;
        }

        @KafkaListener(
                topics = TOPIC,
                groupId = GROUP_ID
        )
        void consume(
                ConsumerRecord<String, String> record
        ) {
            try {
                processor.process(
                        CONSUMER_NAME,
                        SOURCE,
                        EVENT_TYPE,
                        record,
                        () -> {
                            actionCount.incrementAndGet();

                            RequestMetadataContext.current()
                                    .ifPresent(
                                            capturedMetadata::set
                                    );

                            return null;
                        }
                );
            } finally {
                latch.countDown();
            }
        }

        void reset() {
            actionCount.set(0);
            capturedMetadata.set(null);
            latch = new CountDownLatch(0);
        }

        void expectRecords(int count) {
            latch = new CountDownLatch(count);
        }

        boolean await(Duration timeout)
                throws InterruptedException {

            return latch.await(
                    timeout.toMillis(),
                    TimeUnit.MILLISECONDS
            );
        }

        int actionCount() {
            return actionCount.get();
        }

        RequestMetadata capturedMetadata() {
            return capturedMetadata.get();
        }
    }

    @Test
    void shouldPublishOutboxMessageWithHeadersToRealKafkaBroker()
            throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        String headers = """
            {
              "eventId":"%s",
              "requestId":"req-producer-001",
              "traceparent":"00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
              "tracestate":"vendor=value"
            }
            """.formatted(eventId);

        var message =
                new com.taca.paymentwallet.application.outbox.OutboxMessage(
                        eventId,
                        "PAYMENT",
                        aggregateId,
                        "payment.created",
                        "{\"status\":\"PENDING\"}",
                        headers,
                        java.time.Instant.parse(
                                "2026-10-02T00:00:00Z"
                        ),
                        0
                );

        try (
                KafkaConsumer<String, String> consumer =
                        new KafkaConsumer<>(
                                consumerProperties()
                        )
        ) {
            consumer.subscribe(
                    List.of("payment.events.v1")
            );

            outboxMessagePublisherPort.publish(
                    message
            );

            ConsumerRecord<String, String> received =
                    awaitRecord(
                            consumer,
                            eventId.toString()
                    );

            assertThat(received.key())
                    .isEqualTo(
                            aggregateId.toString()
                    );

            assertThat(received.value())
                    .isEqualTo(
                            "{\"status\":\"PENDING\"}"
                    );

            assertThat(
                    headerValue(
                            received,
                            KafkaHeaderNames.EVENT_ID
                    )
            ).isEqualTo(
                    eventId.toString()
            );

            assertThat(
                    headerValue(
                            received,
                            KafkaHeaderNames.REQUEST_ID
                    )
            ).isEqualTo(
                    "req-producer-001"
            );

            assertThat(
                    headerValue(
                            received,
                            KafkaHeaderNames.TRACEPARENT
                    )
            ).isEqualTo(
                    "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
            );
        }
    }

    private Properties consumerProperties() {
        Properties properties = new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers()
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "payment-wallet-producer-integration-"
                        + UUID.randomUUID()
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                "false"
        );

        return properties;
    }

    private ConsumerRecord<String, String> awaitRecord(
            KafkaConsumer<String, String> consumer,
            String expectedEventId
    ) {

        long deadline =
                System.nanoTime()
                        + Duration.ofSeconds(15)
                        .toNanos();

        while (System.nanoTime() < deadline) {

            var records =
                    consumer.poll(
                            Duration.ofMillis(500)
                    );

            for (ConsumerRecord<String, String> record : records) {
                String eventId =
                        headerValue(
                                record,
                                KafkaHeaderNames.EVENT_ID
                        );

                if (expectedEventId.equals(eventId)) {
                    return record;
                }
            }
        }

        throw new AssertionError(
                "Kafka record not received for event "
                        + expectedEventId
        );
    }

    private String headerValue(
            ConsumerRecord<String, String> record,
            String name
    ) {
        var header =
                record.headers()
                        .lastHeader(name);

        if (header == null) {
            return null;
        }

        return new String(
                header.value(),
                StandardCharsets.UTF_8
        );
    }
}
