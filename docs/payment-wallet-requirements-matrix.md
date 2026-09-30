# Payment-Wallet Requirements Traceability

Status:

- DONE — implemented and tested.
- PARTIAL — foundation exists but requirement is incomplete.
- TODO — not implemented.
- BLOCKED — authoritative external contract is missing.

| Area | Requirement | Status | Implementation / Gap |
|---|---|---:|---|
| Architecture | DDD + Hexagonal boundaries | DONE | `ArchitectureTest` |
| Payment | VNPAY create intent | DONE | CreatePayment flow |
| Payment | COD PENDING_COD | DONE | Payment domain |
| Payment | Get payment detail API | TODO | No query use case/controller |
| Payment | Order authoritative snapshot | TODO | `OrderSnapshotPort` missing |
| Payment | Shipping fee separated from seller gross | DONE | `PaymentOrder.merchandiseAmount`, `shippingFee`; `AllocationCalculator` uses merchandise only |
| VNPAY | Payment URL creation | DONE | VNPAY adapter |
| VNPAY | Verified webhook + dedupe | DONE | Webhook service |
| VNPAY | Provider IP policy | TODO | Missing |
| Wallet | pending/available projection | DONE | Wallet domain/persistence |
| Ledger | double-entry posting | DONE | LedgerPosting |
| Ledger | shipment payable | DONE | `SHIPMENT_PAYABLE`; capture posting credits shipping separately |
| Refund | request/application workflow | PARTIAL | Use case exists; REST/provider worker missing |
| Payout | reserve workflow | PARTIAL | Use case exists; KYC/provider/API missing |
| Payout | KYC/status projection | TODO | Missing |
| Settlement | domain/persistence | DONE | Settlement aggregate, repository, lines and unsettled candidate persistence foundation |
| Settlement | eligibility policy | BLOCKED | Refund/return window and authoritative eligible timestamp remain open in ADR-003 |
| Settlement | production execution | BLOCKED | `SettlementCandidatePort` and `RunSettlementUseCase` intentionally not wired until eligibility is resolved |
| Kafka | outbox persistence | PARTIAL | DB + adapter only |
| Kafka | publisher/retry/DLQ | TODO | Missing |
| Kafka | consumers | TODO | Missing |
| Events | payment.created | DONE | Domain/outbox |
| Events | wallet.allocated | DONE | Domain/outbox |
| Events | payment notification recipient | BLOCKED | ADR-003 |
| Events | payout notification recipient | BLOCKED | ADR-003 |
| API | Payment/refund endpoints | PARTIAL | Create/webhook only |
| API | Seller finance endpoints | TODO | Missing |
| API | Admin finance endpoints | TODO | Missing |
| Security | Actor/shop/FINANCE_OPS scope | TODO | Missing |
| Security | Step-up 2FA | TODO | Missing |
| Observability | Request/trace propagation | TODO | Missing |
| Health | `/health/live` | TODO | Existing route differs |
| Health | `/health/ready` | TODO | Missing |
| Operations | Dockerfile/Kafka local stack | TODO | Missing |
| Operations | OpenAPI/runbook | TODO | Missing |