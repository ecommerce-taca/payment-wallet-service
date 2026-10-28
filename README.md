# Payment Wallet Service

`payment-wallet-service` là microservice quản lý thanh toán và ví cho hệ thống thương mại điện tử. Service xử lý payment VNPAY/COD, refund, sổ cái ví seller, cấu hình phí/thuế, settlement, sự kiện Kafka và các API tài chính nội bộ.

Ứng dụng được xây bằng Java 25, Spring Boot 4, MySQL 8.4, Kafka và Flyway. Source code tổ chức theo DDD và Hexagonal Architecture (`presentation` → `application` → `domain`, với `infrastructure` triển khai các outbound port).

## Chức năng hiện có

- Tạo, truy vấn payment và nhận webhook VNPAY.
- Refund có idempotency.
- Ví seller, ledger và doanh thu seller.
- Back-office Finance Ops: reconciliation, fee/tax configuration, settlement batch và finance summary/export.
- Inbox/outbox, Kafka event publishing/consuming và Flyway migrations.
- Health/readiness, Prometheus metrics, W3C trace context và structured logging.

Một số hợp đồng liên dịch vụ như tạo payout mới, seller revenue export và tạo settlement batch vẫn được chủ động gate, chưa public runtime. Xem chi tiết tại [API contract](docs/api/payment-wallet-api.md) và các ADR trong [`docs/adr`](docs/adr).

## Yêu cầu môi trường

- JDK 25 (khi chạy Maven trên máy).
- Docker Desktop / Docker Engine có Docker Compose v2.
- Docker Hub account nếu build và push image.

Maven Wrapper đã có sẵn, nên không cần cài Maven global.

## Chạy local

Khởi động MySQL và Kafka:

```powershell
docker compose up -d
```

`docker-compose.yml` chỉ cung cấp infrastructure. Ứng dụng chạy từ máy host cần trỏ Kafka đến port `29092` (Compose hiện publish port này), đồng thời bật topic provisioning để tạo các topic bắt buộc trong môi trường local:

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:29092"
$env:KAFKA_TOPIC_PROVISIONING_GUARD_ENABLED = "true"
./mvnw.cmd spring-boot:run
```

Trên macOS/Linux:

```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:29092 \
KAFKA_TOPIC_PROVISIONING_GUARD_ENABLED=true \
./mvnw spring-boot:run
```

Local profile mặc định dùng MySQL `localhost:15405/paymentdb`, user `payment_user`, password `payment_password`. Flyway tự chạy migration khi app khởi động.

## Kiểm tra service

Service lắng nghe trên port `8086`.

```powershell
curl.exe http://localhost:8086/health/live
curl.exe http://localhost:8086/health/ready
curl.exe http://localhost:8086/prometheus
```

`/health/live` chỉ phản ánh tiến trình ứng dụng. `/health/ready` yêu cầu MySQL, Kafka, các Kafka topic bắt buộc, outbox và VNPAY configuration sẵn sàng; vì vậy có thể trả `503` nếu thiếu biến VNPAY hoặc Kafka topic. Không public các Actuator endpoint nhạy cảm như `/env`, `/beans` hay `/heapdump`.

## Biến môi trường runtime

| Biến | Mục đích | Giá trị local/container mẫu |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL MySQL | `jdbc:mysql://mysql:15405/paymentdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `SPRING_DATASOURCE_USERNAME` | MySQL user | `payment_user` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL password | `payment_password` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap server | `kafka:9092` |
| `KAFKA_TOPIC_PROVISIONING_GUARD_ENABLED` | Tạo topic khi môi trường cho phép | `true` ở local/dev |
| `KAFKA_CONSUMER_GROUP_ID` | Kafka consumer group | `payment-wallet-service` |
| `VNPAY_TMN_CODE` | VNPAY terminal code | secret |
| `VNPAY_HASH_SECRET` | VNPAY signing secret | secret |
| `VNPAY_PAYMENT_URL` | VNPAY endpoint | sandbox/production URL |
| `VNPAY_RETURN_URL` | URL trả về cho client | URL frontend của môi trường |

Không commit VNPAY secret, mật khẩu database hoặc token Docker Hub vào repository. Dùng secret manager/CI secret ở môi trường deploy.

## Test và build JAR

```powershell
./mvnw.cmd test
./mvnw.cmd clean package
```

Test integration dùng Testcontainers, vì vậy Docker phải đang chạy. File JAR đầu ra nằm tại `target/payment-wallet-service-0.0.1-SNAPSHOT.jar`.

## Build và chạy Docker image

`Dockerfile` là multi-stage build: stage đầu compile bằng Maven + JDK 25, stage sau chỉ chứa JRE 25 và JAR, chạy bằng user không phải root.

Tại thư mục gốc repository:

```powershell
docker build --tag payment-wallet-service:local .
```

Chạy image vừa build cùng các dependency trong Compose. Các biến `SPRING_DATASOURCE_*` và `KAFKA_BOOTSTRAP_SERVERS` là bắt buộc khi app chạy trong container, vì `localhost` lúc này là chính container app.

```powershell
docker run --rm --name payment-wallet-service `
  --network payment-wallet-service_default `
  -p 8086:8086 `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://mysql:15405/paymentdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" `
  -e SPRING_DATASOURCE_USERNAME=payment_user `
  -e SPRING_DATASOURCE_PASSWORD=payment_password `
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 `
  -e KAFKA_TOPIC_PROVISIONING_GUARD_ENABLED=true `
  -e VNPAY_TMN_CODE=your_tmn_code `
  -e VNPAY_HASH_SECRET=your_hash_secret `
  -e VNPAY_RETURN_URL=http://localhost:3000/payment/vnpay-return `
  payment-wallet-service:local
```

Tên network Compose mặc định lấy theo tên thư mục project. Nếu bạn chạy Compose với project name khác, kiểm tra bằng `docker network ls` rồi thay giá trị `--network` tương ứng. Sau khi startup, kiểm tra `http://localhost:8086/health/live`.

## Build và push lên Docker Hub

Quy ước khuyến nghị: không push duy nhất tag `latest`. Mỗi image cần một tag version hoặc commit SHA bất biến; có thể gắn thêm `latest` cho nhánh release.

1. Tạo repository `payment-wallet-service` trên Docker Hub dưới namespace của team, ví dụ `your-dockerhub-org/payment-wallet-service`.
2. Đăng nhập Docker Hub. Nếu tài khoản bật 2FA, dùng Personal Access Token thay mật khẩu.

```powershell
docker login
```

3. Chọn version. Ví dụ phát hành `1.0.0`:

```powershell
$IMAGE = "your-dockerhub-org/payment-wallet-service"
$VERSION = "1.0.0"
docker build --pull --tag "${IMAGE}:${VERSION}" --tag "${IMAGE}:latest" .
```

4. Kiểm tra image trước khi publish:

```powershell
docker image inspect "${IMAGE}:${VERSION}"
docker run --rm --entrypoint java "${IMAGE}:${VERSION}" -version
```

5. Push cả tag version và `latest` (nếu team dùng tag này):

```powershell
docker push "${IMAGE}:${VERSION}"
docker push "${IMAGE}:latest"
```

6. Thành viên khác pull và chạy đúng bản phát hành:

```powershell
docker pull your-dockerhub-org/payment-wallet-service:1.0.0
```

Để phát hành theo commit thay vì version thủ công:

```powershell
$IMAGE = "your-dockerhub-org/payment-wallet-service"
$VERSION = (git rev-parse --short HEAD)
docker build --pull -t "${IMAGE}:${VERSION}" .
docker push "${IMAGE}:${VERSION}"
```

Chỉ người có quyền write repository Docker Hub mới push được. CI nên lấy Docker Hub access token từ secret, build tag theo commit SHA và chỉ gắn `latest` từ branch release đã được duyệt.

## Tài liệu liên quan

- [API contract](docs/api/payment-wallet-api.md)
- [Database design](docs/db/payment-wallet-db.md)
- [Kiến trúc và kế hoạch](docs/payment-wallet-architecture-plan.md)
- [Observability contract](docs/operations/observability-contract.md)
- [Architecture decision records](docs/adr)
