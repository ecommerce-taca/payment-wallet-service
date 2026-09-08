# ADR-001 — Payment-Wallet Domain Decisions

- Status: Accepted
- Date: 2026-09-08
- Service: payment-wallet-service

## 1. Context

Payment-Wallet Service chịu trách nhiệm quản lý:

- Payment intent và payment transaction.
- VNPAY và COD.
- Payment allocation cho từng shop.
- Seller wallet.
- Double-entry ledger.
- Refund.
- Payout.
- Settlement.
- Transactional outbox.

Service được xây dựng theo DDD kết hợp Hexagonal Architecture.

## 2. Decisions

### 2.1 Payment scope

Một `Payment` đại diện cho một `checkout_group`.

Một `checkout_group` có thể chứa nhiều order con thuộc nhiều shop.

Quan hệ:

- Một payment có nhiều payment orders.
- Một payment có nhiều payment attempts.
- Một payment có nhiều payment allocations.
- Tại một thời điểm chỉ có tối đa một attempt đang PENDING.

### 2.2 Payment method

Payment method hỗ trợ:

- VNPAY
- COD

VNPAY sử dụng webhook để xác nhận kết quả.

COD được xác nhận thành công sau khi nhận sự kiện giao hàng thành công.

### 2.3 COD cancellation

Không bổ sung PaymentStatus.CANCELLED.

Đơn COD bị hủy trước khi giao được lưu:

- status = FAILED
- failure_code = ORDER_CANCELLED

### 2.4 Payment status

PaymentStatus gồm:

- PENDING
- PENDING_COD
- SUCCESS
- FAILED
- EXPIRED
- PARTIALLY_REFUNDED
- REFUNDED

### 2.5 Webhook

Giữ nguyên webhook:

POST /api/v1/payments/webhook

Webhook phải:

- Verify signature.
- Kiểm tra payment/order/amount.
- Chống xử lý trùng.
- Không tạo ledger hai lần.
- Cập nhật payment, ledger và outbox trong cùng transaction.

### 2.6 Wallet and ledger

Ledger sử dụng double-entry accounting.

Ledger được thiết kế theo:

- Ledger account.
- Ledger posting.
- Ledger entry.

Wallet balance là projection của ledger.

Không cập nhật wallet balance nếu không có ledger posting tương ứng.

### 2.7 Refund

Refund không được vượt quá captured amount.

Tổng tiền cần kiểm tra bao gồm:

- REQUESTED refunds.
- PROCESSING refunds.
- SUCCESS refunds.
- Refund mới.

Refund được phân bổ về từng payment allocation.

### 2.8 Settlement

Mỗi payment allocation chỉ được settlement một lần.

Settlement retry không được tạo thêm tiền hoặc tạo ledger posting trùng.

### 2.9 Idempotency

Idempotency key được scope theo aggregate:

- Payment: checkout_group_id + idempotency_key.
- Refund: payment_id + idempotency_key.
- Payout: shop_id + idempotency_key.
- Webhook: provider + provider_event_id.

### 2.10 Fee and tax

Phiên bản v1 chỉ áp dụng:

- scope = PLATFORM.

CATEGORY scope được giữ trong thiết kế nhưng chưa triển khai tính toán ở v1.

### 2.11 Cross-service references

Không tạo foreign key tới database của:

- Order-Commerce.
- Auth-User.
- Shipment.
- Product-Catalog.

Các ID bên ngoài chỉ được lưu dưới dạng reference.

## 3. Consequences

Ưu điểm:

- Hỗ trợ checkout nhiều shop.
- Có thể retry thanh toán.
- Chống double payment và double ledger.
- Có khả năng thay thế provider.
- Dễ mở rộng refund, settlement và reconciliation.

Đánh đổi:

- Số lượng bảng nhiều hơn.
- Cần mapper giữa Domain và Persistence.
- Cần transaction và concurrency control chặt chẽ.