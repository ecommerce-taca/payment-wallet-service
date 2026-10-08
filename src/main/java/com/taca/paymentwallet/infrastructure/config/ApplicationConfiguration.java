package com.taca.paymentwallet.infrastructure.config;

import com.taca.paymentwallet.application.port.in.*;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.application.security.*;
import com.taca.paymentwallet.application.service.*;
import com.taca.paymentwallet.domain.finance.AllocationCalculator;
import com.taca.paymentwallet.domain.finance.RefundAllocationCalculator;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.infrastructure.messaging.kafka.OutboxCleanupProperties;
import com.taca.paymentwallet.infrastructure.messaging.kafka.OutboxPublisherProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    AuthenticationGuard authenticationGuard() {
        return new AuthenticationGuard();
    }

    @Bean
    AuthorizationPolicy authorizationPolicy(AuthenticationGuard authenticationGuard) {
        return new AuthorizationPolicy(authenticationGuard);
    }

    @Bean
    ShopScopePolicy shopScopePolicy(AuthenticationGuard authenticationGuard) {
        return new ShopScopePolicy(authenticationGuard);
    }

    @Bean
    MfaStepUpPolicy mfaStepUpPolicy() {
        return new MfaStepUpPolicy();
    }

    @Bean
    InternalCallerPolicy internalCallerPolicy(AuthenticationGuard authenticationGuard) {
        return new InternalCallerPolicy(authenticationGuard);
    }

    @Bean
    GetPaymentUseCase getPaymentUseCase(
            PaymentRepositoryPort paymentRepository
    ) {
        return new GetPaymentService(paymentRepository);
    }

    @Bean
    CreatePaymentUseCase createPaymentUseCase(
            PaymentRepositoryPort paymentRepository,
            PaymentAttemptRepositoryPort paymentAttemptRepository,
            IdempotencyPort idempotencyPort,
            RequestHashPort requestHashPort,
            PaymentUrlHashPort paymentUrlHashPort,
            IdGeneratorPort idGeneratorPort,
            ClockPort clockPort,
            VnpayGatewayPort vnpayGatewayPort,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            CreatePaymentResultPayloadPort resultPayloadPort
    ) {
        return new CreatePaymentService(
                paymentRepository,
                paymentAttemptRepository,
                idempotencyPort,
                requestHashPort,
                paymentUrlHashPort,
                idGeneratorPort,
                clockPort,
                vnpayGatewayPort,
                outboxPort,
                transactionPort,
                resultPayloadPort
        );
    }

    @Bean
    ProcessVnpayWebhookUseCase
    processVnpayWebhookUseCase(
            PaymentRepositoryPort paymentRepository,
            PaymentAttemptRepositoryPort paymentAttemptRepository,
            PaymentProviderEventPort paymentProviderEventPort,
            PaymentAllocationRepositoryPort allocationRepository,
            WalletRepositoryPort walletRepository,
            LedgerPostingRepositoryPort ledgerPostingRepository,
            LedgerAccountLookupPort ledgerAccountLookupPort,
            FeePolicyPort feePolicyPort,
            VnpayWebhookVerifierPort verifierPort,
            IdGeneratorPort idGeneratorPort,
            ClockPort clockPort,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            AllocationCalculator allocationCalculator,
            LedgerPostingFactory ledgerPostingFactory
    ) {
        return new ProcessVnpayWebhookService(
                paymentRepository,
                paymentAttemptRepository,
                paymentProviderEventPort,
                allocationRepository,
                walletRepository,
                ledgerPostingRepository,
                ledgerAccountLookupPort,
                feePolicyPort,
                verifierPort,
                idGeneratorPort,
                clockPort,
                outboxPort,
                transactionPort,
                allocationCalculator,
                ledgerPostingFactory
        );
    }

    @Bean
    ProcessCodPaymentUseCase processCodPaymentUseCase(
            PaymentRepositoryPort paymentRepository,
            PaymentAllocationRepositoryPort allocationRepository,
            WalletRepositoryPort walletRepository,
            LedgerPostingRepositoryPort ledgerPostingRepository,
            LedgerAccountLookupPort ledgerAccountLookupPort,
            FeePolicyPort feePolicyPort,
            IdGeneratorPort idGeneratorPort,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            AllocationCalculator allocationCalculator,
            LedgerPostingFactory ledgerPostingFactory
    ) {
        return new ProcessCodPaymentService(
                paymentRepository,
                allocationRepository,
                walletRepository,
                ledgerPostingRepository,
                ledgerAccountLookupPort,
                feePolicyPort,
                idGeneratorPort,
                outboxPort,
                transactionPort,
                allocationCalculator,
                ledgerPostingFactory
        );
    }

    @Bean
    RequestRefundUseCase requestRefundUseCase(
            PaymentRepositoryPort paymentRepository,
            RefundRepositoryPort refundRepository,
            IdempotencyPort idempotencyPort,
            RequestHashPort requestHashPort,
            IdGeneratorPort idGeneratorPort,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            RequestRefundResultPayloadPort resultPayloadPort
    ) {
        return new RequestRefundService(
                paymentRepository,
                refundRepository,
                idempotencyPort,
                requestHashPort,
                idGeneratorPort,
                outboxPort,
                transactionPort,
                resultPayloadPort
        );
    }

    @Bean
    ProcessRefundResultUseCase
    processRefundResultUseCase(
            RefundRepositoryPort refundRepository,
            PaymentRepositoryPort paymentRepository,
            PaymentAllocationRepositoryPort allocationRepository,
            RefundAllocationRepositoryPort refundAllocationRepository,
            LedgerPostingRepositoryPort ledgerPostingRepository,
            LedgerAccountLookupPort ledgerAccountLookupPort,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            IdGeneratorPort idGeneratorPort,
            RefundAllocationCalculator refundAllocationCalculator,
            LedgerPostingFactory ledgerPostingFactory
    ) {
        return new ProcessRefundResultService(
                refundRepository,
                paymentRepository,
                allocationRepository,
                refundAllocationRepository,
                ledgerPostingRepository,
                ledgerAccountLookupPort,
                outboxPort,
                transactionPort,
                idGeneratorPort,
                refundAllocationCalculator,
                ledgerPostingFactory
        );
    }

    @Bean
    RequestPayoutUseCase requestPayoutUseCase(
            WalletRepositoryPort walletRepository,
            PayoutRepositoryPort payoutRepository,
            IdempotencyPort idempotencyPort,
            RequestHashPort requestHashPort,
            IdGeneratorPort idGeneratorPort,
            LedgerAccountLookupPort ledgerAccountLookupPort,
            LedgerPostingRepositoryPort ledgerPostingRepository,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            RequestPayoutResultPayloadPort resultPayloadPort,
            LedgerPostingFactory ledgerPostingFactory
    ) {
        return new RequestPayoutService(
                walletRepository,
                payoutRepository,
                idempotencyPort,
                requestHashPort,
                idGeneratorPort,
                ledgerAccountLookupPort,
                ledgerPostingRepository,
                outboxPort,
                transactionPort,
                resultPayloadPort,
                ledgerPostingFactory
        );
    }

    @Bean
    ProcessPayoutResultUseCase
    processPayoutResultUseCase(
            PayoutRepositoryPort payoutRepository,
            WalletRepositoryPort walletRepository,
            LedgerPostingRepositoryPort ledgerPostingRepository,
            LedgerAccountLookupPort ledgerAccountLookupPort,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            IdGeneratorPort idGeneratorPort,
            LedgerPostingFactory ledgerPostingFactory
    ) {
        return new ProcessPayoutResultService(
                payoutRepository,
                walletRepository,
                ledgerPostingRepository,
                ledgerAccountLookupPort,
                outboxPort,
                transactionPort,
                idGeneratorPort,
                ledgerPostingFactory
        );
    }

    @Bean
    ProcessInboxEventUseCase processInboxEventUseCase(
            InboxEventPort inboxEventPort,
            ClockPort clockPort,
            TransactionPort transactionPort
    ) {
        return new ProcessInboxEventService(
                inboxEventPort,
                clockPort,
                transactionPort
        );
    }

    @Bean
    PublishOutboxUseCase publishOutboxUseCase(
            OutboxPublishingPort outboxPublishingPort,
            OutboxMessagePublisherPort publisherPort,
            TransactionPort transactionPort,
            ClockPort clockPort,
            OutboxPublisherProperties properties
    ) {
        return new PublishOutboxService(
                outboxPublishingPort,
                publisherPort,
                transactionPort,
                clockPort,
                properties.batchSize(),
                properties.maxRetries(),
                properties.retryBackoff()
        );
    }

    @Bean
    PublishOutboxDeadLetterUseCase publishOutboxDeadLetterUseCase(
            OutboxDeadLetterPort deadLetterPort,
            DeadLetterPublisherPort deadLetterPublisherPort,
            OutboxMessagePublisherPort messagePublisherPort,
            TransactionPort transactionPort,
            ClockPort clockPort,
            OutboxPublisherProperties properties
    ) {
        return new PublishOutboxDeadLetterService(
                deadLetterPort,
                deadLetterPublisherPort,
                messagePublisherPort,
                transactionPort,
                clockPort,
                properties.batchSize(),
                properties.maxRetries()
        );
    }

    @Bean
    CleanupOutboxUseCase cleanupOutboxUseCase(
            OutboxCleanupPort cleanupPort,
            ClockPort clockPort,
            TransactionPort transactionPort,
            OutboxCleanupProperties properties
    ) {
        return new CleanupOutboxService(
                cleanupPort,
                clockPort,
                transactionPort,
                properties.retention(),
                properties.batchSize()
        );
    }
}
