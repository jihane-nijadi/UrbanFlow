# UrbanFlow - Kubernetes Deployment on AWS EKS

## Overview

This document provides a complete guide for deploying the UrbanFlow microservices application to AWS Elastic Kubernetes Service (EKS).

## ⚠️ IMPORTANT: Small Cluster Notice

**If you're using t3.small nodes (2 vCPU, 2GB RAM), please read `k8s/SMALL_CLUSTER_GUIDE.md` first!**

The default configurations assume t3.medium or larger nodes. For t3.small clusters, you must optimize the deployment to fit within resource constraints.

**Quick check**: If you created your cluster with:
```powershell
eksctl create cluster --node-type t3.small ...
```

**👉 Follow the [Small Cluster Guide](k8s/SMALL_CLUSTER_GUIDE.md) instead!**

## Prerequisites Checklist

- [x] Dockerized all services
- [x] Created ECR repositories
- [x] Pushed Docker images to ECR
- [x] Created EKS cluster
- [ ] Configure kubectl for EKS
- [ ] Update Kubernetes manifests with ECR URLs
- [ ] **If using t3.small: Optimize for small cluster**
- [ ] Deploy to EKS

## Quick Start Guide

### Step 1: Configure kubectl for your EKS cluster

```powershell
cd k8s
.\configure-kubectl.ps1 -ClusterName YOUR_EKS_CLUSTER_NAME -Region YOUR_AWS_REGION
```

Example:
```powershell
.\configure-kubectl.ps1 -ClusterName urbanflow-cluster -Region us-east-1
```

### Step 2: Update ECR Image References

Update all Kubernetes manifests with your actual ECR repository URLs:

```powershell
.\update-ecr-images.ps1 -AccountId YOUR_AWS_ACCOUNT_ID -Region YOUR_AWS_REGION
```

Example:
```powershell
.\update-ecr-images.ps1 -AccountId 123456789012 -Region us-east-1
```

This will replace all placeholder image URLs with your actual ECR URLs in:
- `backend/geolocation-service.yaml`
- `backend/route-service.yaml`
- `backend/ticket-service.yaml`
- `backend/payment-service.yaml`
- `backend/user-service.yaml`
- `backend/api-gateway.yaml`
- `frontend/frontend.yaml`

### Step 3: Update Secrets (CRITICAL!)

**⚠️ IMPORTANT:** Before deploying to production, update the sensitive values in `k8s/base/secrets.yaml`:

```powershell
notepad k8s\base\secrets.yaml
```

Update these values:
- `POSTGRES_PASSWORD` - Use a strong, unique password
- `JWT_SECRET` - Use a long, random string (at least 64 characters)
- `RABBITMQ_DEFAULT_PASS` - Use a strong password

### Step 4: Deploy to EKS

```powershell
# Test deployment (dry run) - recommended first!
.\deploy.ps1 -DryRun

# If dry run succeeds, deploy for real
.\deploy.ps1
```

The deployment script will:
1. Create the `urbanflow` namespace
2. Deploy ConfigMaps and Secrets
3. Deploy PostgreSQL databases (4 instances)
4. Deploy RabbitMQ message broker
5. Deploy backend microservices (6 services)
6. Deploy frontend application

### Step 5: Monitor Deployment

The script automatically waits for services to be ready, but you can also monitor manually:

```powershell
# Watch pods come up
kubectl get pods -n urbanflow -w

# Check all resources
kubectl get all -n urbanflow

# Get LoadBalancer URLs
kubectl get svc -n urbanflow
```

### Step 6: Access Your Application

After deployment, get your application URLs:

```powershell
# API Gateway URL (backend)
kubectl get svc api-gateway -n urbanflow

# Frontend URL
kubectl get svc frontend -n urbanflow
```

The `EXTERNAL-IP` column will show your LoadBalancer URL. It may take a few minutes for AWS to provision the LoadBalancer.

Access your application:
- **Frontend**: `http://<FRONTEND-EXTERNAL-IP>`
- **API Gateway**: `http://<API-GATEWAY-EXTERNAL-IP>:8080`

## Architecture Deployed

### Infrastructure Components
- **4 PostgreSQL Databases** (StatefulSets with persistent storage)
  - route-db (5Gi storage)
  - ticket-db (5Gi storage)
  - user-db (5Gi storage)
  - geolocation-db (5Gi storage)
- **RabbitMQ** (StatefulSet with 5Gi storage)

### Backend Services (All with 2 replicas)
- **API Gateway** (Port 8080) - LoadBalancer
- **Geolocation Service** (Port 8091)
- **Route Service** (Port 8081)
- **Ticket Service** (Port 8082)
- **Payment Service** (Port 8085)
- **User Service** (Port 8083)

### Frontend
- **Transport Frontend** (Port 80) - LoadBalancer, 2 replicas

## Resource Requirements

### Per Service
- **Backend Services**: 512Mi-1Gi memory, 250m-500m CPU
- **Databases**: 256Mi-512Mi memory, 250m-500m CPU
- **RabbitMQ**: 512Mi-1Gi memory, 250m-500m CPU
- **Frontend**: 256Mi-512Mi memory, 100m-250m CPU

### Minimum Cluster Requirements
- **Nodes**: 2-3 nodes recommended
- **Instance Type**: t3.medium or larger
- **Total**: ~8-12 vCPUs, 16-24 GB RAM

## Configuration Files

### Directory Structure
```
k8s/
├── base/
│   ├── namespace.yaml              # Namespace definition
│   ├── configmap.yaml              # Non-sensitive config
│   └── secrets.yaml                # Sensitive config (UPDATE THIS!)
├── databases/                      # Database StatefulSets
│   ├── route-db-statefulset.yaml
│   ├── ticket-db-statefulset.yaml
│   ├── user-db-statefulset.yaml
│   └── geolocation-db-statefulset.yaml
├── infrastructure/
│   └── rabbitmq-statefulset.yaml  # Message broker
├── backend/                        # Backend service deployments
│   ├── api-gateway.yaml
│   ├── geolocation-service.yaml
│   ├── payment-service.yaml
│   ├── route-service.yaml
│   ├── ticket-service.yaml
│   └── user-service.yaml
├── frontend/
│   └── frontend.yaml               # Frontend deployment
├── configure-kubectl.ps1           # kubectl setup script
├── update-ecr-images.ps1          # Update ECR URLs
├── deploy.ps1                      # Main deployment script
├── undeploy.ps1                   # Cleanup script
└── README.md                       # Detailed documentation
```

## Troubleshooting

### Check Pod Status
```powershell
# Get all pods
kubectl get pods -n urbanflow

# Describe a specific pod
kubectl describe pod <pod-name> -n urbanflow

# View logs
kubectl logs <pod-name> -n urbanflow

# Follow logs
kubectl logs -f <pod-name> -n urbanflow
```

### Common Issues

#### 1. ImagePullBackOff Error
**Cause**: Cannot pull image from ECR

**Solution**:
```powershell
# Verify ECR authentication
aws ecr get-login-password --region YOUR_REGION | docker login --username AWS --password-stdin YOUR_ACCOUNT_ID.dkr.ecr.YOUR_REGION.amazonaws.com

# Check image exists
aws ecr describe-images --repository-name urbanflow-api-gateway --region YOUR_REGION

# Verify image URL in deployment
kubectl describe deployment api-gateway -n urbanflow
```

#### 2. Pods Stuck in Pending
**Cause**: Insufficient cluster resources or PVC issues

**Solution**:
```powershell
# Check node resources
kubectl describe nodes

# Check PVC status
kubectl get pvc -n urbanflow

# Scale cluster if needed
aws eks update-nodegroup-config --cluster-name YOUR_CLUSTER --nodegroup-name YOUR_NODEGROUP --scaling-config minSize=3,maxSize=5,desiredSize=3
```

#### 3. CrashLoopBackOff
**Cause**: Application error or misconfiguration

**Solution**:
```powershell
# Check logs
kubectl logs <pod-name> -n urbanflow --previous

# Check events
kubectl get events -n urbanflow --sort-by='.lastTimestamp'

# Verify environment variables
kubectl exec -it <pod-name> -n urbanflow -- env
```

#### 4. LoadBalancer Pending
**Cause**: AWS LoadBalancer not provisioned yet

**Solution**: Wait 5-10 minutes for AWS to provision the LoadBalancer
```powershell
# Check LoadBalancer status
kubectl describe svc api-gateway -n urbanflow
```

### Port Forwarding for Testing

If LoadBalancer is not available, use port forwarding:

```powershell
# Forward API Gateway
kubectl port-forward -n urbanflow svc/api-gateway 8080:8080

# Forward Frontend
kubectl port-forward -n urbanflow svc/frontend 3000:80

# Forward RabbitMQ Management
kubectl port-forward -n urbanflow svc/rabbitmq-service 15672:15672
```

## Updating Services

### Update a Single Service

```powershell
# Rebuild and push new image to ECR
docker build -t urbanflow-api-gateway:latest ./backend-services/api-gateway
docker tag urbanflow-api-gateway:latest YOUR_ACCOUNT_ID.dkr.ecr.YOUR_REGION.amazonaws.com/urbanflow-api-gateway:latest
docker push YOUR_ACCOUNT_ID.dkr.ecr.YOUR_REGION.amazonaws.com/urbanflow-api-gateway:latest

# Update deployment (kubectl will pull new image)
kubectl rollout restart deployment/api-gateway -n urbanflow

# Check rollout status
kubectl rollout status deployment/api-gateway -n urbanflow
```

### Rollback a Deployment

```powershell
# Rollback to previous version
kubectl rollout undo deployment/api-gateway -n urbanflow

# Rollback to specific revision
kubectl rollout undo deployment/api-gateway --to-revision=2 -n urbanflow
```

## Scaling

### Manual Scaling

```powershell
# Scale a service
kubectl scale deployment/api-gateway --replicas=3 -n urbanflow

# Scale multiple services
kubectl scale deployment/route-service deployment/ticket-service --replicas=3 -n urbanflow
```

### Auto-scaling (Optional)

Requires Metrics Server to be installed:

```powershell
# Create HorizontalPodAutoscaler
kubectl autoscale deployment api-gateway --min=2 --max=5 --cpu-percent=70 -n urbanflow

# Check HPA status
kubectl get hpa -n urbanflow
```

## Monitoring

### Basic Monitoring

```powershell
# Get pod resource usage
kubectl top pods -n urbanflow

# Get node resource usage
kubectl top nodes

# Watch pod status
kubectl get pods -n urbanflow -w
```

### View Logs

```powershell
# All pods with label
kubectl logs -l app=api-gateway -n urbanflow

# Multiple pods
kubectl logs -l app=api-gateway --all-containers=true -n urbanflow

# Previous container instance
kubectl logs <pod-name> -n urbanflow --previous
```

### Access Databases

```powershell
# Port forward database
kubectl port-forward -n urbanflow svc/route-db-service 5432:5432

# Connect from another terminal
psql -h localhost -U postgres -d route_db

# Or exec directly into database pod
kubectl exec -it route-db-0 -n urbanflow -- psql -U postgres -d route_db
```

## Clean Up

### Remove Deployment (Keep Data)

```powershell
.\undeploy.ps1
```

### Remove Everything (Including Data)

```powershell
.\undeploy.ps1 -DeletePVCs -Force
```

### Manual Cleanup

```powershell
# Delete namespace (removes everything)
kubectl delete namespace urbanflow

# This will delete:
# - All deployments
# - All services
# - All pods
# - All StatefulSets
# - All PVCs (persistent data)
```

## Security Best Practices

1. **Update Secrets**: Change default passwords in `base/secrets.yaml`
2. **Use IAM Roles**: Configure IAM roles for service accounts (IRSA)
3. **Enable Network Policies**: Restrict pod-to-pod communication
4. **Use Private Subnets**: Deploy worker nodes in private subnets
5. **Enable Encryption**: Use encrypted EBS volumes for PVCs
6. **Regular Updates**: Keep Docker images and cluster updated
7. **Use AWS Secrets Manager**: For production secrets management

## Production Considerations

1. **Use separate namespaces** for dev, staging, and production
2. **Implement proper RBAC** for access control
3. **Set up monitoring** (CloudWatch, Prometheus, Grafana)
4. **Configure logging** (CloudWatch Logs, ELK stack)
5. **Implement backup strategy** for databases
6. **Use Ingress Controller** instead of multiple LoadBalancers
7. **Configure auto-scaling** for both pods and cluster nodes
8. **Set up CI/CD pipeline** for automated deployments

## Useful Commands Reference

```powershell
# Get everything in namespace
kubectl get all -n urbanflow

# Describe resources
kubectl describe deployment/api-gateway -n urbanflow
kubectl describe svc/api-gateway -n urbanflow
kubectl describe pod <pod-name> -n urbanflow

# Edit resources
kubectl edit deployment/api-gateway -n urbanflow
kubectl edit configmap/urbanflow-config -n urbanflow

# Execute commands in pods
kubectl exec -it <pod-name> -n urbanflow -- /bin/sh
kubectl exec -it <pod-name> -n urbanflow -- curl http://localhost:8080/actuator/health

# Copy files
kubectl cp <pod-name>:/path/to/file ./local-file -n urbanflow
kubectl cp ./local-file <pod-name>:/path/to/file -n urbanflow

# Get resource usage
kubectl top pods -n urbanflow
kubectl top nodes

# Get events
kubectl get events -n urbanflow --sort-by='.lastTimestamp'
kubectl get events -n urbanflow --field-selector type=Warning
```

## Support and Resources

- **Detailed Guide**: See `k8s/README.md`
- **Kubernetes Docs**: https://kubernetes.io/docs/
- **AWS EKS Docs**: https://docs.aws.amazon.com/eks/
- **kubectl Cheat Sheet**: https://kubernetes.io/docs/reference/kubectl/cheatsheet/

## Next Steps

After successful deployment:

1. ✅ Configure DNS for LoadBalancer URLs
2. ✅ Set up SSL/TLS certificates
3. ✅ Configure monitoring and alerting
4. ✅ Set up log aggregation
5. ✅ Implement backup strategy
6. ✅ Configure auto-scaling
7. ✅ Set up CI/CD pipeline

---

**Congratulations! Your UrbanFlow application is now running on Kubernetes! 🎉**

