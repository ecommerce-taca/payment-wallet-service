# ADR-003 — Payment-Wallet Open Cross-Service Contracts

- Status: Proposed
- Date: 2026-09-29
- Service: payment-wallet-service

## 1. Purpose

This ADR records cross-service information required by Payment-Wallet but
whose source is not fully defined by the current Payment-Wallet LLD/API.

No placeholder or fabricated value may be used in production event payloads.

## 2. Buyer notification recipient

Payment-Wallet LLD requires the following integration events to contain:

buyer.user_id
buyer.email

Events:

- payment.succeeded
- payment.failed
- payment.expired
- payment.refunded

Current Payment aggregate stores buyer_user_id but does not store buyer email.

Status:

OPEN — authoritative source for buyer.email must be defined.

Until resolved:

- Do not emit fake email.
- Do not emit empty email.
- Do not use "unknown@example.com".
- Do not silently make Payment-Wallet query another service.

## 3. Payout notification recipient

Payment-Wallet LLD requires:

owner_user_id
owner_email

for:

- payout.succeeded
- payout.failed

Current Payout contains shop_id but not owner identity/email.

Status:

OPEN — authoritative seller owner recipient contract must be defined.

Until resolved:

- shop_id must not be treated as owner_user_id.
- owner_email must not be fabricated.
- payout integration events remain incomplete for Notification EMAIL mapping.

## 4. Shop payout gate projection

Payment-Wallet consumes:

- shop.kyc.approved
- shop.kyc.expired
- shop.status_changed

The local projection schema and exact event payload are not yet defined in
this repository.

Status:

OPEN — define local shop payout-gate projection before exposing production
seller payout.

## 5. Bank account source

POST /seller/payouts uses bank_account_id in the API contract.

Current application command accepts bankCode, accountHolderName and
maskedAccountNumber directly.

Status:

OPEN — bank-account ownership/source contract must be defined before the
public payout API is considered complete.

## 6. Settlement trigger

The LLD explicitly leaves settlement triggering open.

Current Order-Commerce contract does not provide order.completed.

Status:

OPEN — do not implement an order.completed consumer unless the upstream
contract is explicitly extended.

### Settlement candidate eligibility

The persistence layer may identify allocations that have not yet appeared in
`settlement_lines`, but this is not sufficient to release seller funds.

The following remain OPEN:

- refund/return hold-window length;
- exact eligible timestamp;
- whether eligibility is based on shipment delivery, order lifecycle, or a
  scheduled finance window;
- treatment of allocations with pending or completed refunds.

Until these are resolved, `SettlementCandidatePort` must not be wired to a
production `RunSettlementUseCase`.

### Production wiring guard

The persistence foundation distinguishes between:

- `UnsettledSettlementCandidate`: an allocation that has not yet appeared in
  `settlement_lines`;
- `SettlementCandidate`: an allocation that has passed the authoritative
  settlement eligibility policy and may be released.

`SettlementCandidatePersistenceAdapter` only produces
`UnsettledSettlementCandidate`.

It must not be treated as a production implementation of
`SettlementCandidatePort`.

Until the eligibility contract is resolved:

- no production `SettlementCandidatePort` bean is registered;
- no production `RunSettlementUseCase` bean is registered;
- `RunSettlementService` remains testable with explicit fake eligible
  candidates;
- no allocation may move from `SELLER_PENDING` to `SELLER_AVAILABLE`
  solely because it is absent from `settlement_lines`.

## 7. Rules

An OPEN item in this ADR:

- must not be solved with fabricated data;
- must not introduce a new cross-service dependency silently;
- must be resolved by a separate Accepted ADR or an authoritative upstream
  contract before production behavior depends on it.