# Payment Wallet Service Observability Contract

## 1. Scope

This document freezes the observability contract used by
`payment-wallet-service`.

It covers:

- health and readiness;
- metrics naming and tags;
- correlation identifiers;
- logging safety;
- actuator exposure.

This contract does not define an external monitoring stack such as
Prometheus, Grafana, Loki or OpenTelemetry Collector.

## 2. Health contract

### 2.1 Liveness

Canonical endpoint:

`GET /health/live`

Liveness indicates whether the service process is able to run.

Liveness must not depend on:

- MySQL availability;
- Kafka availability;
- VNPAY configuration or availability;
- outbox backlog;
- other external services.

A temporary external dependency failure must not cause the process to be
restarted through liveness failure.

Spring's conventional probe remains available:

`GET /health/liveness`

### 2.2 Readiness

Canonical endpoint:

`GET /health/ready`

Readiness indicates whether the service is ready to serve its production
responsibilities.

The readiness group currently includes:

- `readinessState`;
- `db`;
- `kafkaReadiness`;
- `outboxBacklog`;
- `vnpayReadiness`.

Spring's conventional probe remains available:

`GET /health/readiness`

### 2.3 Kafka readiness

Kafka readiness validates that the required Kafka topics are available.

Required topics:

- `payment.events.v1`;
- `wallet.events.v1`;
- `shipment.events.v1`;
- `payment-wallet.outbox.dlq.v1`.

If Kafka is unavailable or a required topic is missing, readiness may become
`OUT_OF_SERVICE`.

The health check must respect the configured timeout.

### 2.4 Outbox backlog readiness

Outbox readiness evaluates the oldest pending supported outbox event.

Baseline:

`app.outbox.health.max-lag = 5m`

A backlog older than the configured maximum lag may make readiness
`OUT_OF_SERVICE`.

Pending count alone does not make the service unhealthy while the oldest
pending event remains within the allowed lag.

### 2.5 VNPAY readiness

VNPAY readiness validates the required runtime configuration.

Health output must never expose:

- hash secret;
- signatures;
- credentials;
- raw provider payload.

## 3. Metrics contract

### 3.1 Naming

Custom service metrics use the prefix:

`payment_wallet_`

Metric names must describe a stable operational concept rather than an
implementation class or method.

Existing metric names are frozen:

- `payment_wallet_outbox_publish_total`;
- `payment_wallet_outbox_publish_latency`;
- `payment_wallet_outbox_dlq_publish_total`.

Existing names must not be renamed without an explicit migration because
dashboards and alerts may depend on them.

### 3.2 Allowed tags

Metric tags must have bounded cardinality.

Allowed examples:

- `result`;
- `method`;
- `provider`;
- `event_type`;
- `topic`;
- `operation`;
- `status`.

The concrete value set for each tag must remain bounded by application
contracts or enums.

### 3.3 Forbidden high-cardinality tags

Metrics must not use identifiers or unbounded values as tags.

Forbidden examples:

- `payment_id`;
- `refund_id`;
- `batch_id`;
- `posting_id`;
- `user_id`;
- `shop_id`;
- `request_id`;
- `provider_transaction_ref`;
- exception messages;
- raw URLs;
- arbitrary reason text.

Identifiers belong in logs or traces when safe, not metric dimensions.

### 3.4 Result values

Result tags should use a small stable vocabulary such as:

- `success`;
- `failure`;
- `duplicate`;
- `rejected`.

New result values must not contain dynamic exception names or messages unless
the metric contract explicitly defines a bounded error classification.

## 4. Correlation contract

### 4.1 Request ID

Externally exposed business APIs use:

`X-Request-ID`

The request ID is intended for request correlation.

It must not be used as a metric tag.

### 4.2 Distributed trace context

W3C trace headers are the canonical distributed trace contract:

- `traceparent`;
- `tracestate`.

A proprietary trace header must not be introduced when W3C trace context is
sufficient.

### 4.3 Kafka/outbox correlation

Where correlation metadata is propagated through outbox and Kafka, the
existing metadata/header mapping must be preserved.

Observability work must not modify business event payloads merely to add
monitoring fields when transport metadata is sufficient.

## 5. Logging contract

Operational logs must be useful for investigation without leaking sensitive
information.

Safe structured fields may include:

- operation;
- request ID;
- trace ID when available;
- event type;
- topic;
- provider;
- result;
- HTTP status;
- duration.

Sensitive data must not be logged:

- JWT/access token;
- VNPAY hash secret;
- provider signature;
- raw signed webhook payload;
- bank account credentials;
- full card/account data;
- passwords;
- authorization headers.

Expected client validation failures must not generate unnecessary ERROR-level
stack traces.

## 6. Actuator exposure

Current public actuator exposure is intentionally limited to:

- `health`;
- `info`.

Health details use:

`show-details: never`

The following must not be publicly exposed by default:

- `env`;
- `configprops`;
- `beans`;
- `heapdump`;
- `threaddump`;
- `loggers`.

`metrics` or `prometheus` must not be exposed until an explicit operational
contract and access policy are introduced.

## 7. Current implementation baseline

Current runtime observability components:

- `KafkaReadinessHealthIndicator`;
- `OutboxBacklogHealthIndicator`;
- `VnpayReadinessHealthIndicator`;
- `OutboxPublisherObservation`.

Current custom outbox metrics:

- `payment_wallet_outbox_publish_total`;
- `payment_wallet_outbox_publish_latency`;
- `payment_wallet_outbox_dlq_publish_total`.

Phase 9 may extend this baseline but must preserve the contracts above.

## 8. Phase 9 guardrails

Phase 9 must not:

- change financial business rules for observability;
- expose secrets through logs, health or metrics;
- introduce high-cardinality metrics;
- make liveness depend on external infrastructure;
- expose sensitive actuator endpoints publicly;
- introduce a vendor-specific monitoring stack without an explicit deployment
  requirement;
- production-wire currently gated business capabilities merely to observe
  them.