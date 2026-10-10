package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.application.port.out.PaymentRepositoryPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.security.ActorContextHolder;
import com.taca.paymentwallet.application.security.InternalCallerContext;
import com.taca.paymentwallet.application.security.InternalCallerContextHolder;
import com.taca.paymentwallet.application.security.InternalService;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.BuyerUserId;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.RefundJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.RefundJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class RequestRefundHttpE2ETest {

    private static final long PAYMENT_AMOUNT = 100_000L;

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4.0")
                    .withDatabaseName("payment_wallet_refund_http_e2e")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("app.outbox.publisher.enabled", () -> false);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepositoryPort paymentRepository;

    @Autowired
    private TransactionPort transactionPort;

    @Autowired
    private RefundJpaRepository refundJpaRepository;

    @Autowired
    private IdempotencyKeyJpaRepository idempotencyKeyJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @AfterEach
    void clearSecurityContexts() {
        InternalCallerContextHolder.clear();
        ActorContextHolder.clear();
    }

    @Test
    void shouldRequestRefundAsOrderCommerceAndPersistRefund() throws Exception {
        UUID paymentId = UUID.randomUUID();
        String idempotencyKey = "refund-e2e-" + paymentId;

        seedSucceededPayment(paymentId);
        setOrderCommerceCaller();

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", paymentId)
                                .header("Idempotency-Key", idempotencyKey)
                                .header("X-Request-ID", "req-refund-e2e-success")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refundBody(50_000))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.payment_id").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.amount").value(50_000))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.meta.request_id").value("req-refund-e2e-success"));

        RefundJpaEntity refund = refundJpaRepository
                .findByPaymentIdAndIdempotencyKey(paymentId, idempotencyKey)
                .orElseThrow();

        assertThat(refund.getPaymentId()).isEqualTo(paymentId);
        assertThat(refund.getAmount()).isEqualTo(50_000L);
        assertThat(refund.getCurrency()).isEqualTo("VND");
        assertThat(refund.getReason()).isEqualTo("Buyer requested refund");
        assertThat(refund.getStatus()).isEqualTo("REQUESTED");
        assertThat(refund.getFailureCode()).isNull();

        assertIdempotencySucceeded(paymentId, idempotencyKey);
        assertRefundRequestedOutbox(refund.getId(), "req-refund-e2e-success");
    }

    @Test
    void shouldReturnSameRefundForSameIdempotencyRequest() throws Exception {
        UUID paymentId = UUID.randomUUID();
        String idempotencyKey = "refund-e2e-duplicate-" + paymentId;

        seedSucceededPayment(paymentId);
        setOrderCommerceCaller();

        performRefund(
                paymentId,
                idempotencyKey,
                "req-refund-e2e-duplicate-1",
                50_000
        );

        RefundJpaEntity firstRefund = refundJpaRepository
                .findByPaymentIdAndIdempotencyKey(paymentId, idempotencyKey)
                .orElseThrow();

        long refundCountBefore = refundJpaRepository.count();
        long outboxCountBefore = refundOutboxCount(firstRefund.getId());

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", paymentId)
                                .header("Idempotency-Key", idempotencyKey)
                                .header("X-Request-ID", "req-refund-e2e-duplicate-2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refundBody(50_000))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.refund_id").value(firstRefund.getId().toString()))
                .andExpect(jsonPath("$.data.payment_id").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.amount").value(50_000))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"));

        assertThat(refundJpaRepository.count()).isEqualTo(refundCountBefore);
        assertThat(refundOutboxCount(firstRefund.getId())).isEqualTo(outboxCountBefore);
    }

    @Test
    void shouldRejectSameIdempotencyKeyWithDifferentRefundRequest() throws Exception {
        UUID paymentId = UUID.randomUUID();
        String idempotencyKey = "refund-e2e-conflict-" + paymentId;

        seedSucceededPayment(paymentId);
        setOrderCommerceCaller();

        performRefund(
                paymentId,
                idempotencyKey,
                "req-refund-e2e-conflict-1",
                40_000
        );

        long refundCountBefore = refundJpaRepository.count();

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", paymentId)
                                .header("Idempotency-Key", idempotencyKey)
                                .header("X-Request-ID", "req-refund-e2e-conflict-2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refundBody(50_000))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_IDEMPOTENCY_CONFLICT"))
                .andExpect(jsonPath("$.meta.request_id").value("req-refund-e2e-conflict-2"));

        assertThat(refundJpaRepository.count()).isEqualTo(refundCountBefore);
    }

    @Test
    void shouldRejectRefundAmountGreaterThanCapturedAmount() throws Exception {
        UUID paymentId = UUID.randomUUID();
        String idempotencyKey = "refund-e2e-limit-" + paymentId;

        seedSucceededPayment(paymentId);
        setOrderCommerceCaller();

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", paymentId)
                                .header("Idempotency-Key", idempotencyKey)
                                .header("X-Request-ID", "req-refund-e2e-limit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refundBody(150_000))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REFUND_AMOUNT_INVALID"))
                .andExpect(jsonPath("$.meta.request_id").value("req-refund-e2e-limit"));

        assertThat(
                refundJpaRepository.findByPaymentIdAndIdempotencyKey(
                        paymentId,
                        idempotencyKey
                )
        ).isEmpty();

        assertThat(findRefundIdempotency(paymentId, idempotencyKey)).isEmpty();
    }

    @Test
    void shouldRejectRefundWhenPaymentStateIsPending() throws Exception {
        UUID paymentId = UUID.randomUUID();
        String idempotencyKey = "refund-e2e-state-" + paymentId;

        seedPendingPayment(paymentId);
        setOrderCommerceCaller();

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", paymentId)
                                .header("Idempotency-Key", idempotencyKey)
                                .header("X-Request-ID", "req-refund-e2e-state")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refundBody(50_000))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REFUND_STATE_INVALID"));

        assertThat(
                refundJpaRepository.findByPaymentIdAndIdempotencyKey(
                        paymentId,
                        idempotencyKey
                )
        ).isEmpty();
    }

    @Test
    void shouldReturnNotFoundForUnknownPayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        String idempotencyKey = "refund-e2e-missing-" + paymentId;

        setOrderCommerceCaller();

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", paymentId)
                                .header("Idempotency-Key", idempotencyKey)
                                .header("X-Request-ID", "req-refund-e2e-missing")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refundBody(50_000))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_NOT_FOUND"));

        assertThat(findRefundIdempotency(paymentId, idempotencyKey)).isEmpty();
    }

    @Test
    void shouldRejectAnonymousRefundCaller() throws Exception {
        UUID paymentId = UUID.randomUUID();

        seedSucceededPayment(paymentId);
        InternalCallerContextHolder.clear();

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", paymentId)
                                .header("Idempotency-Key", "refund-e2e-anonymous-" + paymentId)
                                .header("X-Request-ID", "req-refund-e2e-anonymous")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refundBody(50_000))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_UNAUTHENTICATED"));

        assertThat(refundJpaRepository.findAll()
                .stream()
                .noneMatch(refund -> paymentId.equals(refund.getPaymentId())))
                .isTrue();
    }

    @Test
    void shouldRejectNonFinanceActor() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        seedSucceededPayment(paymentId);
        InternalCallerContextHolder.clear();

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", paymentId)
                                .header("Idempotency-Key", "refund-e2e-seller-" + paymentId)
                                .header("X-Request-ID", "req-refund-e2e-seller")
                                .header("X-User-ID", actorId.toString())
                                .header("X-User-Roles", "SELLER")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refundBody(50_000))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_FORBIDDEN"));

        assertThat(refundJpaRepository.findAll()
                .stream()
                .noneMatch(refund -> paymentId.equals(refund.getPaymentId())))
                .isTrue();
    }

    @Test
    void shouldAllowFinanceOpsToRequestRefund() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        String idempotencyKey = "refund-e2e-finance-" + paymentId;

        seedSucceededPayment(paymentId);
        InternalCallerContextHolder.clear();

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", paymentId)
                                .header("Idempotency-Key", idempotencyKey)
                                .header("X-Request-ID", "req-refund-e2e-finance")
                                .header("X-User-ID", actorId.toString())
                                .header("X-User-Roles", "FINANCE_OPS")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refundBody(50_000))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.payment_id").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"));

        assertThat(
                refundJpaRepository.findByPaymentIdAndIdempotencyKey(
                        paymentId,
                        idempotencyKey
                )
        ).isPresent();
    }

    private void performRefund(
            UUID paymentId,
            String idempotencyKey,
            String requestId,
            long amount
    ) throws Exception {
        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", paymentId)
                                .header("Idempotency-Key", idempotencyKey)
                                .header("X-Request-ID", requestId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refundBody(amount))
                )
                .andExpect(status().isAccepted());
    }

    private void setOrderCommerceCaller() {
        InternalCallerContextHolder.set(
                new InternalCallerContext(
                        InternalService.ORDER_COMMERCE
                )
        );
    }

    private void seedSucceededPayment(UUID paymentId) {
        Payment payment = newPayment(paymentId);

        payment.markSucceeded(
                Instant.parse("2026-10-10T06:00:00Z")
        );

        payment.clearDomainEvents();

        transactionPort.execute(
                () -> paymentRepository.save(payment)
        );
    }

    private void seedPendingPayment(UUID paymentId) {
        Payment payment = newPayment(paymentId);

        payment.clearDomainEvents();

        transactionPort.execute(
                () -> paymentRepository.save(payment)
        );
    }

    private Payment newPayment(UUID paymentId) {
        return Payment.create(
                new PaymentId(paymentId),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.VNPAY,
                Money.vnd(PAYMENT_AMOUNT),
                List.of(
                        new PaymentOrder(
                                new OrderId(UUID.randomUUID()),
                                new ShopId(UUID.randomUUID()),
                                Money.vnd(90_000),
                                Money.vnd(10_000)
                        )
                ),
                Instant.parse("2026-10-10T07:00:00Z")
        );
    }

    private String refundBody(long amount) {
        return """
                {
                  "amount": %d,
                  "reason": "Buyer requested refund"
                }
                """.formatted(amount);
    }

    private void assertIdempotencySucceeded(
            UUID paymentId,
            String idempotencyKey
    ) {
        IdempotencyKeyJpaEntity entity =
                findRefundIdempotency(
                        paymentId,
                        idempotencyKey
                ).orElseThrow();

        assertThat(entity.getScope()).isEqualTo("REFUND");
        assertThat(entity.getScopeId()).isEqualTo(paymentId.toString());
        assertThat(entity.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(entity.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(entity.getResponseSnapshot()).isNotBlank();
        assertThat(entity.getFailureCode()).isNull();
    }

    private java.util.Optional<IdempotencyKeyJpaEntity> findRefundIdempotency(
            UUID paymentId,
            String idempotencyKey
    ) {
        return idempotencyKeyJpaRepository
                .findByScopeAndScopeIdAndIdempotencyKey(
                        "REFUND",
                        paymentId.toString(),
                        idempotencyKey
                );
    }

    private void assertRefundRequestedOutbox(
            UUID refundId,
            String requestId
    ) {
        List<OutboxEventJpaEntity> events =
                outboxEventJpaRepository.findAll()
                        .stream()
                        .filter(event -> refundId.equals(event.getAggregateId()))
                        .filter(event -> "refund.requested".equals(event.getEventType()))
                        .toList();

        assertThat(events).hasSize(1);

        OutboxEventJpaEntity event = events.getFirst();

        assertThat(event.getAggregateType()).isEqualTo("REFUND");
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getHeaders()).contains(requestId);
    }

    private long refundOutboxCount(UUID refundId) {
        return outboxEventJpaRepository.findAll()
                .stream()
                .filter(event -> refundId.equals(event.getAggregateId()))
                .filter(event -> "refund.requested".equals(event.getEventType()))
                .count();
    }
}