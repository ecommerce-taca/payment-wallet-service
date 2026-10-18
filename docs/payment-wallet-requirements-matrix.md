# Payment-Wallet Requirements Traceability

Status:

- DONE — implemented and tested.
- PARTIAL — foundation exists but requirement is incomplete.
- TODO — not implemented.
- BLOCKED — authoritative external contract is missing.

| Area | Requirement | Status | Implementation / Gap |
|---|---|---:|---|
| Architecture | DDD + Hexagonal boundaries | DONE | `ArchitectureTest` |
| Payment | VNPAY create intent | DONE | Create Payment flow |
| Payment | COD PENDING_COD | DONE | Payment domain |
| Payment | Get payment detail API | DONE | Payment detail query/use case/controller + HTTP coverage |
| Payment | Order authoritative snapshot | TODO | `OrderSnapshotPort` missing; ADR-002 records this as implementation debt |
| Payment | Shipping fee separated from seller gross | DONE | `PaymentOrder.merchandiseAmount`, `shippingFee`; `AllocationCalculator` uses merchandise only |
| VNPAY | Payment URL creation | DONE | VNPAY adapter |
| VNPAY | Verified webhook + dedupe | DONE | Webhook service |
| VNPAY | Provider IP policy | TODO | Authoritative allowlist policy missing |
| Wallet | pending/available projection | DONE | Wallet domain/persistence |
| Wallet | seller wallet read projection | DONE | `SellerWalletReadPort`, `SellerWalletReadAdapter` |
| Ledger | double-entry posting | DONE | `LedgerPosting` |
| Ledger | shipment payable | DONE | `SHIPMENT_PAYABLE`; capture posting credits shipping separately |
| Ledger | seller ledger read model | DONE | `SellerLedgerReadPort`, `SellerLedgerReadAdapter` |
| Refund | request/application workflow | DONE | Refund request API, application workflow, persistence and tests |
| Payout | reserve workflow | PARTIAL | Domain/application/persistence foundation exists; production seller payout API intentionally gated |
| Payout | KYC/status projection | BLOCKED | Authoritative shop payout-gate projection remains OPEN in ADR-003 |
| Payout | bank-account source | BLOCKED | Authoritative source/ownership of `bank_account_id` remains OPEN in ADR-003 |
| Settlement | domain/persistence | DONE | Settlement aggregate, repository, lines and unsettled candidate persistence foundation |
| Settlement | eligibility policy | BLOCKED | Refund/return window and authoritative eligible timestamp remain OPEN in ADR-003 |
| Settlement | production execution | BLOCKED | `SettlementCandidatePort` and `RunSettlementUseCase` intentionally not wired until eligibility is resolved |
| Kafka | outbox persistence | DONE | Outbox persistence and application foundation |
| Kafka | publisher/retry/DLQ | DONE | Outbox publisher, retry and dead-letter flow |
| Kafka | consumers | DONE | Kafka consumer foundation implemented for asynchronous financial workflows |
| Events | payment.created | DONE | Domain/outbox |
| Events | wallet.allocated | DONE | Domain/outbox |
| Events | payment notification recipient | BLOCKED | Authoritative buyer email source remains OPEN in ADR-003 |
| Events | payout notification recipient | BLOCKED | Authoritative seller owner recipient remains OPEN in ADR-003 |
| API | Payment/refund endpoints | DONE | Create payment, payment detail, webhook and refund request APIs implemented and tested |
| API | Seller wallet | DONE | `GET /seller/wallet` |
| API | Seller ledger | DONE | `GET /seller/wallet/ledger` |
| API | Seller revenue | DONE | `GET /seller/revenue` |
| API | Seller revenue export | TODO | Target contract exists; runtime endpoint not implemented |
| API | Seller payout request | BLOCKED | ADR-003 payout-gate and bank-account source remain OPEN |
| API | Seller payout history | TODO | Endpoint listed in target API inventory; runtime endpoint not implemented |
| API | Admin finance endpoints | TODO | Phase 8 |
| Security | Actor context | DONE | Actor context/filter/guard foundation |
| Security | Seller single-shop scope | DONE | Seller access requires exactly one shop in actor `shopScope` |
| Security | FINANCE_OPS scope | DONE | Finance authorization foundation from Phase 5 |
| Security | Step-up 2FA | PARTIAL | MFA boundary exists; seller payout production exposure remains gated |
| Seller Finance | wallet HTTP API | DONE | Wallet balance/status/`as_of` response |
| Seller Finance | ledger pagination + filters | DONE | `page`, `size`, `from`, `to`, `type` |
| Seller Finance | revenue aggregation | DONE | DAY/WEEK/MONTH aggregation |
| Seller Finance | refund aggregation | DONE | Refund allocations included in seller revenue summary |
| Seller Finance | shop data isolation | DONE | Wallet/ledger/revenue derive shop from authenticated seller scope |
| Seller Finance | HTTP integration coverage | DONE | `SellerFinanceHttpE2ETest` with MySQL Testcontainers |
| Seller Finance | error contract | DONE | Seller finance 400/401/403/404 behavior aligned and tested |
| Observability | Request/trace propagation | TODO | Phase 9 scope |
| Health | `/health/live` | DONE | Runtime liveness endpoint |
| Health | `/health/ready` | DONE | Runtime readiness foundation |
| Operations | Dockerfile/Kafka local stack | DONE | Local runtime composition available |
| Operations | OpenAPI/runbook | TODO | Phase 9/10 scope |
