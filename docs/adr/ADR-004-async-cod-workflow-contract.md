# ADR-004 — Async COD Workflow Contract

- Status: Proposed
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

## 3. Open decision — multi-order COD capture

The authoritative behavior for a multi-order checkout_group is not currently
defined.

The system must choose one of the following before production listener wiring:

### Option A — checkout-group atomic capture

Payment becomes SUCCESS only after all child orders are successfully delivered.

No seller allocation is released before the final required delivery.

This keeps one Payment transition and one capture posting.

### Option B — per-order capture

Each child order may be financially captured independently when its shipment
is delivered.

This requires additional domain/persistence state because current Payment
status does not represent partial capture.

Potential additions would include per-PaymentOrder capture state and possibly
a partially captured Payment status.

This option must not be implemented implicitly.

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