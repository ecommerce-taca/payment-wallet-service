package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.exception.FeePolicyNotConfiguredException;
import com.taca.paymentwallet.application.fee.PaymentFeePolicy;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.infrastructure.persistence.entity.FeeConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.TaxConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.FeeConfigJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.TaxConfigJpaRepository;
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
class FeePolicyPersistenceIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4.0")
                    .withDatabaseName(
                            "payment_wallet_fee_policy_test"
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
    private FeeConfigJpaRepository feeRepository;

    @Autowired
    private TaxConfigJpaRepository taxRepository;

    @Test
    void shouldSelectLatestEffectiveFeeAndTaxPolicy() {
        Instant now =
                Instant.parse(
                        "2026-09-25T05:00:00Z"
                );

        FeeConfigJpaEntity olderFee =
                feeConfig(
                        500,
                        LocalDateTime.of(
                                2026,
                                8,
                                1,
                                0,
                                0
                        )
                );

        FeeConfigJpaEntity currentFee =
                feeConfig(
                        700,
                        LocalDateTime.of(
                                2026,
                                9,
                                20,
                                0,
                                0
                        )
                );

        FeeConfigJpaEntity futureFee =
                feeConfig(
                        900,
                        LocalDateTime.of(
                                2026,
                                10,
                                1,
                                0,
                                0
                        )
                );

        TaxConfigJpaEntity olderTax =
                taxConfig(
                        100,
                        LocalDateTime.of(
                                2026,
                                8,
                                1,
                                0,
                                0
                        )
                );

        TaxConfigJpaEntity currentTax =
                taxConfig(
                        300,
                        LocalDateTime.of(
                                2026,
                                9,
                                20,
                                0,
                                0
                        )
                );

        TaxConfigJpaEntity futureTax =
                taxConfig(
                        500,
                        LocalDateTime.of(
                                2026,
                                10,
                                1,
                                0,
                                0
                        )
                );

        feeRepository.saveAndFlush(
                olderFee
        );

        feeRepository.saveAndFlush(
                currentFee
        );

        feeRepository.saveAndFlush(
                futureFee
        );

        taxRepository.saveAndFlush(
                olderTax
        );

        taxRepository.saveAndFlush(
                currentTax
        );

        taxRepository.saveAndFlush(
                futureTax
        );

        ClockPort clockPort =
                () -> now;

        FeePolicyPersistenceAdapter adapter =
                new FeePolicyPersistenceAdapter(
                        feeRepository,
                        taxRepository,
                        clockPort
                );

        PaymentFeePolicy policy =
                adapter.currentPaymentFeePolicy();

        assertThat(
                policy.feeConfigId().value()
        ).isEqualTo(
                currentFee.getId()
        );

        assertThat(
                policy.taxConfigId().value()
        ).isEqualTo(
                currentTax.getId()
        );

        assertThat(
                policy.commissionRate().value()
        ).isEqualTo(700);

        assertThat(
                policy.taxRate().value()
        ).isEqualTo(300);
    }

    @Test
    void shouldIgnoreFutureFeeAndTaxConfig() {
        Instant now =
                Instant.parse(
                        "2026-09-25T05:00:00Z"
                );

        FeeConfigJpaEntity currentFee =
                feeConfig(
                        700,
                        LocalDateTime.of(
                                2026,
                                9,
                                1,
                                0,
                                0
                        )
                );

        FeeConfigJpaEntity futureFee =
                feeConfig(
                        999,
                        LocalDateTime.of(
                                2026,
                                9,
                                26,
                                0,
                                0
                        )
                );

        TaxConfigJpaEntity currentTax =
                taxConfig(
                        300,
                        LocalDateTime.of(
                                2026,
                                9,
                                1,
                                0,
                                0
                        )
                );

        TaxConfigJpaEntity futureTax =
                taxConfig(
                        999,
                        LocalDateTime.of(
                                2026,
                                9,
                                26,
                                0,
                                0
                        )
                );

        feeRepository.saveAndFlush(
                currentFee
        );

        feeRepository.saveAndFlush(
                futureFee
        );

        taxRepository.saveAndFlush(
                currentTax
        );

        taxRepository.saveAndFlush(
                futureTax
        );

        FeePolicyPersistenceAdapter adapter =
                new FeePolicyPersistenceAdapter(
                        feeRepository,
                        taxRepository,
                        () -> now
                );

        PaymentFeePolicy policy =
                adapter.currentPaymentFeePolicy();

        assertThat(
                policy.feeConfigId().value()
        ).isNotEqualTo(
                futureFee.getId()
        );

        assertThat(
                policy.taxConfigId().value()
        ).isNotEqualTo(
                futureTax.getId()
        );

        assertThat(
                policy.commissionRate().value()
        ).isNotEqualTo(999);

        assertThat(
                policy.taxRate().value()
        ).isNotEqualTo(999);
    }

    @Test
    void shouldRejectWhenNoPolicyIsEffectiveYet() {
        /*
         * V014 seed có PLATFORM config effective từ
         * 2026-01-01.
         *
         * Dùng thời điểm 2025 để chứng minh query
         * không lấy một config chưa effective.
         */
        Instant beforeEverySeededPolicy =
                Instant.parse(
                        "2025-01-01T00:00:00Z"
                );

        FeePolicyPersistenceAdapter adapter =
                new FeePolicyPersistenceAdapter(
                        feeRepository,
                        taxRepository,
                        () -> beforeEverySeededPolicy
                );

        assertThatThrownBy(
                adapter::currentPaymentFeePolicy
        ).isInstanceOf(
                FeePolicyNotConfiguredException.class
        );
    }

    private FeeConfigJpaEntity feeConfig(
            int rateBps,
            LocalDateTime effectiveFrom
    ) {
        FeeConfigJpaEntity entity =
                new FeeConfigJpaEntity();

        entity.setId(
                UUID.randomUUID()
        );

        entity.setScope(
                "PLATFORM"
        );

        entity.setCategoryId(null);

        entity.setRateBps(
                rateBps
        );

        entity.setEffectiveFrom(
                effectiveFrom
        );

        entity.setNote(
                "Phase 1E fee policy integration test"
        );

        entity.setCreatedBy(
                UUID.randomUUID()
        );

        entity.setCreatedAt(
                effectiveFrom
        );

        return entity;
    }

    private TaxConfigJpaEntity taxConfig(
            int rateBps,
            LocalDateTime effectiveFrom
    ) {
        TaxConfigJpaEntity entity =
                new TaxConfigJpaEntity();

        entity.setId(
                UUID.randomUUID()
        );

        entity.setScope(
                "PLATFORM"
        );

        entity.setCategoryId(null);

        entity.setRateBps(
                rateBps
        );

        entity.setEffectiveFrom(
                effectiveFrom
        );

        entity.setNote(
                "Phase 1E tax policy integration test"
        );

        entity.setCreatedBy(
                UUID.randomUUID()
        );

        entity.setCreatedAt(
                effectiveFrom
        );

        return entity;
    }
}