# ADR-002 — Payment Contract Alignment

- Status: Accepted
- Date: 2026-09-29
- Service: payment-wallet-service

## 1. Context

The current Payment-Wallet documents contain a contract mismatch.

The accepted domain/database model defines one logical Payment per
checkout_group, where one checkout_group can contain multiple child orders.

The current API specification for POST /api/v1/payments exposes a single
order_id, while the existing domain model, database model and implementation
support multiple payment_orders under one Payment.

The API specification also accepts expires_at from the caller, while the
Payment-Wallet LLD defines PAYMENT_INTENT_TTL = 15 minutes and the service
currently calculates the VNPAY expiration time internally.

The Payment-Wallet LLD states that Payment-Wallet owns payment state and must
validate monetary data against the Order contract rather than blindly trust
client-supplied allocation data.

## 2. Decisions

### 2.1 Payment aggregate scope

One Payment represents one checkout_group.

A checkout_group may contain one or more child orders.

The existing relationship remains:

Payment 1 ── N PaymentOrder

There is no direct order_id column on payments.

### 2.2 Create Payment request

POST /api/v1/payments identifies the checkout_group instead of treating one
order as one payment.

The caller must not provide shop allocation values that Payment-Wallet treats
as authoritative financial data.

The canonical request is:

- checkout_group_id
- buyer_user_id
- method
- amount
- currency

Payment-Wallet obtains the authoritative order/payment snapshot through an
OrderSnapshotPort.

The snapshot must provide at least:

- checkout_group_id
- buyer_user_id
- child orders
- order_id
- shop_id
- merchandise amount
- shipping fee
- grand total
- currency

Payment-Wallet validates the requested amount against the authoritative
grand total before creating the Payment.

### 2.3 Payment expiration

expires_at is not accepted as an authoritative client input.

For VNPAY, Payment-Wallet calculates:

expires_at = now + PAYMENT_INTENT_TTL

The baseline PAYMENT_INTENT_TTL is 15 minutes.

For COD, expires_at is null.

The calculated expires_at is returned in the response.

### 2.4 Multi-shop order allocation

The external create-payment command does not contain authoritative shop
allocation data.

PaymentOrder records are constructed from the OrderSnapshot returned by
OrderSnapshotPort.

Seller financial allocation is created only after payment capture.

### 2.5 Compatibility

The current Java request containing orders[] is temporary implementation
debt.

It must be replaced when the Payment API contract is aligned.

The database model payments + payment_orders remains valid and does not
require reverting to one payment per order.

## 3. Consequences

Advantages:

- Keeps one Payment per checkout_group.
- Supports multi-shop checkout.
- Prevents Payment-Wallet from trusting caller-provided shop allocations.
- Makes Order-Commerce the source of truth for order monetary snapshots.
- Keeps payment expiration controlled by Payment-Wallet.
- Supports shipping-fee accounting required by the LLD.

Trade-offs:

- Requires OrderSnapshotPort and an infrastructure adapter.
- Requires changing the current CreatePaymentRequest/CreatePaymentCommand.
- Existing HTTP tests for POST /payments must be updated.
- Order-Commerce and Payment-Wallet contracts must use the same checkout
  snapshot semantics.