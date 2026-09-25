package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.WalletJpaEntity;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessimisticLockingIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4.0")
                    .withDatabaseName(
                            "payment_wallet_lock_test"
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
    private WalletJpaRepository walletRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ExecutorService executor =
            Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void secondTransactionShouldWaitForPaymentLock()
            throws Exception {

        UUID checkoutGroupId =
                UUID.randomUUID();

        UUID paymentId =
                createPayment(
                        checkoutGroupId
                );

        CountDownLatch firstLockAcquired =
                new CountDownLatch(1);

        CountDownLatch releaseFirstLock =
                new CountDownLatch(1);

        CountDownLatch secondTransactionStarted =
                new CountDownLatch(1);

        Future<?> firstTransaction =
                executor.submit(
                        () ->
                                transactionTemplate()
                                        .executeWithoutResult(
                                                status -> {
                                                    PaymentJpaEntity payment =
                                                            paymentRepository
                                                                    .findByCheckoutGroupIdForUpdate(
                                                                            checkoutGroupId
                                                                    )
                                                                    .orElseThrow();

                                                    assertThat(
                                                            payment.getId()
                                                    ).isEqualTo(
                                                            paymentId
                                                    );

                                                    firstLockAcquired
                                                            .countDown();

                                                    awaitLatch(
                                                            releaseFirstLock
                                                    );
                                                }
                                        )
                );

        assertThat(
                firstLockAcquired.await(
                        5,
                        TimeUnit.SECONDS
                )
        ).isTrue();

        Future<UUID> secondTransaction =
                executor.submit(
                        () ->
                                transactionTemplate()
                                        .execute(
                                                status -> {
                                                    secondTransactionStarted
                                                            .countDown();

                                                    return paymentRepository
                                                            .findByCheckoutGroupIdForUpdate(
                                                                    checkoutGroupId
                                                            )
                                                            .orElseThrow()
                                                            .getId();
                                                }
                                        )
                );

        assertThat(
                secondTransactionStarted.await(
                        5,
                        TimeUnit.SECONDS
                )
        ).isTrue();

        /*
         * Transaction 1 vẫn giữ PESSIMISTIC_WRITE lock.
         * Transaction 2 không được hoàn thành lúc này.
         */
        Thread.sleep(300);

        assertThat(
                secondTransaction.isDone()
        ).isFalse();

        releaseFirstLock.countDown();

        firstTransaction.get(
                5,
                TimeUnit.SECONDS
        );

        UUID lockedPaymentId =
                secondTransaction.get(
                        5,
                        TimeUnit.SECONDS
                );

        assertThat(
                lockedPaymentId
        ).isEqualTo(
                paymentId
        );
    }

    @Test
    void secondTransactionShouldWaitForWalletLock()
            throws Exception {

        UUID shopId =
                UUID.randomUUID();

        UUID walletId =
                createWallet(shopId);

        CountDownLatch firstLockAcquired =
                new CountDownLatch(1);

        CountDownLatch releaseFirstLock =
                new CountDownLatch(1);

        CountDownLatch secondTransactionStarted =
                new CountDownLatch(1);

        Future<?> firstTransaction =
                executor.submit(
                        () ->
                                transactionTemplate()
                                        .executeWithoutResult(
                                                status -> {
                                                    WalletJpaEntity wallet =
                                                            walletRepository
                                                                    .findByShopIdAndCurrencyForUpdate(
                                                                            shopId,
                                                                            "VND"
                                                                    )
                                                                    .orElseThrow();

                                                    assertThat(
                                                            wallet.getId()
                                                    ).isEqualTo(
                                                            walletId
                                                    );

                                                    firstLockAcquired
                                                            .countDown();

                                                    awaitLatch(
                                                            releaseFirstLock
                                                    );
                                                }
                                        )
                );

        assertThat(
                firstLockAcquired.await(
                        5,
                        TimeUnit.SECONDS
                )
        ).isTrue();

        Future<UUID> secondTransaction =
                executor.submit(
                        () ->
                                transactionTemplate()
                                        .execute(
                                                status -> {
                                                    secondTransactionStarted
                                                            .countDown();

                                                    return walletRepository
                                                            .findByShopIdAndCurrencyForUpdate(
                                                                    shopId,
                                                                    "VND"
                                                            )
                                                            .orElseThrow()
                                                            .getId();
                                                }
                                        )
                );

        assertThat(
                secondTransactionStarted.await(
                        5,
                        TimeUnit.SECONDS
                )
        ).isTrue();

        Thread.sleep(300);

        assertThat(
                secondTransaction.isDone()
        ).isFalse();

        releaseFirstLock.countDown();

        firstTransaction.get(
                5,
                TimeUnit.SECONDS
        );

        UUID lockedWalletId =
                secondTransaction.get(
                        5,
                        TimeUnit.SECONDS
                );

        assertThat(
                lockedWalletId
        ).isEqualTo(
                walletId
        );
    }

    private UUID createPayment(
            UUID checkoutGroupId
    ) {
        UUID paymentId =
                UUID.randomUUID();

        transactionTemplate()
                .executeWithoutResult(
                        status -> {
                            LocalDateTime now =
                                    LocalDateTime.of(
                                            2026,
                                            9,
                                            25,
                                            7,
                                            30
                                    );

                            PaymentJpaEntity payment =
                                    new PaymentJpaEntity();

                            payment.setId(
                                    paymentId
                            );

                            payment.setCheckoutGroupId(
                                    checkoutGroupId
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
                                    "PENDING"
                            );

                            payment.setCapturedAmount(
                                    0L
                            );

                            payment.setRefundedAmount(
                                    0L
                            );

                            payment.setExpiresAt(
                                    now.plusMinutes(15)
                            );

                            payment.setCreatedAt(now);
                            payment.setUpdatedAt(now);

                            paymentRepository
                                    .saveAndFlush(
                                            payment
                                    );
                        }
                );

        return paymentId;
    }

    private UUID createWallet(
            UUID shopId
    ) {
        UUID walletId =
                UUID.randomUUID();

        transactionTemplate()
                .executeWithoutResult(
                        status -> {
                            LocalDateTime now =
                                    LocalDateTime.of(
                                            2026,
                                            9,
                                            25,
                                            7,
                                            30
                                    );

                            WalletJpaEntity wallet =
                                    new WalletJpaEntity();

                            wallet.setId(
                                    walletId
                            );

                            wallet.setShopId(
                                    shopId
                            );

                            wallet.setCurrency(
                                    "VND"
                            );

                            wallet.setAvailableBalance(
                                    100_000L
                            );

                            wallet.setPendingBalance(
                                    50_000L
                            );

                            wallet.setStatus(
                                    "ACTIVE"
                            );

                            wallet.setCreatedAt(now);
                            wallet.setUpdatedAt(now);

                            walletRepository
                                    .saveAndFlush(
                                            wallet
                                    );
                        }
                );

        return walletId;
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(
                transactionManager
        );
    }

    private void awaitLatch(
            CountDownLatch latch
    ) {
        try {
            boolean released =
                    latch.await(
                            5,
                            TimeUnit.SECONDS
                    );

            if (!released) {
                throw new IllegalStateException(
                        "timed out waiting for test latch"
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread()
                    .interrupt();

            throw new IllegalStateException(
                    "test thread interrupted",
                    exception
            );
        }
    }
}