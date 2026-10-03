# ADR-004 — Async COD Workflow Contract

- Status: Accepted
- Date: 2026-10-02
- Service: payment-wallet-service

## 1. Context

Payment-Wallet consumes asynchronous shipment/order events for COD payment
processing.

The accepted payment model defines:

- one Payment per checkout_group;
- one checkout_group may contain multiple child orders;
- one Payment owns multiple PaymentOrder records.

The current ProcessCodPaymentService operates at checkout_group scope:
a successful COD result marks the entire Payment SUCCESS, allocates all child
orders, posts the full capture ledger, and credits seller pending balances.

The Payment-Wallet LLD, however, describes COD confirmation through
shipment.delivered / shipment.failed and refers to the delivered order's
grand_total.

Shipment lifecycle is normally order/shipment scoped and child orders of one
checkout_group may not necessarily complete at the same time.

Therefore wiring shipment.delivered directly to the existing
ProcessCodPaymentUseCase could release funds for child orders that have not
been delivered.

## 2. Confirmed facts

The following are already authoritative:

- COD Payment starts in PENDING_COD.
- PENDING_COD does not block fulfillment.
- shipment.delivered is a trigger for COD confirmation.
- shipment.failed or order cancellation before delivery causes COD failure.
- shipping fee is posted to SHIPMENT_PAYABLE.
- seller wallet allocation occurs only after COD capture.
- Payment is scoped to checkout_group.
- checkout_group may contain multiple child orders.

## 3. Decision

Payment-Wallet uses per-order COD capture.

Each PaymentOrder owns its COD processing state:

- PENDING
- CAPTURED
- FAILED

A delivered child order may be financially captured independently.

The parent Payment remains PENDING_COD until all child orders are CAPTURED.
When all child orders are CAPTURED, Payment becomes SUCCESS.

When all child orders are FAILED and no amount has been captured, Payment becomes FAILED.

For mixed multi-order outcomes such as CAPTURED + FAILED, the existing PaymentStatus
model has no authoritative aggregate status. Until that contract is extended:

- child PaymentOrder state is authoritative;
- captured financial postings remain valid;
- failed child orders are not allocated;
- Payment remains PENDING_COD;
- no checkout-level payment.succeeded/payment.failed event is emitted.

Shipment/order events provide order_id for correlation.

Payment-Wallet does not use amount/currency from shipment events as monetary authority.
Financial values are read from payment_orders.

## 4. Event identity contract

Before production Kafka listeners are registered, the authoritative Shipment
and Order event contracts must define the fields required to correlate an
incoming event with Payment-Wallet state.

At minimum the contract must identify the affected child order.

Required semantic identifier:

- order_id

Additional identifiers such as shipment_id, checkout_group_id or payment_id
must not be assumed unless upstream contracts explicitly define them.

## 5. Monetary authority

Shipment events are not treated as the authoritative source of payment amount.

Payment-Wallet must use monetary data already stored in:

- payments
- payment_orders

Incoming shipment/order events must not be trusted to determine COD capture
amount.

## 6. Current implementation debt

ProcessCodPaymentCommand currently contains:

- checkoutGroupId
- amount
- currency

This shape is suitable for direct application tests but is not yet aligned
with asynchronous shipment processing.

The async workflow should not propagate untrusted shipment monetary fields
into this command.

## 7. Production guard

Until the multi-order COD decision and upstream event payload are accepted:

- do not register shipment.delivered production listener;
- do not register shipment.failed production listener;
- do not register order.cancelled production listener that mutates COD state;
- do not fabricate checkout_group_id from unrelated fields;
- do not trust amount/currency from shipment events;
- keep ProcessCodPaymentUseCase callable and testable directly.

## 8. Follow-up

After this ADR is accepted:

- refactor ProcessCodPaymentUseCase to the chosen financial scope;
- add repository lookup by authoritative event identifier;
- add Kafka listener adapters;
- execute through KafkaInboxProcessor;
- add duplicate and out-of-order integration tests.