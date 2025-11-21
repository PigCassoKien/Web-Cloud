# SmartQueue - Hệ thống xếp hàng thông minh

## 🌟 Tổng quan

SmartQueue là hệ thống xếp hàng thông minh được thiết kế theo kiến trúc microservices, triển khai 100% trên AWS với mục tiêu:

- Quản lý nhiều hàng đợi theo thời gian thực
- Dự đoán chính xác thời gian chờ (ETA) bằng thuật toán EMA
- Tự động thông báo khách hàng qua email (SES) khi gần đến lượt
- Thu thập thống kê thời gian phục vụ để tối ưu hệ thống quản lý.
- Hỗ trợ multi-queue cho nhiều chi nhánh.
- Giảm thiểu chi phí vận hành, chịu tải được nhiều dữ liệu đồng thời.

Dự án được xây dựng hoàn toàn bằng các công nghệ hiện đại, dễ mở rộng và phù hợp làm bài tập lớn môn Cloud Computing.

### 🏗️ Kiến trúc hệ thống
![alt text](<sơ đồ.jpg>)
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
│   └── src/main/java/
├── service-eta-aws/          # Spring Boot Service B
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/
├── infra/
│   ├── aws/                     # Terraform AWS
└── tools/
    └── k6/                      # Load testing scripts
```
## Chức năng từng Service
### 1. Smartqueue-queue-service (Port 8081)

- Quản lý hàng đợi theo thời gian thực và phát hành ticket
- Hỗ trợ tham gia hàng đợi (POST /api/queues/{id}/join)
- Kiểm tra trạng thái & vị trí trong hàng đợi (GET /api/queues/{id}/status)
- Gọi khách tiếp theo (admin): POST /api/queues/{id}/next
- Cập nhật thống kê phục vụ (gọi từ ETA service)

### 2. Smartqueue-eta-service (Port 8082)

- Tính toán ETA thông minh dựa trên EMA (Exponential Moving Average)
- Lưu trữ lịch sử phục vụ trong DynamoDB
- Scheduler tự động kiểm tra và gửi thông báo khi khách gần đến lượt
- Gửi email thông báo qua AWS SES (Simple Email Service)


## Các công nghệ được sử dụng
### Backend:
  - Java 17, Spring Boot 3, Spring WebFlux, Spring Data DynamoDB
### Frontend:
  - React 18, Vite, TypeScript, Ant Design, Tanstack Query.
### Database: 
  - Amazon DynamoDB (NoSQL)
### Infrastructure:
  - AWS ECR, EC2, IAM, VPC,...
### Reverse Proxy:
  - Nginx(static + proxy API)
### Monitoring:
  - Spring Boot Actuator + Prometheus endpoint

## Các dịch vụ AWS:
#### EC2 (t3.small):
  - Chạy docker containers + Nginx
  - Thiết lập Application Load Balancer: Path-based routing & terminate HTTP.
#### ECR: Lưu trữ Docker images
#### DynamoDB: Lưu trữ dữ liệu người dùng, tickets, queues, notifications và thống kê phục vụ,...
#### SES: Gửi email thông báo
#### IAM: Role cho EC2 truy cập DynamoDB & SES, phân quyền và users
#### VCP + Subnets + IGW: Thiết lập mạng riêng cho application

## Triển khai dự án

#### Backend Services

```bash

git clone https://github.com/PigCassoKien/Web-Cloud
cd smartqueue

# Service A - AWS Queue Manager
cd service-queue-aws
mvn spring-boot:run

# Service B - AWS ETA & Notification  
cd service-eta-aws
mvn spring-boot:run

```

#### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Build và chạy với Docker

```bash
# Build tất cả services
docker build -t smartqueue-aws ./service-queue-aws
docker build -t smartqueue-eta ./service-eta-aws
docker build -t smartqueue-frontend ./frontend

# Chạy với docker-compose (tạo file docker-compose.yml)
docker-compose up -d
```

## 🔧 Cấu hình môi trường

### Biến môi trường chung

```yaml
# AWS Core
AWS_REGION=ap-southeast-1
AWS_DEFAULT_REGION=ap-southeast-1

#DynamoDB Enpoint
DYNAMODB_ENDPOINT=${DYNAMODB_ENDPOINT:}              

# Spring profile
SPRING_PROFILES_ACTIVE=prod
```
### Service A - AWS Queue Manager

```yaml
# Table names (auto-resolved with prefix)
aws.dynamodb.tickets-table=${DDB_TABLE_TICKETS:tickets}
aws.dynamodb.queues-table=${DDB_TABLE_QUEUES:queues}
aws.dynamodb.users-table=${DDB_TABLE_USERS:smartqueue-users}

# ETA Service Integration
service.eta.base-url=${ETA_SERVICE_URL:http://smartqueue-eta-service:8082}
```

### Service B - AWS ETA & Notification

```yaml
# Table names (auto-resolved with prefix)
aws.dynamodb.table-prefix=smartqueue-${spring.profiles.active}-
# SES
SES_FROM_EMAIL=kien0610minh@gmail.com
SES_FROM_NAME=SmartQueue System

# ETA calculation
ETA_EMA_ALPHA=0.3
ETA_WINDOW_SIZE=60
DEFAULT_SERVICE_RATE=1.0
ETA_THRESHOLD=10
```

## ☁️ Triển khai Cloud
#### Build và Push Images lên ECR
```bash
# Queue Service
cd smartqueue-queue-service
aws ecr get-login-password --region ap-southeast-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com
docker push <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com/smartqueue-queue-service:latest
docker push <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com/smartqueue-eta-service:latest
docker push <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com/smartqueue-frontend:latest

# ETA Service
cd smartqueue-eta-service
aws ecr get-login-password --region ap-southeast-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com
docker push <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com/smartqueue-queue-service:latest
docker push <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com/smartqueue-eta-service:latest
docker push <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com/smartqueue-frontend:latest
```

## 📊 Monitoring & Observability

### Health Checks

```bash
# Service A
curl http://localhost:8081/actuator/health

# Service B  
curl http://localhost:8082/actuator/health

# Frontend
curl http://localhost:3000/health
```

### Metrics Endpoints

```bash
# Prometheus metrics
curl http://localhost:8081/actuator/prometheus
curl http://localhost:8082/actuator/prometheus
```

## 🔧 Development Guide

### Thêm feature mới

1. **Backend**: Tạo controller, service, repository
2. **Frontend**: Tạo component, page, API call



## 👥 Team

- **Backend**: Java Spring Boot, AWS SDK, Aliyun SDK
- **Frontend**: React, TypeScript, Ant Design  
- **Infrastructure**: Terraform, AWS, Aliyun
- **Testing**: k6, JUnit, TestContainers
- **CI/CD**: GitHub Actions, Docker


## 📝 License

Dự án này được phát triển cho mục đích học tập - môn Cloud Computing.