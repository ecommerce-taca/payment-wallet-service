package com.taca.paymentwallet.infrastructure.config;

import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.domain.finance.AllocationCalculator;
import com.taca.paymentwallet.domain.finance.RefundAllocationCalculator;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.infrastructure.crypto.Sha256PaymentUrlHashAdapter;
import com.taca.paymentwallet.infrastructure.crypto.Sha256RequestHashAdapter;
import com.taca.paymentwallet.infrastructure.id.UuidV7IdGeneratorAdapter;
import com.taca.paymentwallet.infrastructure.messaging.kafka.*;
import com.taca.paymentwallet.infrastructure.messaging.kafka.shipment.ShipmentEventParser;
import com.taca.paymentwallet.infrastructure.persistence.adapter.*;
import com.taca.paymentwallet.infrastructure.persistence.mapper.*;
import com.taca.paymentwallet.infrastructure.persistence.repository.*;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import com.taca.paymentwallet.infrastructure.serialization.JacksonCreatePaymentResultPayloadAdapter;
import com.taca.paymentwallet.infrastructure.serialization.JacksonRequestPayoutResultPayloadAdapter;
import com.taca.paymentwallet.infrastructure.serialization.JacksonRequestRefundResultPayloadAdapter;
import com.taca.paymentwallet.infrastructure.time.SystemClockAdapter;
import com.taca.paymentwallet.infrastructure.transaction.SpringTransactionAdapter;
import com.taca.paymentwallet.infrastructure.vnpay.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({
        VnpayProperties.class,
        KafkaTopicProperties.class,
        OutboxPublisherProperties.class
})
public class InfrastructureConfiguration {

    @Bean
    ClockPort clockPort() {
        return new SystemClockAdapter();
    }

    @Bean
    PersistenceUuidGenerator persistenceUuidGenerator() {
        return new PersistenceUuidGenerator();
    }

    @Bean
    IdGeneratorPort idGeneratorPort(
            ClockPort clockPort,
            PersistenceUuidGenerator uuidGenerator
    ) {
        return new UuidV7IdGeneratorAdapter(
                clockPort,
                uuidGenerator
        );
    }

    @Bean
    RequestHashPort requestHashPort(
            ObjectMapper objectMapper
    ) {
        return new Sha256RequestHashAdapter(
                objectMapper
        );
    }

    @Bean
    CreatePaymentResultPayloadPort
    createPaymentResultPayloadPort(
            ObjectMapper objectMapper
    ) {
        return new JacksonCreatePaymentResultPayloadAdapter(
                objectMapper
        );
    }

    @Bean
    RequestRefundResultPayloadPort
    requestRefundResultPayloadPort(
            ObjectMapper objectMapper
    ) {
        return new JacksonRequestRefundResultPayloadAdapter(
                objectMapper
        );
    }

    @Bean
    RequestPayoutResultPayloadPort
    requestPayoutResultPayloadPort(
            ObjectMapper objectMapper
    ) {
        return new JacksonRequestPayoutResultPayloadAdapter(
                objectMapper
        );
    }

    @Bean
    PaymentUrlHashPort paymentUrlHashPort() {
        return new Sha256PaymentUrlHashAdapter();
    }

    @Bean
    TransactionPort transactionPort(
            PlatformTransactionManager transactionManager
    ) {
        return new SpringTransactionAdapter(
                transactionManager
        );
    }

    @Bean
    VnpaySigner vnpaySigner() {
        return new VnpaySigner();
    }

    @Bean
    VnpayGatewayPort vnpayGatewayPort(
            VnpayProperties properties,
            VnpaySigner signer,
            ClockPort clockPort
    ) {
        return new VnpayGatewayAdapter(
                properties,
                signer,
                clockPort
        );
    }

    @Bean
    VnpayWebhookVerifierPort vnpayWebhookVerifierPort(
            VnpayProperties properties,
            VnpaySigner signer
    ) {
        return new VnpayWebhookVerifierAdapter(
                properties,
                signer
        );
    }

    @Bean
    AllocationCalculator allocationCalculator() {
        return new AllocationCalculator();
    }

    @Bean
    RefundAllocationCalculator refundAllocationCalculator() {
        return new RefundAllocationCalculator();
    }

    @Bean
    LedgerPostingFactory ledgerPostingFactory() {
        return new LedgerPostingFactory();
    }

    @Bean
    PaymentPersistenceMapper paymentPersistenceMapper() {
        return new PaymentPersistenceMapper();
    }

    @Bean
    PaymentAttemptPersistenceMapper paymentAttemptPersistenceMapper() {
        return new PaymentAttemptPersistenceMapper();
    }

    @Bean
    PaymentAllocationPersistenceMapper
    paymentAllocationPersistenceMapper() {
        return new PaymentAllocationPersistenceMapper();
    }

    @Bean
    WalletPersistenceMapper walletPersistenceMapper() {
        return new WalletPersistenceMapper();
    }

    @Bean
    LedgerPostingPersistenceMapper
    ledgerPostingPersistenceMapper() {
        return new LedgerPostingPersistenceMapper();
    }

    @Bean
    RefundPersistenceMapper refundPersistenceMapper() {
        return new RefundPersistenceMapper();
    }

    @Bean
    RefundAllocationPersistenceMapper
    refundAllocationPersistenceMapper() {
        return new RefundAllocationPersistenceMapper();
    }

    @Bean
    BankAccountSnapshotPersistenceCodec
    bankAccountSnapshotPersistenceCodec() {
        return new BankAccountSnapshotPersistenceCodec();
    }

    @Bean
    PayoutPersistenceMapper payoutPersistenceMapper(
            BankAccountSnapshotPersistenceCodec codec
    ) {
        return new PayoutPersistenceMapper(codec);
    }

    @Bean
    SettlementPersistenceMapper
    settlementPersistenceMapper() {
        return new SettlementPersistenceMapper();
    }

    @Bean
    PaymentRepositoryPort paymentRepositoryPort(
            PaymentJpaRepository paymentRepository,
            PaymentOrderJpaRepository orderRepository,
            PaymentPersistenceMapper mapper,
            ClockPort clockPort,
            PersistenceUuidGenerator uuidGenerator
    ) {
        return new PaymentRepositoryAdapter(
                paymentRepository,
                orderRepository,
                mapper,
                clockPort,
                uuidGenerator
        );
    }

    @Bean
    PaymentAttemptRepositoryPort
    paymentAttemptRepositoryPort(
            PaymentAttemptJpaRepository repository,
            PaymentAttemptPersistenceMapper mapper,
            ClockPort clockPort
    ) {
        return new PaymentAttemptRepositoryAdapter(
                repository,
                mapper,
                clockPort
        );
    }

    @Bean
    PaymentProviderEventPort paymentProviderEventPort(
            PaymentEventJpaRepository repository,
            ClockPort clockPort,
            PersistenceUuidGenerator uuidGenerator
    ) {
        return new PaymentProviderEventAdapter(
                repository,
                clockPort,
                uuidGenerator
        );
    }

    @Bean
    PaymentAllocationRepositoryPort
    paymentAllocationRepositoryPort(
            PaymentAllocationJpaRepository repository,
            PaymentAllocationPersistenceMapper mapper,
            ClockPort clockPort
    ) {
        return new PaymentAllocationRepositoryAdapter(
                repository,
                mapper,
                clockPort
        );
    }

    @Bean
    WalletRepositoryPort walletRepositoryPort(
            WalletJpaRepository repository,
            WalletPersistenceMapper mapper,
            ClockPort clockPort
    ) {
        return new WalletRepositoryAdapter(
                repository,
                mapper,
                clockPort
        );
    }

    @Bean
    LedgerPostingRepositoryPort
    ledgerPostingRepositoryPort(
            LedgerPostingJpaRepository postingRepository,
            LedgerEntryJpaRepository entryRepository,
            LedgerPostingPersistenceMapper mapper,
            ClockPort clockPort,
            PersistenceUuidGenerator uuidGenerator
    ) {
        return new LedgerPostingRepositoryAdapter(
                postingRepository,
                entryRepository,
                mapper,
                clockPort,
                uuidGenerator
        );
    }

    @Bean
    LedgerAccountLookupPort ledgerAccountLookupPort(
            LedgerAccountJpaRepository ledgerAccountRepository,
            PaymentAllocationJpaRepository allocationRepository,
            SettlementLineJpaRepository settlementLineRepository
    ) {
        return new LedgerAccountLookupAdapter(
                ledgerAccountRepository,
                allocationRepository,
                settlementLineRepository
        );
    }

    @Bean
    FeePolicyPort feePolicyPort(
            FeeConfigJpaRepository feeRepository,
            TaxConfigJpaRepository taxRepository,
            ClockPort clockPort
    ) {
        return new FeePolicyPersistenceAdapter(
                feeRepository,
                taxRepository,
                clockPort
        );
    }

    @Bean
    IdempotencyPort idempotencyPort(
            IdempotencyKeyJpaRepository repository,
            ClockPort clockPort,
            PersistenceUuidGenerator uuidGenerator
    ) {
        return new IdempotencyPersistenceAdapter(
                repository,
                clockPort,
                uuidGenerator,
                Duration.ofHours(24)
        );
    }

    @Bean
    OutboxPort outboxPort(
            OutboxEventJpaRepository repository,
            ObjectMapper objectMapper
    ) {
        return new OutboxPersistenceAdapter(
                repository,
                objectMapper
        );
    }

    @Bean
    OutboxPublishingPort outboxPublishingPort(OutboxEventJpaRepository repository) {
        return new OutboxPublishingPersistenceAdapter(repository);
    }

    @Bean
    KafkaTopicRouter kafkaTopicRouter(KafkaTopicProperties properties) {
        return new KafkaTopicRouter(properties);
    }
    
    @Bean
    KafkaDeadLetterHeaderMapper kafkaDeadLetterHeaderMapper(
            ObjectMapper objectMapper
    ) {
        return new KafkaDeadLetterHeaderMapper(objectMapper);
    }

    @Bean
    OutboxMessagePublisherPort outboxMessagePublisherPort(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaTopicRouter topicRouter,
            KafkaHeaderMapper headerMapper,
            OutboxPublisherObservation observation
    ) {
        return new KafkaOutboxMessagePublisherAdapter(
                kafkaTemplate,
                topicRouter,
                headerMapper,
                observation
        );
    }

    @Bean
    KafkaHeaderMapper kafkaHeaderMapper(
            ObjectMapper objectMapper
    ) {
        return new KafkaHeaderMapper(objectMapper);
    }

    @Bean
    InboxEventPort inboxEventPort(
            InboxEventJpaRepository repository,
            PersistenceUuidGenerator uuidGenerator
    ) {
        return new InboxEventAdapter(
                repository,
                uuidGenerator
        );
    }

    @Bean
    RefundRepositoryPort refundRepositoryPort(
            RefundJpaRepository repository,
            RefundPersistenceMapper mapper,
            ClockPort clockPort
    ) {
        return new RefundRepositoryAdapter(
                repository,
                mapper,
                clockPort
        );
    }

    @Bean
    RefundAllocationRepositoryPort
    refundAllocationRepositoryPort(
            RefundAllocationJpaRepository repository,
            RefundAllocationPersistenceMapper mapper,
            ClockPort clockPort,
            PersistenceUuidGenerator uuidGenerator
    ) {
        return new RefundAllocationRepositoryAdapter(
                repository,
                mapper,
                clockPort,
                uuidGenerator
        );
    }

    @Bean
    PayoutRepositoryPort payoutRepositoryPort(
            PayoutJpaRepository repository,
            PayoutPersistenceMapper mapper,
            ClockPort clockPort
    ) {
        return new PayoutRepositoryAdapter(
                repository,
                mapper,
                clockPort
        );
    }

    @Bean
    SettlementRepositoryPort settlementRepositoryPort(
            SettlementBatchJpaRepository batchRepository,
            SettlementBatchItemJpaRepository itemRepository,
            SettlementLineJpaRepository lineRepository,
            SettlementPersistenceMapper mapper,
            ClockPort clockPort
    ) {
        return new SettlementRepositoryAdapter(
                batchRepository,
                itemRepository,
                lineRepository,
                mapper,
                clockPort
        );
    }

    @Bean
    OutboxDeadLetterPort outboxDeadLetterPort(
            OutboxEventJpaRepository repository
    ) {
        return new OutboxDeadLetterPersistenceAdapter(repository);
    }

    @Bean
    DeadLetterPublisherPort deadLetterPublisherPort(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaTopicProperties properties,
            ObjectMapper objectMapper,
            KafkaDeadLetterHeaderMapper headerMapper,
            OutboxPublisherObservation observation
    ) {
        return new KafkaDeadLetterPublisherAdapter(
                kafkaTemplate,
                properties,
                objectMapper,
                headerMapper,
                observation
        );
    }

    @Bean
    ShipmentEventParser shipmentEventParser(
            ObjectMapper objectMapper
    ) {
        return new ShipmentEventParser(objectMapper);
    }

    @Bean
    OutboxPublisherObservation outboxPublisherObservation(
            MeterRegistry meterRegistry
    ) {
        return new OutboxPublisherObservation(
                meterRegistry
        );
    }
}