# API Spec — Payment-Wallet Service

> Nguồn: `docs/lld/payment-wallet.md` · `docs/db/payment-wallet.md` · HLD/Penpot · ADR-002 · ADR-003  
> Base path: `/api/v1` · VNPAY sandbox + COD · Payment secrets không đi qua client  
> Cập nhật: `2026-10-10`  
> Trạng thái: Phase 6 Payment + Refund APIs đã hoàn thiện; các API Seller/Admin bên dưới vẫn thuộc các phase tiếp theo.

## 1. Quy ước API chung

| Mục | Quy định |
|---|---|
| Auth | Buyer/seller/admin JWT qua Gateway. Payment-Wallet đọc actor context do Gateway inject. Internal Order-Commerce được kiểm tra bằng `InternalCallerPolicy`; transport production để populate `InternalCallerContext` vẫn là contract mở, không tự suy diễn header service-auth mới. |
| Actor context | Đọc `X-User-ID`, `X-User-Roles`, `X-User-Permissions`, `X-User-Shop-Scope` do Gateway inject. |
| Request ID | `X-Request-ID` bắt buộc ở các Payment/Refund HTTP APIs hiện tại; Gateway tạo/propagate. |
| Trace | W3C `traceparent`/`tracestate` được propagate qua REST/Kafka và được ghi vào outbox metadata khi có. |
| Time/money | ISO-8601 UTC; integer VND, không dùng FLOAT cho tiền. |
| Idempotency | Create Payment và Request Refund dùng `Idempotency-Key`; provider webhook dedupe theo provider event. |
| Response | Success: `{data,meta:{request_id}}`; error: `{error:{code,message},meta:{request_id}}`. |
| Pagination | API có phân trang dùng `page` từ 1, `size` mặc định 20 tối đa 100; meta có thể trả `page,size,total,total_pages`. |
| Redaction | Không trả/log token, VNPAY signature/secret, raw provider payload, bank/card credential hoặc PII nhạy cảm. |

### 1.1 Phạm vi contract hiện tại

Phase 6 triển khai và kiểm thử các endpoint:

- `POST /api/v1/payments`
- `GET /api/v1/payments/{paymentId}`
- `POST /api/v1/payments/webhook`
- `POST /api/v1/payments/{paymentId}/refunds`

Các Seller Finance APIs thuộc Phase 7 và Admin Finance APIs thuộc Phase 8. Các phần đó được giữ trong tài liệu để làm contract mục tiêu, không có nghĩa đã được expose đầy đủ trong runtime hiện tại.

## 2. Danh sách endpoint

| # | Method + path | Quyền | Mục đích | Trạng thái |
|---:|---|---|---|---|
| 1 | `POST /payments` | Trusted Order-Commerce internal caller | Tạo payment intent VNPAY/COD | Implemented |
| 2 | `GET /payments/{paymentId}` | Buyer owner, Order-Commerce internal, `FINANCE_OPS` | Xem payment detail | Implemented |
| 3 | `POST /payments/webhook` | VNPAY provider, không JWT | Reconcile callback | Implemented |
| 4 | `POST /payments/{paymentId}/refunds` | Order-Commerce internal hoặc `FINANCE_OPS` | Tạo refund intent | Implemented |
| 5 | `GET /seller/wallet` | Seller | Xem available/pending balance | Phase 7 |
| 6 | `GET /seller/wallet/ledger` | Seller | Xem ledger summary | Phase 7 |
| 7 | `GET /seller/revenue` | Seller | Báo cáo doanh thu | Phase 7 |
| 7a | `GET /seller/revenue/export` | Seller | Xuất báo cáo doanh thu | Phase 7 |
| 8 | `POST /seller/payouts` | Seller + step-up | Yêu cầu rút tiền | Phase 7 |
| 9 | `GET /seller/payouts` | Seller | Xem payout history | Phase 7 |
| 10 | `GET /admin/payments/reconciliation` | `FINANCE_OPS` | Reconcile provider/payment/ledger | Phase 8 |
| 11 | `GET /admin/fees` | `FINANCE_OPS` | Danh sách version commission | Phase 8 |
| 12 | `PUT /admin/fees` | `FINANCE_OPS` + step-up | Tạo commission version mới | Phase 8 |
| 13 | `GET /admin/taxes` | `FINANCE_OPS` | Danh sách version thuế | Phase 8 |
| 14 | `PUT /admin/taxes` | `FINANCE_OPS` + step-up | Tạo tax version mới | Phase 8 |
| 15 | `GET /admin/settlements` | `FINANCE_OPS` | Danh sách settlement batch | Phase 8 |
| 16 | `GET /admin/settlements/{batchId}` | `FINANCE_OPS` | Chi tiết settlement batch | Phase 8 |
| 17 | `POST /admin/settlements/{batchId}/retry` | `FINANCE_OPS` + step-up | Retry batch `FAILED` | Phase 8 |
| 18 | `GET /admin/finance/summary` | `FINANCE_OPS` | Tổng hợp tài chính sàn | Phase 8 |
| 18a | `GET /admin/finance/summary/export` | `FINANCE_OPS` | Xuất báo cáo tài chính | Phase 8 |
| 19 | `GET /health/live` | Ops | Liveness | Implemented |
| 20 | `GET /health/ready` | Ops | Readiness | Implemented |

> `POST /payments` và `POST /payments/{paymentId}/refunds` không dùng prefix `/internal/**`. Service tự enforce caller. Không được tự tạo một header service-auth mới như `X-Internal-Service` khi contract transport chưa được chốt.

## 3. Chi tiết endpoint

### 3.1 `POST /payments`

Header bắt buộc:

- `Idempotency-Key`
- `X-Request-ID`

Authorization:

- chỉ trusted Order-Commerce internal caller.

Request contract runtime hiện tại:

| Field | Kiểu | Bắt buộc | Ràng buộc |
|---|---|---|---|
| `checkout_group_id` | UUID | Có | Một payment đại diện cho một checkout group |
| `buyer_user_id` | UUID | Có | Buyer của checkout |
| `method` | enum | Có | `VNPAY` hoặc `COD` |
| `amount` | integer | Có | > 0 |
| `currency` | string | Có | Chỉ `"VND"` |
| `orders` | array | Có | Ít nhất một child order |
| `orders[].order_id` | UUID | Có | ID order |
| `orders[].shop_id` | UUID | Có | Shop sở hữu order |
| `orders[].amount` | integer | Có | Grand total của child order |
| `orders[].shipping_fee` | integer | Có | `>= 0` và `< amount` |

Ràng buộc:

- tổng `orders[].amount` phải bằng `amount`;
- `merchandise_amount` không phải request field, được suy ra bằng `amount - shipping_fee`;
- VNPAY có `expires_at` do service tính;
- COD có `expires_at = null`.

> Theo ADR-002, `orders[]` trong request hiện tại là implementation debt. Canonical contract tương lai sẽ dùng authoritative Order snapshot thay vì tin allocation do caller gửi. Phase 6 không tự triển khai `OrderSnapshotPort` khi upstream contract chưa hoàn tất.

Request example:

```json
{
  "checkout_group_id": "01912f90-7a1b-7c12-9c55-8b1c34a6d921",
  "buyer_user_id": "01912f80-7a1b-7c12-9c55-8b1c34a6d921",
  "method": "VNPAY",
  "amount": 1094000,
  "currency": "VND",
  "orders": [
    {
      "order_id": "01912f91-7a1b-7c12-9c55-8b1c34a6d921",
      "shop_id": "01912f31-7a1b-7c12-9c55-8b1c34a6d921",
      "amount": 1094000,
      "shipping_fee": 20000
    }
  ]
}
```

Response `201 Created`:

```json
{
  "data": {
    "payment_id": "01912f95-7a1b-7c12-9c55-8b1c34a6d921",
    "checkout_group_id": "01912f90-7a1b-7c12-9c55-8b1c34a6d921",
    "status": "PENDING",
    "method": "VNPAY",
    "amount": 1094000,
    "currency": "VND",
    "payment_url": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
    "expires_at": "2026-10-10T10:15:00Z"
  },
  "meta": {
    "request_id": "01912fa6-7a1b-7c12-9c55-8b1c34a6d921"
  }
}
```

COD trả `status: "PENDING_COD"` và không có `payment_url`/`expires_at`.

Tạo payment URL không đồng nghĩa thanh toán thành công. Payment chỉ chuyển state tài chính khi workflow tương ứng được xác nhận.

### 3.2 `GET /payments/{paymentId}`

Header bắt buộc:

- `X-Request-ID`

Authorization hiện tại:

- Order-Commerce internal caller: allow;
- `FINANCE_OPS`: allow;
- buyer: chỉ buyer sở hữu payment;
- seller/seller staff: generic payment detail bị từ chối.

Seller không được lọc trực tiếp từ generic payment response vì response chứa payment-level totals của checkout có thể gồm nhiều shop. Seller-specific projection thuộc Phase 7.

Response `200 OK`:

```json
{
  "data": {
    "payment_id": "01912f95-7a1b-7c12-9c55-8b1c34a6d921",
    "checkout_group_id": "01912f90-7a1b-7c12-9c55-8b1c34a6d921",
    "status": "SUCCESS",
    "method": "VNPAY",
    "amount": 1094000,
    "currency": "VND",
    "captured_amount": 1094000,
    "refunded_amount": 0,
    "expires_at": "2026-10-10T10:15:00Z",
    "paid_at": "2026-10-10T10:03:12Z",
    "orders": [
      {
        "order_id": "01912f91-7a1b-7c12-9c55-8b1c34a6d921",
        "shop_id": "01912f31-7a1b-7c12-9c55-8b1c34a6d921",
        "merchandise_amount": 1074000,
        "shipping_fee": 20000,
        "amount": 1094000
      }
    ]
  },
  "meta": {
    "request_id": "01912fa7-7a1b-7c12-9c55-8b1c34a6d921"
  }
}
```

`expires_at` hoặc `paid_at` có thể vắng mặt khi `null`.

Response không expose:

- `buyer_user_id`;
- raw provider payload;
- provider signature/secret;
- unmasked provider reference;
- `created_at` khi application result hiện chưa cung cấp field này.

Errors chính:

- malformed `paymentId` → `400 PAYMENT_INVALID_INPUT`;
- payment không tồn tại → `404 PAYMENT_NOT_FOUND`;
- chưa xác thực → `401 PAYMENT_UNAUTHENTICATED`;
- không đủ quyền → `403 PAYMENT_FORBIDDEN`.

### 3.3 `POST /payments/webhook`

Không JWT.

Request runtime hiện tại:

```json
{
  "provider_event_id": "vnpay-event-001",
  "provider_transaction_ref": "vnpay-txn-001",
  "response_code": "00",
  "transaction_status": "00",
  "amount": 1094000,
  "currency": "VND",
  "payload_hash": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "signed_payload": {
    "vnp_TxnRef": "vnpay-txn-001",
    "vnp_Amount": "109400000",
    "vnp_ResponseCode": "00",
    "vnp_TransactionStatus": "00",
    "vnp_SecureHash": "<signature>"
  }
}
```

Response `200 OK`:

```json
{
  "data": {
    "payment_id": "01912f95-7a1b-7c12-9c55-8b1c34a6d921",
    "payment_status": "SUCCESS",
    "action": "APPLIED"
  },
  "meta": {
    "request_id": "01912fa7-7a1b-7c12-9c55-8b1c34a6d921"
  }
}
```

Duplicate event trả `action: "DUPLICATE"` và không tạo financial side effect lần hai.

Quy tắc:

- verify VNPAY signature;
- dedupe `provider_event_id`;
- tra payment qua payment attempt/provider transaction ref;
- webhook amount/currency phải khớp intent nội bộ;
- success hợp lệ cập nhật payment, allocation, ledger, wallet, provider event và outbox trong transaction;
- duplicate callback không ghi ledger/allocation/outbox lần hai;
- không expose hoặc log raw secret/signature.

Known open item:

- authoritative VNPAY IP allowlist policy chưa được chốt đầy đủ; không tự hard-code policy production nếu chưa có nguồn chính thức.

Errors chính:

- signature sai → `400 INVALID_VNPAY_SIGNATURE`;
- payment attempt không tồn tại → `404 PAYMENT_ATTEMPT_NOT_FOUND`;
- amount mismatch → `409 PAYMENT_AMOUNT_MISMATCH`.

### 3.4 Refund

`POST /payments/{paymentId}/refunds`

Headers bắt buộc:

- `Idempotency-Key`
- `X-Request-ID`

Authorized callers:

- trusted Order-Commerce internal caller;
- authenticated actor có role `FINANCE_OPS`.

Buyer và seller client không gọi trực tiếp endpoint refund này.

Request:

```json
{
  "amount": 50000,
  "reason": "BUYER_CANCELLED"
}
```

Ràng buộc:

- `amount > 0`;
- `reason` bắt buộc, tối đa 500 ký tự;
- HTTP caller không truyền `currency`; refund hiện dùng `VND`;
- contract runtime hiện tại là payment-level refund, không nhận `order_id`;
- tổng `refunded + pending refund + requested refund` không vượt `captured_amount`;
- payment phải ở state cho phép refund.

Response `202 Accepted`:

```json
{
  "data": {
    "refund_id": "01912fa8-7a1b-7c12-9c55-8b1c34a6d921",
    "payment_id": "01912f95-7a1b-7c12-9c55-8b1c34a6d921",
    "amount": 50000,
    "currency": "VND",
    "status": "REQUESTED"
  },
  "meta": {
    "request_id": "01912fa9-7a1b-7c12-9c55-8b1c34a6d921"
  }
}
```

Tạo refund request không đồng nghĩa refund đã thành công.

State payment chỉ chuyển:

- `PARTIALLY_REFUNDED`, hoặc
- `REFUNDED`

sau khi refund-result workflow xác nhận thành công.

Idempotency behavior:

- cùng `Idempotency-Key` + cùng request → trả lại cùng refund result, không tạo refund/outbox thứ hai;
- cùng `Idempotency-Key` + payload khác → `409 PAYMENT_IDEMPOTENCY_CONFLICT`;
- request cùng key đang `PROCESSING` → `409 REQUEST_ALREADY_PROCESSING`.

Errors chính:

- payment không tồn tại → `404 PAYMENT_NOT_FOUND`;
- payment state không cho refund → `409 REFUND_STATE_INVALID`;
- refund vượt captured/pending limit → `409 REFUND_AMOUNT_INVALID`;
- chưa xác thực → `401 PAYMENT_UNAUTHENTICATED`;
- không đủ quyền → `403 PAYMENT_FORBIDDEN`.

### 3.5 Seller wallet/ledger/revenue

> Phase 7 target contract.

`GET /seller/wallet`:

```json
{
  "data": {
    "wallet_id": "wallet-01912fb5",
    "shop_id": "shop-01912f31",
    "available_balance": 12500000,
    "pending_balance": 3200000,
    "currency": "VND",
    "status": "ACTIVE",
    "as_of": "2026-08-31T04:00:00Z"
  },
  "meta": {
    "request_id": "01912fb6-7a1b-7c12-9c55-8b1c34a6d921"
  }
}
```

`pending_balance` là tiền đã ghi nhận nhưng chưa qua settlement. `status` ∈ `ACTIVE | FROZEN | CLOSED`.

`GET /seller/wallet/ledger?page=&size=&from=&to=&type=`:

```json
{
  "data": [
    {
      "entry_id": "entry-01912fb7",
      "posting_id": "posting-01912fb8",
      "entry_type": "CREDIT",
      "amount": 1062000,
      "balance_after": 12500000,
      "reference": {
        "type": "ORDER",
        "id": "order-01912f91"
      },
      "description": "Doanh thu đơn TC-20260830-0001",
      "created_at": "2026-08-30T09:03:12Z"
    }
  ],
  "meta": {
    "request_id": "01912fb9-7a1b-7c12-9c55-8b1c34a6d921",
    "page": 1,
    "size": 20,
    "total": 340,
    "total_pages": 17
  }
}
```

Ledger append-only, không có endpoint sửa/xóa.

`GET /seller/revenue?from=&to=&granularity=DAY|WEEK|MONTH`:

```json
{
  "data": {
    "range": {
      "from": "2026-08-01",
      "to": "2026-08-31",
      "granularity": "DAY"
    },
    "currency": "VND",
    "summary": {
      "gross": 125000000,
      "commission": 8750000,
      "tax": 1250000,
      "net": 115000000,
      "refunded": 2000000,
      "order_count": 340
    },
    "buckets": [
      {
        "period": "2026-08-01",
        "gross": 4200000,
        "commission": 294000,
        "tax": 42000,
        "net": 3864000,
        "order_count": 12
      }
    ]
  },
  "meta": {
    "request_id": "01912fc0-7a1b-7c12-9c55-8b1c34a6d921"
  }
}
```

`GET /seller/revenue/export?from=&to=&format=xlsx|csv`:

```json
{
  "data": {
    "export_url": "https://storage.example/signed-download/revenue-shop-01912f31-202608.xlsx",
    "format": "xlsx",
    "row_count": 31,
    "generated_at": "2026-08-31T04:00:00Z",
    "expires_at": "2026-08-31T04:30:00Z"
  },
  "meta": {
    "request_id": "01912fc3-7a1b-7c12-9c55-8b1c34a6d921"
  }
}
```

### 3.6 `POST /seller/payouts`

> Phase 7 target contract.

Header:

- `Idempotency-Key`
- `X-MFA-Step-Up`

Body:

```json
{
  "amount": 5000000,
  "bank_account_id": "bank_account-01912fc0",
  "reason": "Rút doanh thu tháng 8"
}
```

Response `202`:

```json
{
  "data": {
    "payout_id": "payout-01912fc1",
    "shop_id": "shop-01912f31",
    "amount": 5000000,
    "currency": "VND",
    "status": "REQUESTED",
    "bank_account_masked": "VCB ****3021",
    "requested_at": "2026-08-31T04:10:00Z"
  },
  "meta": {
    "request_id": "01912fc2-7a1b-7c12-9c55-8b1c34a6d921"
  }
}
```

Bank-account authoritative source vẫn là open contract theo ADR-003.

### 3.7 Reconciliation/health

> Reconciliation thuộc Phase 8; health endpoints đã có runtime foundation.

`GET /admin/payments/reconciliation`:

```json
{
  "data": [
    {
      "payment_id": "payment-01912fa1",
      "provider": "VNPAY",
      "local_status": "SUCCESS",
      "provider_status": "success",
      "amount": 1094000,
      "match": true,
      "checked_at": "2026-08-30T09:10:00Z"
    }
  ],
  "meta": {
    "request_id": "01912fa3-7a1b-7c12-9c55-8b1c34a6d921",
    "page": 1,
    "size": 20,
    "total": 1,
    "total_pages": 1,
    "mismatch_count": 0
  }
}
```

`match:false` chỉ phục vụ điều tra, không tự sửa financial state.

Health:

- `/health/live`: liveness;
- `/health/ready`: readiness gồm DB/Kafka/outbox/VNPAY readiness theo cấu hình runtime.

### 3.8 Admin finance back-office (`FINANCE_OPS`)

> Phase 8 target contract.

Các nhóm API mục tiêu:

- reconciliation;
- fee configuration;
- tax configuration;
- settlement batch read/retry;
- finance summary/export.

Fee/Tax config là effective-dated và append-only.

Settlement không được production-wire chỉ dựa vào việc allocation chưa xuất hiện trong `settlement_lines`. Authoritative settlement eligibility vẫn OPEN theo ADR-003.

## 4. Mã lỗi

### 4.1 Error codes đang được Phase 6 runtime sử dụng

| Mã | HTTP | Ý nghĩa |
|---|---:|---|
| `PAYMENT_INVALID_INPUT` | 400 | Payment/refund path/header input không hợp lệ |
| `VALIDATION_ERROR` | 400 | Bean Validation thất bại |
| `MISSING_REQUIRED_HEADER` | 400 | Thiếu required header |
| `MALFORMED_REQUEST_BODY` | 400 | JSON/request body malformed |
| `INVALID_REQUEST` | 400 | Illegal argument/request fallback |
| `INVALID_VNPAY_SIGNATURE` | 400 | VNPAY signature không hợp lệ |
| `PAYMENT_UNAUTHENTICATED` | 401 | Không có actor/internal caller hợp lệ |
| `AUTH_MFA_REQUIRED` | 401 | Yêu cầu MFA step-up nhưng chưa có |
| `PAYMENT_FORBIDDEN` | 403 | Caller đã xác thực nhưng không đủ quyền |
| `PAYMENT_NOT_FOUND` | 404 | Không tìm thấy payment |
| `PAYMENT_ATTEMPT_NOT_FOUND` | 404 | Không tìm thấy payment attempt cho provider transaction |
| `REFUND_NOT_FOUND` | 404 | Không tìm thấy refund |
| `PAYMENT_AMOUNT_MISMATCH` | 409 | Provider/payment amount mismatch |
| `PAYMENT_IDEMPOTENCY_CONFLICT` | 409 | Idempotency key được reuse với request khác |
| `REQUEST_ALREADY_PROCESSING` | 409 | Idempotent request cùng key đang xử lý |
| `REFUND_AMOUNT_INVALID` | 409 | Refund vượt captured/pending limit |
| `REFUND_STATE_INVALID` | 409 | Payment state hiện tại không cho phép refund |
| `REFUND_AMOUNT_MISMATCH` | 409 | Refund result amount không khớp refund đã ghi |
| `INTERNAL_ERROR` | 500 | Unexpected server error; không expose internal detail |

### 4.2 Error codes của các phase sau

| Mã | HTTP | Ý nghĩa |
|---|---:|---|
| `WALLET_INSUFFICIENT_BALANCE` | 409 | Không đủ available balance |
| `WALLET_FROZEN` | 403 | Wallet frozen |
| `PAYOUT_NOT_ALLOWED` | 403/409 | Payout không thỏa policy |
| `FEE_CONFIG_INVALID` | 409 | Fee/tax config không hợp lệ |
| `SETTLEMENT_BATCH_INVALID_STATE` | 409 | Settlement action không hợp lệ với state |

## 5. Known contract debt & open items

| # | Nội dung | Trạng thái / ảnh hưởng |
|---:|---|---|
| 1 | Create-payment canonical Order snapshot | ADR-002 đã Accepted nhưng runtime vẫn dùng `orders[]`; đây là implementation debt. |
| 2 | Internal service-auth transport | `InternalCallerPolicy` đã có nhưng production transport để populate `InternalCallerContext` chưa được chốt; không fabricate service header. |
| 3 | VNPAY IP policy | Signature verification đã có; authoritative IP allowlist policy vẫn cần nguồn chính thức. |
| 4 | Seller payment projection | Generic `GET /payments/{paymentId}` không trả seller projection; Phase 7 sẽ xử lý seller finance riêng. |
| 5 | Provider refund transport/worker | Refund request HTTP + persistence/idempotency/outbox đã có; authoritative provider refund transport vẫn là contract riêng. |
| 6 | Buyer notification recipient | `buyer.email` chưa có authoritative source theo ADR-003; không fabricate email. |
| 7 | Seller payout gate projection | KYC/shop-status projection chưa được chốt. |
| 8 | Bank account source | Ownership/source của `bank_account_id` chưa được chốt. |
| 9 | Settlement trigger/eligibility | Hold window, eligible timestamp và upstream trigger vẫn OPEN; không production-wire `RunSettlementUseCase` bằng dữ liệu chưa authoritative. |
| 10 | Commission/tax business policy | Rate/version/rounding cần tiếp tục tuân theo accepted finance configuration contract ở các phase sau. |

## 6. Phase 6 closeout

Phase 6 hoàn thiện phạm vi Payment + Refund APIs:

- create payment security boundary;
- create payment HTTP hardening;
- payment detail query + endpoint;
- payment visibility authorization;
- refund HTTP DTO/mapping;
- refund request endpoint;
- refund caller authorization;
- refund error contract;
- HTTP integration/E2E coverage;
- regression + architecture validation.

Các open items ở mục 5 không được coi là đã giải quyết chỉ vì Phase 6 đóng. Chúng phải tiếp tục được xử lý bằng ADR hoặc upstream authoritative contract tương ứng.
