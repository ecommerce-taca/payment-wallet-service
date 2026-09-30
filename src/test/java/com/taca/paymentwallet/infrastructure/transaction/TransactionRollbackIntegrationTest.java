package com.taca.paymentwallet.infrastructure.transaction;

import com.taca.paymentwallet.application.port.out.OutboxPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.domain.payment.PaymentSucceededEvent;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.infrastructure.persistence.adapter.OutboxPersistenceAdapter;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransactionRollbackIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4.0")
                    .withDatabaseName(
                            "payment_wallet_transaction_test"
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
    private OutboxEventJpaRepository outboxRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldCommitBusinessDataAndOutboxTogether() {
        UUID paymentId =
                UUID.randomUUID();

        UUID eventId =
                UUID.randomUUID();

        PaymentJpaEntity payment =
                newPayment(paymentId);

        PaymentSucceededEvent event =
                paymentSucceededEvent(
                        eventId,
                        paymentId
                );

        TransactionPort transactionPort =
                transactionPort();

        OutboxPort outboxPort =
                outboxPort();

        transactionPort.execute(
                () -> {
                    paymentRepository.save(
                            payment
                    );

                    outboxPort.save(
                            event
                    );

                    /*
                     * Force SQL xuống MySQL ngay trong
                     * transaction hiện tại.
                     */
                    paymentRepository.flush();
                    outboxRepository.flush();
                }
        );

        assertThat(
                paymentRepository.findById(
                        paymentId
                )
        ).isPresent();

        assertThat(
                outboxRepository.findById(
                        eventId
                )
        ).isPresent();
    }

    @Test
    void shouldRollbackBusinessDataAndOutboxWhenActionFails() {
        UUID paymentId =
                UUID.randomUUID();

        UUID eventId =
                UUID.randomUUID();

        PaymentJpaEntity payment =
                newPayment(paymentId);

        PaymentSucceededEvent event =
                paymentSucceededEvent(
                        eventId,
                        paymentId
                );

        TransactionPort transactionPort =
                transactionPort();

        OutboxPort outboxPort =
                outboxPort();

        assertThatThrownBy(
                () ->
                        transactionPort.execute(
                                () -> {
                                    paymentRepository.save(
                                            payment
                                    );

                                    outboxPort.save(
                                            event
                                    );

                                    /*
                                     * Quan trọng:
                                     * SQL đã thực sự chạy trước
                                     * khi exception xảy ra.
                                     */
                                    paymentRepository.flush();
                                    outboxRepository.flush();

                                    assertThat(
                                            paymentRepository
                                                    .findById(
                                                            paymentId
                                                    )
                                    ).isPresent();

                                    assertThat(
                                            outboxRepository
                                                    .findById(
                                                            eventId
                                                    )
                                    ).isPresent();

                                    throw new IllegalStateException(
                                            "simulated business failure"
                                    );
                                }
                        )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "simulated business failure"
                );

        /*
         * Transaction đã kết thúc.
         *
         * Cả business data và outbox phải rollback.
         */
        assertThat(
                paymentRepository.findById(
                        paymentId
                )
        ).isEmpty();

        assertThat(
                outboxRepository.findById(
                        eventId
                )
        ).isEmpty();
    }

    private TransactionPort transactionPort() {
        return new SpringTransactionAdapter(
                transactionManager
        );
    }

    private OutboxPort outboxPort() {
        return new OutboxPersistenceAdapter(
                outboxRepository,
                new ObjectMapper()
        );
    }

    private PaymentSucceededEvent paymentSucceededEvent(
            UUID eventId,
            UUID paymentId
    ) {
        return new PaymentSucceededEvent(
                eventId,
                Instant.parse(
                        "2026-09-25T09:00:00Z"
                ),
                new PaymentId(
                        paymentId
                ),
                Money.vnd(
                        100_000
                )
        );
    }

    private PaymentJpaEntity newPayment(
            UUID paymentId
    ) {
        LocalDateTime now =
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        9,
                        0
                );

        PaymentJpaEntity payment =
                new PaymentJpaEntity();

        payment.setId(
                paymentId
        );

        payment.setCheckoutGroupId(
                UUID.randomUUID()
        );

        payment.setBuyerUserId(
                UUID.randomUUID()
        );

        payment.setMethod(
                "VNPAY"
        );

        payment.setAmount(
                100_000L
        );

        payment.setCurrency(
                "VND"
        );

        payment.setStatus(
                "SUCCESS"
        );

        payment.setCapturedAmount(
                100_000L
        );

        payment.setRefundedAmount(
                0L
        );

        payment.setExpiresAt(
                now.plusMinutes(15)
        );

        payment.setPaidAt(
                now
        );

        payment.setCreatedAt(
                now
        );

        payment.setUpdatedAt(
                now
        );

        return payment;
    }
}