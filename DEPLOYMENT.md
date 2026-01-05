# 🚀 Deployment Guide - SupplyTracker

Complete guide for deploying SupplyTracker to production environments.

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start - Docker Compose](#quick-start---docker-compose)
- [Cloud Deployment Options](#cloud-deployment-options)
  - [Railway](#railway-deployment)
  - [Render](#render-deployment)
  - [AWS](#aws-deployment)
  - [Azure](#azure-deployment)
  - [Google Cloud](#google-cloud-deployment)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Environment Variables](#environment-variables)
- [Post-Deployment](#post-deployment)

---

## 🔧 Prerequisites

- Docker & Docker Compose installed
- Domain name (optional but recommended)
- SSL certificate (Let's Encrypt recommended)
- Cloud account (AWS/Azure/GCP/Railway/Render)
- Google OAuth2 credentials configured

---

## 🐳 Quick Start - Docker Compose

### 1. Clone and Configure

```bash
git clone https://github.com/yourusername/supplytracker.git
cd supplytracker

# Copy environment template
cp .env.example .env

# Edit .env with your production values
nano .env
```

### 2. Build and Run All Services

```bash
# Build all Docker images
docker-compose -f docker-compose.prod.yml build

# Start all services
docker-compose -f docker-compose.prod.yml up -d

# Check status
docker-compose -f docker-compose.prod.yml ps

# View logs
docker-compose -f docker-compose.prod.yml logs -f
```

### 3. Access Services

- **Frontend**: http://localhost
- **Backend API**: http://localhost:8080
- **YOLOv3 API**: http://localhost:8000
- **Grafana**: http://localhost:3000
- **Prometheus**: http://localhost:9090

### 4. Create Admin User

```bash
# Access backend container
docker exec -it supplytracker-backend sh

# Or use the test-roles script
powershell -ExecutionPolicy Bypass -File test-roles.ps1
```

---

## ☁️ Cloud Deployment Options

### 🚂 Railway Deployment

Railway is the easiest platform for deploying this multi-service app.

#### Step 1: Prepare Railway Project

1. Sign up at [railway.app](https://railway.app)
2. Install Railway CLI:
```bash
npm i -g @railway/cli
railway login
```

#### Step 2: Create New Project

```bash
# Initialize Railway project
railway init

# Create services
railway add --service mongodb
railway add --service redis
```

#### Step 3: Deploy Services

```bash
# Deploy backend
cd supplytracker1
railway up

# Deploy frontend
cd ../supplytracker-frontend
railway up

# Deploy YOLOv3 service
cd ../yolov3-service
railway up
```

#### Step 4: Configure Environment Variables

In Railway dashboard, set these variables for each service:

**Backend Service:**
```
SPRING_DATA_MONGODB_URI=${{MongoDB.MONGODB_URL}}
SPRING_DATA_REDIS_HOST=${{Redis.REDIS_HOST}}
SPRING_DATA_REDIS_PORT=${{Redis.REDIS_PORT}}
JWT_SECRET=your-secure-jwt-secret
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
YOLO_SERVICE_URL=${{yolov3-service.RAILWAY_STATIC_URL}}
```

**Frontend Service:**
```
VITE_API_URL=${{backend.RAILWAY_STATIC_URL}}
```

**YOLOv3 Service:**
```
PORT=8000
```

---

### 🎨 Render Deployment

#### Step 1: Create Render Account

Sign up at [render.com](https://render.com)

#### Step 2: Deploy MongoDB

1. Create new **Web Service** → Choose "Private Service"
2. Or use MongoDB Atlas (recommended): [cloud.mongodb.com](https://cloud.mongodb.com)

#### Step 3: Deploy Backend

1. **New Web Service** → Connect your GitHub repo
2. Configure:
   - **Name**: `supplytracker-backend`
   - **Environment**: Docker
   - **Dockerfile Path**: `supplytracker1/Dockerfile`
   - **Port**: 8080
   - **Environment Variables**: Add all from `.env.example`

#### Step 4: Deploy Frontend

1. **New Static Site** → Connect your GitHub repo
2. Configure:
   - **Build Command**: `cd supplytracker-frontend && npm install && npm run build`
   - **Publish Directory**: `supplytracker-frontend/dist`

#### Step 5: Deploy YOLOv3 Service

1. **New Web Service** → Connect your GitHub repo
2. Configure:
   - **Name**: `supplytracker-yolov3`
   - **Environment**: Docker
   - **Dockerfile Path**: `yolov3-service/Dockerfile`
   - **Port**: 8000

---

### ☁️ AWS Deployment

#### Option A: Elastic Beanstalk (Easiest)

```bash
# Install EB CLI
pip install awsebcli

# Initialize
cd supplytracker1
eb init -p docker supplytracker-backend

# Deploy
eb create supplytracker-production
```

#### Option B: ECS with Fargate (Recommended)

1. **Create ECR Repositories**:
```bash
aws ecr create-repository --repository-name supplytracker-backend
aws ecr create-repository --repository-name supplytracker-frontend
aws ecr create-repository --repository-name supplytracker-yolov3
```

2. **Build and Push Images**:
```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build and push backend
cd supplytracker1
docker build -t supplytracker-backend .
docker tag supplytracker-backend:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/supplytracker-backend:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/supplytracker-backend:latest
```

3. **Create ECS Cluster and Services** via AWS Console

4. **Setup RDS for MongoDB** or use MongoDB Atlas

---

### 🔵 Azure Deployment

#### Azure Container Apps (Recommended)

```bash
# Install Azure CLI
# Login
az login

# Create resource group
az group create --name supplytracker-rg --location eastus

# Create container apps environment
az containerapp env create \
  --name supplytracker-env \
  --resource-group supplytracker-rg \
  --location eastus

# Deploy backend
az containerapp create \
  --name supplytracker-backend \
  --resource-group supplytracker-rg \
  --environment supplytracker-env \
  --image <your-registry>/supplytracker-backend:latest \
  --target-port 8080 \
  --ingress external \
  --env-vars JWT_SECRET=<secret> GOOGLE_CLIENT_ID=<id>
```

#### Azure Cosmos DB

Use Azure Cosmos DB for MongoDB API for production database.

---

### 🟢 Google Cloud Deployment

#### Cloud Run (Serverless)

```bash
# Install gcloud CLI
# Login
gcloud auth login

# Set project
gcloud config set project YOUR_PROJECT_ID

# Deploy backend
cd supplytracker1
gcloud run deploy supplytracker-backend \
  --source . \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated

# Deploy frontend
cd ../supplytracker-frontend
gcloud run deploy supplytracker-frontend \
  --source . \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

---

## ⚓ Kubernetes Deployment

### 1. Create Kubernetes Manifests

See `k8s/` directory for complete manifests.

### 2. Deploy to Kubernetes

```bash
# Create namespace
kubectl create namespace supplytracker

# Apply secrets
kubectl create secret generic supplytracker-secrets \
  --from-literal=JWT_SECRET=your-secret \
  --from-literal=GOOGLE_CLIENT_ID=your-id \
  --from-literal=GOOGLE_CLIENT_SECRET=your-secret \
  -n supplytracker

# Apply manifests
kubectl apply -f k8s/ -n supplytracker

# Check status
kubectl get pods -n supplytracker
kubectl get services -n supplytracker
```

---

## 🔐 Environment Variables

### Production Requirements

**Required Variables:**

| Variable | Description | Example |
|----------|-------------|---------|
| `JWT_SECRET` | Secret for JWT tokens (32+ chars) | `your-super-secret-key-change-this` |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID | `123456.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | Google OAuth secret | `GOCSPX-xxx` |
| `MONGODB_URI` | MongoDB connection string | `mongodb://mongo:27017/agriproj` |

**Optional but Recommended:**

| Variable | Description | Default |
|----------|-------------|---------|
| `REDIS_HOST` | Redis hostname | `localhost` |
| `CORS_ORIGINS` | Allowed origins | `http://localhost` |
| `GRAFANA_ADMIN_PASSWORD` | Grafana password | `admin123` |

### Setting Environment Variables

**Docker Compose:**
```bash
# Edit .env file
nano .env
```

**Railway:**
```bash
railway variables set JWT_SECRET=your-secret
```

**Render:**
Set in dashboard under "Environment" tab

**Kubernetes:**
```bash
kubectl create secret generic app-secrets \
  --from-literal=JWT_SECRET=your-secret
```

---

## 📊 Post-Deployment

### 1. Health Checks

```bash
# Backend health
curl https://your-domain.com/actuator/health

# Frontend health
curl https://your-frontend-domain.com/health

# YOLOv3 health
curl https://your-yolo-domain.com/health
```

### 2. Create Admin User

```powershell
# Using the test-roles script
$env:API_URL="https://your-backend-url.com"
.\test-roles.ps1
```

Or manually via API:
```bash
curl -X POST https://your-domain.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@example.com",
    "password": "Admin@123",
    "roles": ["ROLE_ADMIN"]
  }'
```

### 3. Configure Google OAuth Redirect URIs

Update in Google Cloud Console:
```
https://your-backend-domain.com/login/oauth2/code/google
https://your-frontend-domain.com/oauth-callback.html
```

### 4. Setup SSL/TLS

**Let's Encrypt (Free):**
```bash
# Using Certbot
sudo certbot --nginx -d your-domain.com
```

**Cloud Platform:**
Most platforms auto-provision SSL certificates.

### 5. Setup Monitoring

Access Grafana at `https://your-domain.com:3000`:
- Username: `admin`
- Password: Set via `GRAFANA_ADMIN_PASSWORD`

Import the dashboard from `monitoring/supplytracker-dashboard.json`

### 6. Configure Backups

**MongoDB Backup:**
```bash
# Automated backup script
docker exec supplytracker-mongodb mongodump \
  --out /backup/$(date +%Y%m%d_%H%M%S) \
  --db agriproj
```

**Database-as-a-Service:**
- MongoDB Atlas: Automatic backups included
- AWS DocumentDB: Configure automated backups

---

## 🔍 Troubleshooting

### Backend Won't Start

```bash
# Check logs
docker logs supplytracker-backend

# Common issues:
# - MongoDB connection failed → Check MONGODB_URI
# - Redis connection failed → Check REDIS_HOST
# - YOLOv3 unreachable → Check YOLO_SERVICE_URL
```

### Frontend Can't Reach Backend

```bash
# Update API URL in frontend
# Create supplytracker-frontend/.env.production:
VITE_API_URL=https://your-backend-url.com
```

### YOLOv3 Model Not Found

```bash
# Download weights
cd yolov3-service/models
wget https://pjreddie.com/media/files/yolov3.weights
```

### CORS Errors

Add frontend URL to backend's allowed origins in `application.properties`:
```properties
cors.allowed.origins=https://your-frontend-url.com
```

---

## 📈 Scaling

### Horizontal Scaling

**Docker Compose:**
```bash
docker-compose -f docker-compose.prod.yml up -d --scale backend=3
```

**Kubernetes:**
```bash
kubectl scale deployment supplytracker-backend --replicas=3 -n supplytracker
```

### Load Balancing

Use NGINX or cloud load balancers:
- AWS: Application Load Balancer (ALB)
- Azure: Azure Load Balancer
- GCP: Cloud Load Balancing
- Railway/Render: Built-in

---

## 🔒 Security Checklist

- [ ] Change default JWT_SECRET
- [ ] Use strong Grafana admin password
- [ ] Enable HTTPS/SSL for all services
- [ ] Restrict MongoDB access (firewall/VPC)
- [ ] Use environment variables (never commit secrets)
- [ ] Enable Redis password authentication
- [ ] Configure CORS properly
- [ ] Implement rate limiting
- [ ] Regular security updates
- [ ] Backup strategy in place

---

## 📚 Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Railway Documentation](https://docs.railway.app/)
- [Render Documentation](https://render.com/docs)
- [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
- [Spring Boot Deployment](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)

---

## 🆘 Support

For issues or questions:
1. Check logs: `docker-compose logs -f [service-name]`
2. Review health endpoints: `/actuator/health`
3. Check monitoring: Grafana dashboards
4. Verify environment variables are set correctly

---

**Happy Deploying! 🚀**
