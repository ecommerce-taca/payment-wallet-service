package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.infrastructure.persistence.entity.*;
import com.taca.paymentwallet.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SellerFinanceHttpE2ETest {

    private static final UUID USER_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000001");

    private static final UUID SHOP_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000002");

    private static final UUID OTHER_SHOP_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000003");

    private static final UUID WALLET_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000004");

    private static final UUID OTHER_WALLET_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000005");

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4.0")
                    .withDatabaseName("payment_wallet_seller_finance_e2e")
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
    private MockMvc mockMvc;

    @Autowired
    private WalletJpaRepository walletRepository;

    @Autowired
    private LedgerAccountJpaRepository ledgerAccountRepository;

    @Autowired
    private LedgerPostingJpaRepository ledgerPostingRepository;

    @Autowired
    private LedgerEntryJpaRepository ledgerEntryRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private PaymentAllocationJpaRepository paymentAllocationRepository;

    @Autowired
    private FeeConfigJpaRepository feeConfigRepository;

    @Autowired
    private TaxConfigJpaRepository taxConfigRepository;

    @Autowired
    private RefundJpaRepository refundRepository;

    @Autowired
    private RefundAllocationJpaRepository refundAllocationRepository;

    @BeforeEach
    void cleanDatabase() {
        refundAllocationRepository.deleteAll();
        refundRepository.deleteAll();

        ledgerEntryRepository.deleteAll();
        ledgerPostingRepository.deleteAll();

        paymentAllocationRepository.deleteAll();
        ledgerAccountRepository.deleteAll();

        paymentRepository.deleteAll();
        walletRepository.deleteAll();

        feeConfigRepository.deleteAll();
        taxConfigRepository.deleteAll();
    }

    @Test
    void shouldReturnSellerWalletThroughHttp() throws Exception {
        seedWallet();

        mockMvc.perform(sellerGet("/api/v1/seller/wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wallet_id").value(WALLET_ID.toString()))
                .andExpect(jsonPath("$.data.shop_id").value(SHOP_ID.toString()))
                .andExpect(jsonPath("$.data.available_balance").value(12_500_000))
                .andExpect(jsonPath("$.data.pending_balance").value(3_200_000))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.as_of").value("2026-08-31T04:00:00Z"))
                .andExpect(jsonPath("$.meta.request_id").value("req-seller-finance-e2e"));
    }

    @Test
    void shouldRejectAnonymousSeller() throws Exception {
        mockMvc.perform(
                        get("/api/v1/seller/wallet")
                                .header("X-Request-ID", "req-anonymous")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_UNAUTHENTICATED"));
    }

    @Test
    void shouldRejectNonSellerActor() throws Exception {
        mockMvc.perform(
                        get("/api/v1/seller/wallet")
                                .header("X-User-ID", USER_ID.toString())
                                .header("X-User-Roles", "BUYER")
                                .header("X-User-Shop-Scope", SHOP_ID.toString())
                                .header("X-Request-ID", "req-buyer")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_FORBIDDEN"));
    }

    @Test
    void shouldRejectSellerWithMultipleShopScope() throws Exception {
        mockMvc.perform(
                        get("/api/v1/seller/wallet")
                                .header("X-User-ID", USER_ID.toString())
                                .header("X-User-Roles", "SELLER")
                                .header(
                                        "X-User-Shop-Scope",
                                        SHOP_ID + "," + OTHER_SHOP_ID
                                )
                                .header("X-Request-ID", "req-multi-shop")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_FORBIDDEN"));
    }

    @Test
    void shouldReturnFilteredSellerLedgerThroughHttp() throws Exception {
        LedgerAccountJpaEntity account =
                seedSellerPendingAccount(SHOP_ID);

        UUID referenceId = UUID.randomUUID();

        LedgerPostingJpaEntity posting = seedPosting(
                referenceId,
                LocalDateTime.of(2026, 8, 15, 10, 0)
        );

        LedgerEntryJpaEntity entry = seedEntry(
                posting.getId(),
                account.getId(),
                "CREDIT",
                92_000,
                LocalDateTime.of(2026, 8, 15, 10, 0)
        );

        mockMvc.perform(
                        sellerGet("/api/v1/seller/wallet/ledger")
                                .param("page", "1")
                                .param("size", "20")
                                .param("from", "2026-08-01T00:00:00Z")
                                .param("to", "2026-09-01T00:00:00Z")
                                .param("type", "CREDIT")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].entry_id")
                        .value(entry.getId().toString()))
                .andExpect(jsonPath("$.data[0].posting_id")
                        .value(posting.getId().toString()))
                .andExpect(jsonPath("$.data[0].entry_type").value("CREDIT"))
                .andExpect(jsonPath("$.data[0].amount").value(92_000))
                .andExpect(jsonPath("$.data[0].balance_after").value(92_000))
                .andExpect(jsonPath("$.data[0].reference.type").value("PAYMENT"))
                .andExpect(jsonPath("$.data[0].reference.id")
                        .value(referenceId.toString()))
                .andExpect(jsonPath("$.data[0].description").value("Seller revenue"))
                .andExpect(jsonPath("$.meta.request_id")
                        .value("req-seller-finance-e2e"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(20))
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.meta.total_pages").value(1));
    }

    @Test
    void shouldNotExposeOtherShopLedgerEntries() throws Exception {
        LedgerAccountJpaEntity ownAccount =
                seedSellerPendingAccount(SHOP_ID);

        LedgerAccountJpaEntity otherAccount =
                seedSellerPendingAccount(OTHER_SHOP_ID);

        LedgerPostingJpaEntity ownPosting = seedPosting(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 8, 10, 10, 0)
        );

        LedgerPostingJpaEntity otherPosting = seedPosting(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 8, 11, 10, 0)
        );

        LedgerEntryJpaEntity ownEntry = seedEntry(
                ownPosting.getId(),
                ownAccount.getId(),
                "CREDIT",
                50_000,
                LocalDateTime.of(2026, 8, 10, 10, 0)
        );

        seedEntry(
                otherPosting.getId(),
                otherAccount.getId(),
                "CREDIT",
                999_999,
                LocalDateTime.of(2026, 8, 11, 10, 0)
        );

        mockMvc.perform(
                        sellerGet("/api/v1/seller/wallet/ledger")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].entry_id")
                        .value(ownEntry.getId().toString()))
                .andExpect(jsonPath("$.data[0].amount").value(50_000));
    }

    @Test
    void shouldReturnSellerRevenueThroughHttp() throws Exception {
        seedRevenueData();

        mockMvc.perform(
                        sellerGet("/api/v1/seller/revenue")
                                .param("from", "2026-08-01")
                                .param("to", "2026-08-31")
                                .param("granularity", "DAY")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.range.from").value("2026-08-01"))
                .andExpect(jsonPath("$.data.range.to").value("2026-08-31"))
                .andExpect(jsonPath("$.data.range.granularity").value("DAY"))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.summary.gross").value(100_000))
                .andExpect(jsonPath("$.data.summary.commission").value(7_000))
                .andExpect(jsonPath("$.data.summary.tax").value(1_000))
                .andExpect(jsonPath("$.data.summary.net").value(92_000))
                .andExpect(jsonPath("$.data.summary.refunded").value(25_000))
                .andExpect(jsonPath("$.data.summary.order_count").value(1))
                .andExpect(jsonPath("$.data.buckets[0].period").value("2026-08-01"))
                .andExpect(jsonPath("$.data.buckets[0].gross").value(100_000))
                .andExpect(jsonPath("$.data.buckets[0].commission").value(7_000))
                .andExpect(jsonPath("$.data.buckets[0].tax").value(1_000))
                .andExpect(jsonPath("$.data.buckets[0].net").value(92_000))
                .andExpect(jsonPath("$.data.buckets[0].order_count").value(1))
                .andExpect(jsonPath("$.meta.request_id")
                        .value("req-seller-finance-e2e"));
    }

    @Test
    void shouldNotIncludeOtherShopRevenue() throws Exception {
        seedRevenueData();
        seedOtherShopRevenue();

        mockMvc.perform(
                        sellerGet("/api/v1/seller/revenue")
                                .param("from", "2026-08-01")
                                .param("to", "2026-08-31")
                                .param("granularity", "MONTH")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.gross").value(100_000))
                .andExpect(jsonPath("$.data.summary.commission").value(7_000))
                .andExpect(jsonPath("$.data.summary.tax").value(1_000))
                .andExpect(jsonPath("$.data.summary.net").value(92_000))
                .andExpect(jsonPath("$.data.summary.refunded").value(25_000))
                .andExpect(jsonPath("$.data.summary.order_count").value(1));
    }

    @Test
    void shouldReturnZeroRevenueForEmptyRange() throws Exception {
        mockMvc.perform(
                        sellerGet("/api/v1/seller/revenue")
                                .param("from", "2026-09-01")
                                .param("to", "2026-09-30")
                                .param("granularity", "DAY")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.gross").value(0))
                .andExpect(jsonPath("$.data.summary.commission").value(0))
                .andExpect(jsonPath("$.data.summary.tax").value(0))
                .andExpect(jsonPath("$.data.summary.net").value(0))
                .andExpect(jsonPath("$.data.summary.refunded").value(0))
                .andExpect(jsonPath("$.data.summary.order_count").value(0))
                .andExpect(jsonPath("$.data.buckets").isEmpty());
    }

    @Test
    void shouldReturnNotFoundWhenSellerWalletDoesNotExist() throws Exception {

        mockMvc.perform(
                        sellerGet("/api/v1/seller/wallet")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code")
                        .value("WALLET_NOT_FOUND"))
                .andExpect(jsonPath("$.meta.request_id")
                        .value("req-seller-finance-e2e"));
    }

    @Test
    void shouldRejectInvalidSellerLedgerQueryThroughHttp() throws Exception {

        mockMvc.perform(
                        sellerGet("/api/v1/seller/wallet/ledger")
                                .param("page", "0")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.meta.request_id")
                        .value("req-seller-finance-e2e"));
    }

    private MockHttpServletRequestBuilder sellerGet(String path) {
        return get(path)
                .header("X-User-ID", USER_ID.toString())
                .header("X-User-Roles", "SELLER")
                .header("X-User-Shop-Scope", SHOP_ID.toString())
                .header("X-Request-ID", "req-seller-finance-e2e");
    }

    private WalletJpaEntity seedWallet() {
        WalletJpaEntity wallet = new WalletJpaEntity();

        wallet.setId(WALLET_ID);
        wallet.setShopId(SHOP_ID);
        wallet.setCurrency("VND");
        wallet.setAvailableBalance(12_500_000L);
        wallet.setPendingBalance(3_200_000L);
        wallet.setStatus("ACTIVE");
        wallet.setCreatedAt(LocalDateTime.of(2026, 8, 1, 8, 0));
        wallet.setUpdatedAt(LocalDateTime.of(2026, 8, 31, 4, 0));

        return walletRepository.saveAndFlush(wallet);
    }

    private WalletJpaEntity seedOtherShopWallet() {
        WalletJpaEntity wallet = new WalletJpaEntity();

        wallet.setId(OTHER_WALLET_ID);
        wallet.setShopId(OTHER_SHOP_ID);
        wallet.setCurrency("VND");
        wallet.setAvailableBalance(0L);
        wallet.setPendingBalance(0L);
        wallet.setStatus("ACTIVE");
        wallet.setCreatedAt(LocalDateTime.of(2026, 8, 1, 8, 0));
        wallet.setUpdatedAt(LocalDateTime.of(2026, 8, 1, 8, 0));

        return walletRepository.saveAndFlush(wallet);
    }

    private LedgerAccountJpaEntity seedSellerPendingAccount(UUID shopId) {
        LedgerAccountJpaEntity account = new LedgerAccountJpaEntity();

        account.setId(UUID.randomUUID());
        account.setAccountCode(
                "SELLER_PENDING:" + shopId + ":" + UUID.randomUUID()
        );
        account.setAccountType("SELLER_PENDING");
        account.setOwnerType("SHOP");
        account.setOwnerId(shopId);
        account.setCurrency("VND");
        account.setStatus("ACTIVE");
        account.setCreatedAt(LocalDateTime.of(2026, 8, 1, 0, 0));

        return ledgerAccountRepository.saveAndFlush(account);
    }

    private LedgerPostingJpaEntity seedPosting(
            UUID referenceId,
            LocalDateTime createdAt
    ) {
        LedgerPostingJpaEntity posting = new LedgerPostingJpaEntity();

        posting.setId(UUID.randomUUID());
        posting.setPostingType("PAYMENT_CAPTURE");
        posting.setBusinessKey(
                "seller-finance-e2e-" + UUID.randomUUID()
        );
        posting.setReferenceType("PAYMENT");
        posting.setReferenceId(referenceId);
        posting.setDescription("Seller revenue");
        posting.setCreatedAt(createdAt);

        return ledgerPostingRepository.saveAndFlush(posting);
    }

    private LedgerEntryJpaEntity seedEntry(
            UUID postingId,
            UUID accountId,
            String entryType,
            long amount,
            LocalDateTime createdAt
    ) {
        LedgerEntryJpaEntity entry = new LedgerEntryJpaEntity();

        entry.setId(UUID.randomUUID());
        entry.setPostingId(postingId);
        entry.setAccountId(accountId);
        entry.setEntryType(entryType);
        entry.setAmount(amount);
        entry.setCreatedAt(createdAt);

        return ledgerEntryRepository.saveAndFlush(entry);
    }

    private void seedRevenueData() {
        WalletJpaEntity wallet = seedWallet();

        PaymentJpaEntity payment =
                paymentRepository.saveAndFlush(
                        newSucceededPayment(100_000L, 25_000L)
                );

        FeeConfigJpaEntity feeConfig =
                feeConfigRepository.saveAndFlush(newFeeConfig());

        TaxConfigJpaEntity taxConfig =
                taxConfigRepository.saveAndFlush(newTaxConfig());

        PaymentAllocationJpaEntity allocation =
                new PaymentAllocationJpaEntity();

        allocation.setId(UUID.randomUUID());
        allocation.setPaymentId(payment.getId());
        allocation.setOrderId(UUID.randomUUID());
        allocation.setShopId(SHOP_ID);
        allocation.setWalletId(wallet.getId());
        allocation.setGrossAmount(100_000L);
        allocation.setCommissionAmount(7_000L);
        allocation.setTaxAmount(1_000L);
        allocation.setSellerNetAmount(92_000L);
        allocation.setCurrency("VND");
        allocation.setFeeConfigId(feeConfig.getId());
        allocation.setTaxConfigId(taxConfig.getId());
        allocation.setCreatedAt(
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );

        paymentAllocationRepository.saveAndFlush(allocation);

        RefundJpaEntity refund = new RefundJpaEntity();

        refund.setId(UUID.randomUUID());
        refund.setPaymentId(payment.getId());
        refund.setAmount(25_000L);
        refund.setCurrency("VND");
        refund.setReason("Seller revenue E2E refund");
        refund.setStatus("SUCCESS");
        refund.setProvider("VNPAY");
        refund.setIdempotencyKey(
                "seller-revenue-refund-" + UUID.randomUUID()
        );
        refund.setCreatedAt(
                LocalDateTime.of(2026, 8, 5, 12, 0)
        );
        refund.setUpdatedAt(
                LocalDateTime.of(2026, 8, 5, 12, 0)
        );
        refund.setCompletedAt(
                LocalDateTime.of(2026, 8, 5, 12, 0)
        );

        refundRepository.saveAndFlush(refund);

        RefundAllocationJpaEntity refundAllocation =
                new RefundAllocationJpaEntity();

        refundAllocation.setId(UUID.randomUUID());
        refundAllocation.setRefundId(refund.getId());
        refundAllocation.setPaymentAllocationId(allocation.getId());
        refundAllocation.setGrossAmount(25_000L);
        refundAllocation.setCommissionReversal(1_750L);
        refundAllocation.setTaxReversal(250L);
        refundAllocation.setSellerReversal(23_000L);
        refundAllocation.setCreatedAt(
                LocalDateTime.of(2026, 8, 5, 12, 0)
        );

        refundAllocationRepository.saveAndFlush(refundAllocation);
    }

    private void seedOtherShopRevenue() {
        WalletJpaEntity wallet = seedOtherShopWallet();

        PaymentJpaEntity payment =
                paymentRepository.saveAndFlush(
                        newSucceededPayment(200_000L, 0L)
                );

        FeeConfigJpaEntity feeConfig =
                feeConfigRepository.saveAndFlush(newFeeConfig());

        TaxConfigJpaEntity taxConfig =
                taxConfigRepository.saveAndFlush(newTaxConfig());

        PaymentAllocationJpaEntity allocation =
                new PaymentAllocationJpaEntity();

        allocation.setId(UUID.randomUUID());
        allocation.setPaymentId(payment.getId());
        allocation.setOrderId(UUID.randomUUID());
        allocation.setShopId(OTHER_SHOP_ID);
        allocation.setWalletId(wallet.getId());
        allocation.setGrossAmount(200_000L);
        allocation.setCommissionAmount(14_000L);
        allocation.setTaxAmount(2_000L);
        allocation.setSellerNetAmount(184_000L);
        allocation.setCurrency("VND");
        allocation.setFeeConfigId(feeConfig.getId());
        allocation.setTaxConfigId(taxConfig.getId());
        allocation.setCreatedAt(
                LocalDateTime.of(2026, 8, 2, 10, 0)
        );

        paymentAllocationRepository.saveAndFlush(allocation);
    }

    private PaymentJpaEntity newSucceededPayment(
            long amount,
            long refundedAmount
    ) {
        PaymentJpaEntity payment = new PaymentJpaEntity();

        payment.setId(UUID.randomUUID());
        payment.setCheckoutGroupId(UUID.randomUUID());
        payment.setBuyerUserId(UUID.randomUUID());
        payment.setMethod("VNPAY");
        payment.setAmount(amount);
        payment.setCurrency("VND");
        payment.setStatus("SUCCESS");
        payment.setCapturedAmount(amount);
        payment.setRefundedAmount(refundedAmount);
        payment.setExpiresAt(
                LocalDateTime.of(2026, 8, 1, 10, 30)
        );
        payment.setPaidAt(
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        payment.setCreatedAt(
                LocalDateTime.of(2026, 8, 1, 9, 55)
        );
        payment.setUpdatedAt(
                LocalDateTime.of(2026, 8, 5, 12, 0)
        );

        return payment;
    }

    private FeeConfigJpaEntity newFeeConfig() {
        FeeConfigJpaEntity config = new FeeConfigJpaEntity();

        config.setId(UUID.randomUUID());
        config.setScope("PLATFORM");
        config.setCategoryId(null);
        config.setRateBps(700);
        config.setEffectiveFrom(
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        config.setNote("Seller finance E2E fee");
        config.setCreatedBy(UUID.randomUUID());
        config.setCreatedAt(
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );

        return config;
    }

    private TaxConfigJpaEntity newTaxConfig() {
        TaxConfigJpaEntity config = new TaxConfigJpaEntity();

        config.setId(UUID.randomUUID());
        config.setScope("PLATFORM");
        config.setCategoryId(null);
        config.setRateBps(100);
        config.setEffectiveFrom(
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        config.setNote("Seller finance E2E tax");
        config.setCreatedBy(UUID.randomUUID());
        config.setCreatedAt(
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );

        return config;
    }
}