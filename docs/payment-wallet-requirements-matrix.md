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
| API | Admin finance endpoints | DONE | Reconciliation, fee/tax configuration, settlement read/retry and finance summary/export implemented in Phase 8 |
| Security | Actor context | DONE | Actor context/filter/guard foundation |
| Security | Seller single-shop scope | DONE | Seller access requires exactly one shop in actor `shopScope` |
| Security | FINANCE_OPS scope | DONE | Finance authorization foundation from Phase 5 |
| Security | Step-up 2FA | PARTIAL | MFA boundary is implemented for protected runtime operations; seller payout production exposure remains gated |
| Seller Finance | wallet HTTP API | DONE | Wallet balance/status/`as_of` response |
| Seller Finance | ledger pagination + filters | DONE | `page`, `size`, `from`, `to`, `type` |
| Seller Finance | revenue aggregation | DONE | DAY/WEEK/MONTH aggregation |
| Seller Finance | refund aggregation | DONE | Refund allocations included in seller revenue summary |
| Seller Finance | shop data isolation | DONE | Wallet/ledger/revenue derive shop from authenticated seller scope |
| Seller Finance | HTTP integration coverage | DONE | `SellerFinanceHttpE2ETest` with MySQL Testcontainers |
| Seller Finance | error contract | DONE | Seller finance 400/401/403/404 behavior aligned and tested |
| Observability | Observability architecture audit | DONE | Phase 9A audited existing health, metrics, tracing and operational boundaries before extension |
| Observability | Operation metrics foundation | DONE | `OperationMetricsPort` + `MicrometerOperationMetricsAdapter`; bounded `operation` and `result` tags |
| Observability | Outbox publish/DLQ metrics | DONE | Publish/DLQ counters and latency timers plus operational backlog gauges |
| Observability | Kafka consumer metrics | DONE | Shared inbox processor records bounded consumer/event/topic/result counters and latency |
| Observability | HTTP request metrics | DONE | Method, route-template and status counters/latency; raw URI is not used as a metric tag |
| Observability | Request/trace propagation | DONE | W3C trace context is created/continued across HTTP, outbox and Kafka transport metadata |
| Observability | Business outcome metrics | DONE | Payment, refund, payout and settlement retry outcome counters |
| Observability | Structured correlation logging | DONE | MDC `request_id`, `trace_id`, `span_id` at HTTP/Kafka boundaries with cleanup after processing |
| Health | `/health/live` | DONE | Liveness depends only on service process state |
| Health | `/health/liveness` | DONE | Conventional Spring liveness probe retained |
| Health | `/health/ready` | DONE | DB/Kafka/outbox/VNPAY readiness group with hardened failure handling |
| Health | `/health/readiness` | DONE | Conventional Spring readiness probe retained |
| Operations | Prometheus registry | DONE | Micrometer Prometheus runtime registry |
| Operations | `/prometheus` scrape endpoint | DONE | Explicit Actuator exposure; custom service metrics are scrapeable |
| Operations | Sensitive Actuator endpoints protected | DONE | `/metrics`, `/env`, `/beans`, `/configprops`, `/heapdump`, `/threaddump`, `/loggers` are not exposed by default |
| Operations | Structured operational diagnostics | DONE | Readiness + Prometheus + structured logs + existing reconciliation/settlement APIs provide current diagnostic surface |
| Operations | Dedicated admin operations API | NOT REQUIRED | Phase 9K intentionally skipped; no `/api/v1/admin/operations` contract introduced |
| Operations | Dockerfile/Kafka local stack | DONE | Local runtime composition available |
| Operations | OpenAPI/runbook | TODO | Deferred to Phase 10 contract/E2E/operational hardening |

## Phase 9 closeout

Phase 9 completes the current observability and operations foundation without
changing financial business rules or inventing new public business APIs.

Completed runtime capabilities:

- Micrometer operation metrics;
- outbox publish, DLQ and backlog metrics;
- Kafka consumer metrics;
- bounded HTTP request metrics;
- payment/refund/payout/settlement outcome metrics;
- W3C HTTP/outbox/Kafka trace propagation;
- hardened readiness behavior;
- Prometheus scrape exposure;
- MDC structured correlation logging.

Operational diagnostics Phase 9K was reviewed and intentionally skipped.

The current combination of:

- `/health/ready`;
- `/health/readiness`;
- `/prometheus`;
- structured correlated logs;
- admin payment reconciliation;
- admin settlement read/retry;

is sufficient for the current runtime scope.

No `/api/v1/admin/operations` endpoint is part of the runtime contract.