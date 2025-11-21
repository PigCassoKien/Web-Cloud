# SmartQueue - Hệ thống xếp hàng thông minh đa đám mây

## 🌟 Tổng quan

SmartQueue là hệ thống xếp hàng thông minh được thiết kế theo kiến trúc đa đám mây (Multi-cloud), kết hợp AWS và Alibaba Cloud để cung cấp dịch vụ xếp hàng có khả năng mở rộng cao và dự đoán thời gian chờ thông minh.

### 🏗️ Kiến trúc hệ thống

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │  Service A      │    │  Service B      │
│   React/Vite    │    │  (AWS)          │    │  (Aliyun)       │
│                 │    │ Queue Manager   │    │ ETA & Notifier  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
    ┌─────────┐            ┌─────────┐            ┌─────────┐
    │AWS S3 + │            │DynamoDB │            │TableStore│
    │CloudFront│            │API GW   │            │MNS Queue │
    │         │            │Lambda   │            │Function │
    │Aliyun   │            │         │            │Compute  │
    │OSS + CDN│            │         │            │         │
    └─────────┘            └─────────┘            └─────────┘
```

## 📁 Cấu trúc dự án

```
smartqueue/
├── README.md
├── frontend/                    # React/Vite SPA
│   ├── package.json
│   ├── vite.config.ts
│   ├── Dockerfile
│   └── src/
├── service-queue-aws/           # Spring Boot Service A
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/digimarket/
├── service-eta-aliyun/          # Spring Boot Service B
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/digimarket/
├── infra/
│   ├── aws/                     # Terraform AWS
│   └── aliyun/                  # Terraform Aliyun
└── tools/
    └── k6/                      # Load testing scripts
```

## 🚀 Bắt đầu nhanh

### 1. Prerequisites

- Java 18+
- Node.js 18+
- Docker
- Terraform 1.0+
- k6 (cho load testing)

### 2. Chạy local development

#### Backend Services

```bash
# Service A - AWS Queue Manager
cd service-queue-aws
mvn spring-boot:run

# Service B - Aliyun ETA & Notification  
cd service-eta-aliyun
mvn spring-boot:run
```

#### Frontend

```bash
cd frontend
npm install
npm run dev
```

### 3. Build và chạy với Docker

```bash
# Build tất cả services
docker build -t smartqueue-aws ./service-queue-aws
docker build -t smartqueue-aliyun ./service-eta-aliyun
docker build -t smartqueue-frontend ./frontend

# Chạy với docker-compose (tạo file docker-compose.yml)
docker-compose up -d
```

## 🔧 Cấu hình môi trường

### Service A - AWS Queue Manager

```yaml
# application.properties
aws:
  region: ap-southeast-1
  dynamodb:
    tickets-table: smartqueue-tickets
    queues-table: smartqueue-queues
service:
  eta:
    base-url: http://service-b:8081
```

### Service B - Aliyun ETA & Notification

```yaml
# application.properties
aliyun:
  region: ap-southeast-1
  access-key-id: ${ALIYUN_AK}
  access-key-secret: ${ALIYUN_SK}
  ots:
    endpoint: ${OTS_ENDPOINT}
    instance: smartqueue-ots
  mns:
    endpoint: ${MNS_ENDPOINT}
    queue-name: smartqueue-notifications
```

## 📡 API Documentation

### Service A - Queue Manager (Port 8080)

#### Tham gia hàng đợi
```http
POST /queues/{queueId}/join
Content-Type: application/json

{
  "email": "user@example.com",
  "phone": "+84901234567",
  "userName": "John Doe"
}
```

#### Kiểm tra trạng thái
```http
GET /queues/{queueId}/status?ticketId={ticketId}
```

#### Xử lý khách hàng tiếp theo (Admin)
```http
POST /queues/{queueId}/next
Content-Type: application/json

{
  "count": 1
}
```

#### Load Testing endpoint
```http
POST /queues/test/join-bulk
X-Test-Key: LOADTEST-SECRET-KEY
Content-Type: application/json

{
  "queueId": "test-queue",
  "batch": 10
}
```

### Service B - ETA & Notification (Port 8081)

#### Tính toán ETA
```http
GET /eta?queueId={queueId}&ticketId={ticketId}&position={position}
```

#### Gửi thông báo
```http
POST /notify
Content-Type: application/json

{
  "ticketId": "ticket-123",
  "channel": "EMAIL",
  "address": "user@example.com",
  "message": "Your turn is coming up!"
}
```

#### Cập nhật thống kê
```http
POST /stats/served
Content-Type: application/json

{
  "queueId": "queue-123",
  "count": 5,
  "windowSec": 60
}
```

## 🧪 Load Testing với k6

### Chạy Spike Test
```bash
cd tools/k6
export API_AWS_BASE="http://localhost:8080"
export API_ALIYUN_BASE="http://localhost:8081"
export TEST_KEY="LOADTEST-SECRET-KEY"

k6 run join_spike.js
```

### Chạy Soak Test
```bash
k6 run soak_test.js
```

### Chạy tất cả tests
```bash
chmod +x run_load_tests.sh
./run_load_tests.sh local
```

**Mục tiêu hiệu năng:**
- P90 < 300ms cho API join/status
- P95 < 500ms 
- Error rate < 1%
- Chịu được 5,000 concurrent users

## ☁️ Triển khai Cloud

### 1. AWS Infrastructure

```bash
cd infra/aws
terraform init
terraform plan
terraform apply
```

**Tài nguyên được tạo:**
- DynamoDB tables (tickets, queues)
- S3 bucket + CloudFront cho frontend
- API Gateway + Lambda cho backend
- CloudWatch logs & metrics

### 2. Aliyun Infrastructure

```bash
cd infra/aliyun
terraform init
terraform plan
terraform apply
```

**Tài nguyên được tạo:**
- TableStore (OTS) cho ETA stats
- Message Queue (MNS) cho notifications
- OSS + CDN cho frontend
- Function Compute cho backend
- Log Service cho monitoring

### 3. Container Registry & Deployment

```bash
# AWS ECR
aws ecr create-repository --repository-name smartqueue-aws
docker tag smartqueue-aws:latest {account}.dkr.ecr.ap-southeast-1.amazonaws.com/smartqueue-aws:latest
docker push {account}.dkr.ecr.ap-southeast-1.amazonaws.com/smartqueue-aws:latest

# Aliyun ACR
docker tag smartqueue-aliyun:latest registry.ap-southeast-1.aliyuncs.com/{namespace}/smartqueue-aliyun:latest
docker push registry.ap-southeast-1.aliyuncs.com/{namespace}/smartqueue-aliyun:latest
```

## 📊 Monitoring & Observability

### Health Checks

```bash
# Service A
curl http://localhost:8080/actuator/health

# Service B  
curl http://localhost:8081/actuator/health

# Frontend
curl http://localhost:3000/health
```

### Metrics Endpoints

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8081/actuator/prometheus
```

## 💰 Chi phí ước tính

**AWS (Service A):**
- Lambda: ~$0 (free tier)
- DynamoDB: ~$0 (free tier) 
- API Gateway: ~$3.5/million requests
- CloudFront: ~$0.085/GB

**Aliyun (Service B):**
- Function Compute: ~$0 (free tier)
- TableStore: ~$0.0043/GB
- Message Queue: ~$2/million operations
- OSS: ~$0.02/GB

**Tổng chi phí demo: < $10/month**

## 🔧 Development Guide

### Thêm feature mới

1. **Backend**: Tạo controller, service, repository
2. **Frontend**: Tạo component, page, API call
3. **Test**: Viết unit test, integration test
4. **Load test**: Thêm k6 scenario nếu cần

### Code Structure

```java
// Service Layer Pattern
@Service
public class QueueService {
    private final TicketRepository ticketRepository;
    private final QueueRepository queueRepository;
    
    public JoinQueueResponse joinQueue(String queueId, JoinQueueRequest request) {
        // Business logic
    }
}

// Repository Pattern với DynamoDB
@Repository 
public class TicketRepository {
    private final DynamoDbEnhancedClient dynamoDbClient;
    
    public Ticket save(Ticket ticket) {
        // Data access logic
    }
}
```

### Error Handling

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.builder()
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build());
    }
}
```

## 🤝 Contributing

1. Fork dự án
2. Tạo feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Tạo Pull Request

## 📝 License

Dự án này được phát triển cho mục đích học tập - môn Cloud Computing.

## 👥 Team

- **Backend**: Java Spring Boot, AWS SDK, Aliyun SDK
- **Frontend**: React, TypeScript, Ant Design  
- **Infrastructure**: Terraform, AWS, Aliyun
- **Testing**: k6, JUnit, TestContainers
- **CI/CD**: GitHub Actions, Docker

## 🔗 Links hữu ích

- [AWS SDK for Java](https://aws.amazon.com/sdk-for-java/)
- [Aliyun Java SDK](https://help.aliyun.com/product/29991.html)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [k6 Load Testing](https://k6.io/docs/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest)

---

**🎯 Mục tiêu học tập:** Hiểu và thực hành kiến trúc đa đám mây, microservices, load testing, và infrastructure as code.