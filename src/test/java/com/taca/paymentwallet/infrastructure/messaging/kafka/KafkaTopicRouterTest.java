package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicRouterTest {

    private final KafkaTopicRouter router = new KafkaTopicRouter(
            new KafkaTopicProperties("payment.events.v1", "wallet.events.v1")
    );

    @Test
    void shouldRoutePaymentCreated() {
        Optional<String> topic = router.route(message("payment.created"));

        assertThat(topic).contains("payment.events.v1");
    }

    @Test
    void shouldRouteWalletAllocated() {
        Optional<String> topic = router.route(message("wallet.allocated"));

        assertThat(topic).contains("wallet.events.v1");
    }

    @Test
    void shouldNotRouteBlockedPaymentEvent() {
        assertThat(router.route(message("payment.succeeded"))).isEmpty();
    }

    @Test
    void shouldNotInventTopicForRefundEvent() {
        assertThat(router.route(message("refund.requested"))).isEmpty();
    }

    @Test
    void shouldNotInventTopicForPayoutRequested() {
        assertThat(router.route(message("payout.requested"))).isEmpty();
    }

    @Test
    void shouldNotRoutePaymentEventsMissingBuyerRecipientContract() {
        assertThat(router.route(message("payment.succeeded"))).isEmpty();
        assertThat(router.route(message("payment.failed"))).isEmpty();
        assertThat(router.route(message("payment.expired"))).isEmpty();
        assertThat(router.route(message("payment.refunded"))).isEmpty();
    }

    @Test
    void shouldNotRoutePayoutResultWithoutOwnerRecipientContract() {
        assertThat(router.route(message("payout.succeeded"))).isEmpty();
        assertThat(router.route(message("payout.failed"))).isEmpty();
    }

    private OutboxMessage message(String eventType) {
        return new OutboxMessage(
                UUID.randomUUID(),
                "PAYMENT",
                UUID.randomUUID(),
                eventType,
                "{}",
                null,
                Instant.parse("2026-09-30T10:00:00Z"),
                0
        );
    }
}