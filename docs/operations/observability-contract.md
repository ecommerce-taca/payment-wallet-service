# Payment Wallet Service Observability Contract

## 1. Scope

This document freezes the runtime observability contract used by
`payment-wallet-service` after Phase 9.

It covers:

- health and readiness;
- service and business metrics;
- W3C trace propagation;
- request/Kafka correlation;
- structured logging safety;
- Actuator and Prometheus exposure.

The service does not require a specific external dashboard, log aggregation
platform or trace collector.

Prometheus-compatible scraping is supported by the runtime endpoint defined
in this document.

## 2. Health contract

### 2.1 Liveness

Canonical endpoint:

`GET /health/live`

Spring conventional endpoint:

`GET /health/liveness`

Liveness indicates whether the service process is able to run.

Liveness must not depend on:

- MySQL availability;
- Kafka availability;
- VNPAY configuration or availability;
- outbox backlog;
- other external services.

A temporary external dependency failure must not cause a liveness failure or
trigger unnecessary service restarts.

### 2.2 Readiness

Canonical endpoint:

`GET /health/ready`

Spring conventional endpoint:

`GET /health/readiness`

Readiness indicates whether the service is ready to serve its production
responsibilities.

The readiness group contains:

- `readinessState`;
- `db`;
- `kafkaReadiness`;
- `outboxBacklog`;
- `vnpayReadiness`.

Dependency failures may make readiness `OUT_OF_SERVICE` while liveness remains
`UP`.

### 2.3 Kafka readiness

Kafka readiness validates that the required Kafka topics are available.

Required topics:

- `payment.events.v1`;
- `wallet.events.v1`;
- `shipment.events.v1`;
- `payment-wallet.outbox.dlq.v1`.

If a required topic is missing, readiness becomes `OUT_OF_SERVICE` with:

`reason = required_topics_missing`

If Kafka cannot be queried, readiness becomes `OUT_OF_SERVICE` with:

`reason = kafka_unavailable`

The health check respects:

`app.kafka.health.timeout`

Health output may include bounded diagnostic fields such as:

- `enabled`;
- `requiredTopicCount`;
- `missingTopics`;
- `reason`;
- bounded exception `errorType`.

Health output must not expose arbitrary exception messages.

### 2.4 Outbox backlog readiness

Outbox readiness evaluates supported unpublished, non-dead-lettered outbox
events.

Baseline:

`app.outbox.health.max-lag = 5m`

The runtime records:

- current pending count;
- oldest pending event timestamp;
- oldest pending lag;
- configured maximum lag.

A backlog whose oldest pending event exceeds the configured maximum lag makes
readiness `OUT_OF_SERVICE` with:

`reason = outbox_backlog_exceeded`

A repository or backlog-query failure makes readiness `OUT_OF_SERVICE` with:

`reason = outbox_unavailable`

Pending count alone does not make the service unhealthy while the oldest
pending event remains inside the configured lag threshold.

If no supported event types exist, the check remains `UP` and reports that no
supported event types are available.

### 2.5 VNPAY readiness

VNPAY readiness validates required runtime configuration.

Incomplete required configuration makes readiness `OUT_OF_SERVICE` with:

`reason = configuration_incomplete`

Health output must never expose:

- VNPAY hash secret;
- signatures;
- credentials;
- raw provider payload;
- sensitive configuration values.

## 3. Metrics contract

### 3.1 Naming

Custom service metrics use the prefix:

`payment_wallet_`

Metric names must describe stable operational concepts rather than
implementation classes or method names.

Current runtime metric contract is defined below.

### 3.2 Operation metrics

Metrics:

- `payment_wallet_operation_total`;
- `payment_wallet_operation_latency`.

Tags:

- `operation`;
- `result`.

Current operation vocabulary includes bounded application operations such as:

- create payment;
- process VNPAY webhook;
- request refund;
- retry settlement.

Result values use a bounded vocabulary such as:

- `success`;
- `failure`;
- `duplicate`;
- `rejected`.

### 3.3 Outbox publisher metrics

Metrics:

- `payment_wallet_outbox_publish_total`;
- `payment_wallet_outbox_publish_latency`;
- `payment_wallet_outbox_dlq_publish_total`;
- `payment_wallet_outbox_dlq_publish_latency`.

Tags:

- `event_type`;
- `topic`;
- `result`.

The metrics distinguish normal outbox publishing from DLQ publishing.

### 3.4 Outbox operational metrics

Metrics:

- `payment_wallet_outbox_pending`;
- `payment_wallet_outbox_oldest_lag_seconds`;
- `payment_wallet_outbox_cleanup_deleted_total`.

`payment_wallet_outbox_pending` and
`payment_wallet_outbox_oldest_lag_seconds` represent current operational
state.

`payment_wallet_outbox_cleanup_deleted_total` is cumulative.

These metrics must not introduce event IDs, aggregate IDs or request IDs as
tags.

### 3.5 Kafka consumer metrics

Metrics:

- `payment_wallet_kafka_consume_total`;
- `payment_wallet_kafka_consume_latency`.

Tags:

- `consumer`;
- `event_type`;
- `topic`;
- `result`.

Current result values are bounded:

- `success`;
- `duplicate`;
- `failure`.

Consumer metrics are recorded around the shared Kafka inbox processing
boundary rather than being duplicated independently in each listener.

### 3.6 HTTP metrics

Metrics:

- `payment_wallet_http_request_total`;
- `payment_wallet_http_request_latency`.

Tags:

- `method`;
- `route`;
- `status`.

`route` must use the bounded Spring route template when available.

Examples:

- `/api/v1/payments/{paymentId}`;
- `/api/v1/admin/settlements/{batchId}`;
- `unmatched`.

The raw request URI must never be used as a metric tag because IDs and other
dynamic path values would create high cardinality.

### 3.7 Business outcome metrics

Metrics:

- `payment_wallet_payment_outcome_total`;
- `payment_wallet_refund_outcome_total`;
- `payment_wallet_payout_outcome_total`;
- `payment_wallet_settlement_retry_total`.

Payment outcome tags:

- `method`;
- `status`;
- `action`.

Refund outcome tags:

- `status`;
- `action`.

Payout outcome tags:

- `status`;
- `action`.

Settlement retry tags:

- `status`.

Business outcome values are normalized to lowercase when recorded.

Business outcome metrics are recorded only after the corresponding
transaction successfully completes.

A rolled-back transaction must not be counted as an applied business outcome.

### 3.8 Allowed tags

Metric tags must have bounded cardinality.

Allowed examples include:

- `operation`;
- `result`;
- `method`;
- `status`;
- `action`;
- `consumer`;
- `event_type`;
- `topic`;
- `route`.

Each tag's concrete values must remain bounded by application contracts,
routes or enums.

### 3.9 Forbidden high-cardinality tags

Metrics must not use identifiers or unbounded values as tags.

Forbidden examples:

- `payment_id`;
- `refund_id`;
- `payout_id`;
- `batch_id`;
- `posting_id`;
- `order_id`;
- `checkout_group_id`;
- `event_id`;
- `aggregate_id`;
- `user_id`;
- `shop_id`;
- `request_id`;
- trace IDs;
- span IDs;
- provider transaction references;
- exception messages;
- raw URLs;
- arbitrary reason text.

Identifiers belong in logs or traces when safe, not metric dimensions.

### 3.10 Metric compatibility

Existing metric names are treated as operational contracts.

They must not be renamed without an explicit migration because dashboards,
alerts and external Prometheus queries may depend on them.

## 4. Correlation and tracing contract

### 4.1 Request ID

Externally exposed business APIs use:

`X-Request-ID`

The request ID is intended for request correlation.

It must not be used as a metric tag.

### 4.2 W3C trace context

Canonical distributed trace headers are:

- `traceparent`;
- `tracestate`.

A proprietary trace header must not be introduced when W3C trace context is
sufficient.

For inbound HTTP processing:

- a valid incoming `traceparent` continues the same trace;
- the service creates a new span ID;
- a missing `traceparent` creates a new valid trace context;
- an invalid `traceparent` creates a new valid trace context;
- the effective `traceparent` is returned in the HTTP response.

For inbound Kafka processing:

- a valid Kafka `traceparent` continues the same trace;
- the consumer processing boundary creates a new span ID;
- a missing or invalid trace context results in a new valid trace context.

### 4.3 Trace validation

The runtime uses W3C version `00` traceparent format:

`00-<32 hex trace id>-<16 hex parent/span id>-<2 hex flags>`

Trace IDs and span IDs must not be all zeroes.

Invalid trace context must not be propagated unchanged.

### 4.4 Kafka/outbox correlation

Where correlation metadata is propagated through outbox and Kafka, the
existing metadata/header mapping is preserved.

Outbox/Kafka correlation metadata includes, when available:

- event ID;
- request ID;
- `traceparent`;
- `tracestate`.

Observability work must not modify financial event payloads merely to add
monitoring fields when transport metadata is sufficient.

### 4.5 Exporter scope

Phase 9 implements distributed trace-context generation, continuation and
propagation.

Phase 9 does not require:

- Tempo exporter;
- Zipkin exporter;
- Jaeger exporter;
- OpenTelemetry Collector;
- vendor-specific tracing backend.

A tracing exporter can be added later without changing the business
contracts defined here.

## 5. Structured logging contract

### 5.1 Correlation fields

HTTP and Kafka processing boundaries bind correlation metadata into SLF4J
MDC.

Current MDC keys:

- `request_id`;
- `trace_id`;
- `span_id`.

MDC must be cleared after processing even when processing fails.

This is required because servlet and Kafka worker threads can be reused.

Correlation metadata from one request or message must never leak into the
next request or message handled by the same thread.

### 5.2 Console format

Runtime console logging contains stable fields for:

- timestamp;
- level;
- service;
- `trace_id`;
- `span_id`;
- `request_id`;
- logger;
- message.

Operational messages continue to use bounded structured key/value content
such as:

- `event`;
- `event_type`;
- `topic`;
- `result`;
- `retry_count`;
- `duration_ms`;
- bounded `error_type`.

### 5.3 Logging safety

Sensitive data must not be logged.

Forbidden examples include:

- JWT/access token;
- Authorization header;
- VNPAY hash secret;
- provider signature;
- raw signed webhook payload;
- raw Kafka business payload;
- passwords;
- bank account credentials;
- full card/account data;
- sensitive PII.

Exception messages must not automatically become metric dimensions.

Operational logs should prefer a bounded exception class/type over arbitrary
exception text when detailed text is unnecessary.

Expected client validation failures must not generate unnecessary ERROR-level
stack traces.

## 6. Actuator and Prometheus exposure

### 6.1 Management base path

Management web base path remains:

`/`

The existing health API paths therefore remain unchanged.

### 6.2 Exposed endpoints

Runtime Actuator exposure is intentionally limited to:

- `health`;
- `info`;
- `prometheus`.

Canonical Prometheus scrape endpoint:

`GET /prometheus`

Health details use:

`show-details: never`

### 6.3 Endpoints not exposed

The following are not exposed by default:

- `metrics`;
- `env`;
- `configprops`;
- `beans`;
- `heapdump`;
- `threaddump`;
- `loggers`;
- other sensitive Actuator endpoints.

Requests to unexposed or nonexistent resources are handled as HTTP `404`
rather than being converted into generic internal server errors.

### 6.4 Prometheus registry

The runtime includes the Micrometer Prometheus registry.

Custom Micrometer meters are available through:

`GET /prometheus`

A meter may not appear in the scrape output until that meter has been
created or recorded by the runtime.

### 6.5 Production access

`/prometheus` is an operational endpoint, not a business API.

Production network configuration should restrict access to trusted monitoring
infrastructure where appropriate.

Phase 9 does not introduce a separate management port.

## 7. Current implementation baseline

Current runtime observability components include:

- `MicrometerOperationMetricsAdapter`;
- `OutboxPublisherObservation`;
- `OutboxOperationalObservation`;
- `KafkaConsumerObservation`;
- `HttpRequestObservation`;
- `MicrometerBusinessMetricsAdapter`;
- `W3cTraceContext`;
- `MdcCorrelationContext`;
- `KafkaReadinessHealthIndicator`;
- `OutboxBacklogHealthIndicator`;
- `VnpayReadinessHealthIndicator`.

Observability is wired through the existing application and infrastructure
boundaries without changing financial domain rules.

## 8. Testing contract

Representative Phase 9 coverage includes:

- observability wiring/audit tests;
- operation metric contract tests;
- outbox publisher metric contract tests;
- outbox operational metric tests;
- Kafka consumer observation tests;
- Kafka inbox processing tests;
- HTTP metric contract/filter tests;
- W3C trace-context tests;
- request metadata/filter tests;
- MDC correlation tests;
- business metric contract tests;
- readiness indicator tests;
- health HTTP integration tests;
- readiness failure HTTP integration tests;
- Prometheus endpoint HTTP integration tests.

The full regression suite remains the final Phase 9 closeout gate.

## 9. Phase 9 guardrails

Phase 9 must not:

- change financial business rules for observability;
- expose secrets through logs, health or metrics;
- introduce high-cardinality metrics;
- make liveness depend on external infrastructure;
- expose sensitive Actuator endpoints publicly;
- introduce a vendor-specific monitoring backend without an explicit
  deployment requirement;
- production-wire currently gated business capabilities merely to observe
  them;
- invent new public/admin API contracts for diagnostics.

## 10. Phase 9 closeout

Implementation status:

- 9A Observability Audit — DONE;
- 9B Metrics Foundation — DONE;
- 9C Outbox Metrics — DONE;
- 9D Kafka Observability — DONE;
- 9E HTTP Metrics — DONE;
- 9F Distributed Trace Context — DONE;
- 9G Business Metrics — DONE;
- 9H Health / Readiness Hardening — DONE;
- 9I Actuator / Prometheus Exposure — DONE;
- 9J Structured Logging — DONE;
- 9K Operational Diagnostics — intentionally skipped.

9K is intentionally skipped because the existing combination of:

- readiness;
- Prometheus metrics;
- structured logs;
- payment reconciliation;
- settlement diagnostics;

already provides sufficient operational diagnostics for the current runtime
scope.

No new operational admin API is introduced.

In particular, Phase 9 does not introduce:

`/api/v1/admin/operations`

or:

`/api/v1/admin/operations/outbox`

Phase 9 is complete when the full regression suite succeeds.