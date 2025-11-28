# UrbanFlow 🚌

**Modern Urban Transport Management System - Microservices Architecture**

## 📋 Project Overview

UrbanFlow is a comprehensive urban transport management system built with a microservices architecture (SOA). The system enables:
- 🕒 Real-time bus schedule consultation
- 🎫 Online ticket purchasing
- 📍 Live bus tracking via GPS
- 📅 Subscription management
- 🔔 Real-time notifications for delays/cancellations


## 🏗️ Architecture

### Microservices
1. **User Management Service** - Authentication, registration, user profiles
2. **Ticket Purchase Service** - Ticket sales and management
3. **Route & Schedule Service** - Bus routes and timetables
4. **Geolocation Service** - Real-time bus tracking (GPS integration)
5. **Subscription Service** - Monthly/annual subscriptions
6. **Notification Service** - Email/SMS alerts
7. **API Gateway** - Centralized entry point

### Architecture Pattern
- **Database per Service** - Each microservice has its own PostgreSQL database
- **Event-Driven** - RabbitMQ for asynchronous communication
- **RESTful APIs** - Synchronous service-to-service communication

---

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot (Java)
- **API Documentation**: Swagger/OpenAPI
- **Security**: Spring Security + JWT
- **Validation**: Bean Validation (Hibernate Validator)

### Frontend
- **Framework**: React
- **Styling**: Tailwind CSS
- **State Management**: Redux/Context API
- **HTTP Client**: Axios

### Message Broker
- **RabbitMQ** - Event-driven communication between services

### Databases
- **PostgreSQL** - One database per microservice
- **JPA/Hibernate** - ORM for database operations

### DevOps & Infrastructure

#### Containerization & Orchestration
- **Docker** - Containerization for all microservices
- **Kubernetes (K8s)** - Container orchestration and management
  - Deployments with resource requests/limits
  - Services (LoadBalancer, ClusterIP)
  - ConfigMaps and Secrets management
  - Kustomize for manifest management

#### Infrastructure as Code (IaC)
- **Terraform** - Infrastructure provisioning and management
  - AWS Provider (v5.x)
  - ECR repositories management
  - VPC with public/private subnets
  - Multi-AZ deployment support

#### CI/CD Pipeline
- **GitHub Actions** - Automated CI/CD workflows
  - Terraform plan/apply for infrastructure
  - Multi-service Docker build and push
  - Automated deployment to EKS
  - Secret management via GitHub Secrets
- **AWS ECR** - Container registry for Docker images
- **Kustomize** - Kubernetes manifest management

#### Cloud Platform: AWS
- **EKS (Elastic Kubernetes Service)** - Managed Kubernetes cluster
- **ECR (Elastic Container Registry)** - Docker image storage
- **VPC** - Virtual Private Cloud with public/private subnets
- **EC2** - Compute instances for EKS nodes
- **CloudWatch** - Logging and monitoring

### Monitoring & Observability
- **Prometheus** - Metrics collection and time-series database
  - kube-prometheus-stack (Helm chart)
  - ServiceMonitor for application metrics
  - Node and pod metrics collection
- **Grafana** - Metrics visualization and dashboards
  - Pre-configured Kubernetes dashboards
  - Custom application dashboards
  - Alerting rules
- **Spring Boot Actuator** - Health checks and metrics endpoints
  - `/actuator/health` - Health checks
  - `/actuator/prometheus` - Prometheus metrics export
- **Kubernetes Resource Monitoring** - CPU, memory, and pod metrics

### Testing
- **Unit Testing**: JUnit 5, Mockito
- **Integration Testing**: Spring Boot Test, TestContainers
- **API Testing**: REST Assured

### API Gateway
- **Spring Cloud Gateway** - Request routing, load balancing, authentication

---

## 📁 Project Structure

```
UrbanFlow/
├── backend-services/        # Backend microservices
│   ├── api-gateway/         # API Gateway service
│   ├── user-service/        # User management microservice
│   ├── ticket-service/      # Ticket purchase microservice
│   ├── route-service/       # Routes & schedules microservice
│   ├── geolocation-service/ # Bus tracking microservice
│   └── payment-service/     # Payment processing microservice
├── transport-frontend/      # React frontend application
├── k8s/                     # Kubernetes manifests
│   ├── deployments/         # Deployment configurations
│   ├── services/            # Service definitions
│   ├── namespace.yaml       # Namespace configuration
│   ├── app-config.yaml      # ConfigMap for app configuration
│   ├── secret-db.yaml       # Database secrets (git-ignored)
│   └── kustomization.yaml   # Kustomize configuration
├── infra/                   # Infrastructure as Code
│   └── terraform/           # Terraform configurations
│       ├── main.tf          # Main Terraform configuration
│       ├── providers.tf    # Provider definitions
│       ├── variables.tf     # Variable definitions
│       ├── ecr.tf           # ECR repositories
│       ├── vpc.tf           # VPC and networking
│       └── outputs.tf       # Output values
├── monitoring/              # Monitoring configurations
│   ├── kube-prometheus-values.yaml  # Prometheus Helm values
│   └── README.md            # Monitoring setup guide
├── .github/                 # GitHub configurations
│   ├── workflows/           # GitHub Actions workflows
│   │   └── cicd-urbanflow.yml  # CI/CD pipeline
│   ├── SECRETS_AND_VARIABLES.md  # Secrets documentation
│   └── GITHUB_SECRETS_VARIABLES_VALUES.md  # Values reference
└── docs/                    # Project documentation
    ├── KUBERNETES_DEPLOYMENT.md  # K8s deployment guide
    └── DOCKER_SETUP.md      # Docker setup guide
```

---

## 🚀 Getting Started

### Prerequisites

#### Development
- **Java 17+** - Backend development
- **Node.js 18+** - Frontend development
- **Maven 3.6+** - Java build tool
- **Docker & Docker Compose** - Container runtime

#### DevOps & Deployment
- **kubectl** - Kubernetes CLI
- **Terraform 1.4+** - Infrastructure provisioning
- **AWS CLI** - AWS service management
- **Helm 3.x** - Kubernetes package manager
- **Git** - Version control

### Local Development Setup

1. **Clone the repository**
```bash
git clone https://github.com/jihaneeee/UrbanFlow.git
cd UrbanFlow
```

2. **Start infrastructure services**
```bash
docker-compose up -d
```

3. **Run microservices** (example for ticket-service)
```bash
cd backend-services/ticketservice/ticket-service
mvn spring-boot:run
```

4. **Run frontend**
```bash
cd transport-frontend
npm install
npm start
```

---

## 📊 Service Ports

| Service | Port |
|---------|------|
| API Gateway | 8080 |
| Route Service | 8081 |
| Ticket Service | 8082 |
| User Service | 8083 |
| Payment Service | 8085 |
| Geolocation Service | 8091 |
| Frontend | 80 |
| RabbitMQ | 5672 |
| RabbitMQ Management | 15672 |
| PostgreSQL | 5432 |
| Prometheus | 9090 |
| Grafana | 3000 |

---

## 🔐 Security

### Application Security
- **JWT-based authentication** - Secure token-based auth
- **Spring Security** - Authentication and authorization
- **API rate limiting** - Protection against abuse
- **HTTPS in production** - Encrypted communication
- **Database encryption** - Data at rest encryption

### Infrastructure Security
- **Kubernetes Secrets** - Secure secret management
  - Secrets stored in GitHub Secrets (not in repository)
  - `secret-db.yaml` is git-ignored
- **VPC with Private Subnets** - Network isolation
- **IAM Roles** - Least privilege access
- **ECR Image Scanning** - Vulnerability scanning
- **Resource Limits** - CPU and memory limits on all pods

---

## 📦 Deployment

### Docker (Local Development)
```bash
# Build all services
docker-compose build

# Run with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f
```

### Infrastructure Provisioning (Terraform)

1. **Initialize Terraform**
```bash
cd infra/terraform
terraform init
```

2. **Plan infrastructure changes**
```bash
terraform plan
```

3. **Apply infrastructure**
```bash
terraform apply
```

This will create:
- ECR repositories for all services
- VPC with public/private subnets
- Networking resources

### Kubernetes Deployment (AWS EKS)

#### Prerequisites
- EKS cluster created and configured
- `kubectl` configured for your cluster
- AWS credentials configured

#### Deploy Application

1. **Configure kubectl for EKS**
```bash
aws eks update-kubeconfig --name <CLUSTER_NAME> --region us-east-1
```

2. **Create namespace**
```bash
kubectl apply -f k8s/namespace.yaml
```

3. **Deploy secrets** (from GitHub Secrets in CI/CD, or manually)
```bash
kubectl create secret generic db-secrets \
  --namespace=urbanflow \
  --from-literal=POSTGRES_USER=postgres \
  --from-literal=POSTGRES_PASSWORD=<PASSWORD> \
  --from-literal=JWT_SECRET=<JWT_SECRET> \
  --from-literal=JWT_EXPIRATION=86400000
```

4. **Deploy all resources using Kustomize**
```bash
kubectl apply -k k8s/
```

5. **Verify deployment**
```bash
kubectl get pods -n urbanflow
kubectl get services -n urbanflow
```

### CI/CD Pipeline (GitHub Actions)

The project includes a complete CI/CD pipeline that:

1. **Terraform Stage**
   - Validates Terraform configuration
   - Plans infrastructure changes
   - Applies changes on main branch

2. **Build & Push Stage**
   - Builds Docker images for all services
   - Pushes to AWS ECR
   - Uses matrix strategy for parallel builds

3. **Deploy Stage**
   - Creates Kubernetes secrets from GitHub Secrets
   - Applies Kubernetes manifests
   - Monitors deployment status

**Required GitHub Secrets:**
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION`

**Required GitHub Variables:**
- `AWS_REGION`
- `ECR_REGISTRY`
- `EKS_CLUSTER_NAME`

See `.github/GITHUB_SECRETS_VARIABLES_VALUES.md` for detailed setup instructions.

---

## 📈 Monitoring

### Local Development
Access monitoring dashboards:
- **Grafana**: http://localhost:3001
- **Prometheus**: http://localhost:9090
- **RabbitMQ Management**: http://localhost:15672

### Production (EKS)

#### Install Monitoring Stack
```bash
# Create monitoring namespace
kubectl create namespace monitoring

# Add Prometheus Helm repository
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Install kube-prometheus-stack
helm upgrade --install kube-prometheus-stack \
  prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  -f monitoring/kube-prometheus-values.yaml \
  --set grafana.adminPassword="<YOUR_PASSWORD>"
```

#### Access Monitoring Dashboards
```bash
# Get Grafana LoadBalancer URL
kubectl get svc -n monitoring grafana

# Port-forward for local access
kubectl port-forward -n monitoring svc/grafana 3000:80
```

**Default Credentials:**
- Username: `admin`
- Password: (set during installation)

See `monitoring/README.md` for detailed monitoring setup guide.

---

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -P integration-tests
```

---

## 📝 Project Phases

- [x] **Phase 1**: Architecture & Design
- [x] **Phase 2**: Implementation
- [x] **Phase 3**: Testing
- [x] **Phase 4**: Deployment
  - [x] Docker containerization
  - [x] Kubernetes manifests
  - [x] Terraform infrastructure
  - [x] CI/CD pipeline
  - [x] Monitoring setup
- [x] **Phase 5**: Documentation

---

## 🛠️ DevOps Tools & Technologies Summary

### Infrastructure & Orchestration
- **Kubernetes** - Container orchestration with resource management
- **Terraform** - Infrastructure as Code (ECR, VPC, networking)
- **AWS EKS** - Managed Kubernetes service
- **AWS ECR** - Container registry
- **Kustomize** - Kubernetes manifest management

### CI/CD
- **GitHub Actions** - Automated CI/CD workflows
- **Docker** - Containerization
- **Multi-stage builds** - Optimized Docker images

### Monitoring & Observability
- **Prometheus** - Metrics collection
- **Grafana** - Visualization and dashboards
- **kube-prometheus-stack** - Complete monitoring solution
- **Spring Boot Actuator** - Application metrics

### Resource Management
- **Kubernetes Resource Requests/Limits** - CPU and memory management
- **Horizontal Pod Autoscaling** - (Can be configured)
- **Namespace isolation** - Resource organization

### Security & Secrets
- **GitHub Secrets** - Secure secret storage
- **Kubernetes Secrets** - Runtime secret management
- **Git ignore** - Prevents secret file commits

## 👥 Team

- **Repository**: [UrbanFlow](https://github.com/jihaneeee/UrbanFlow)
- **Branch**: `dodo` (main development branch)

---

## 📄 License

This project is part of an academic microservices architecture study.


