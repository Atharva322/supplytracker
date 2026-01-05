# 🚀 Quick Start Guide - Web Deployment

This guide will help you deploy SupplyTracker to the web in minutes!

## 🎯 Choose Your Deployment Platform

### Option 1: Railway (Easiest - Recommended)
**Perfect for:** Quick deployments, free tier available, automatic HTTPS

```powershell
# 1. Install Railway CLI
npm i -g @railway/cli

# 2. Login
railway login

# 3. Create project
railway init

# 4. Add MongoDB
railway add --service mongodb

# 5. Add Redis
railway add --service redis

# 6. Deploy all services
railway up
```

**Cost:** Free tier available, ~$5-10/month for production

---

### Option 2: Docker on VPS (Most Control)
**Perfect for:** DigitalOcean, Linode, AWS EC2, Azure VM

```bash
# 1. SSH into your VPS
ssh user@your-server-ip

# 2. Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# 3. Clone your repo
git clone https://github.com/yourusername/supplytracker.git
cd supplytracker

# 4. Setup environment
cp .env.example .env
nano .env  # Edit with your values

# 5. Deploy!
./deploy.sh production rebuild
```

**Cost:** $5-20/month (DigitalOcean, Linode)

---

### Option 3: Render (Zero Config)
**Perfect for:** Automatic deployments from GitHub

1. Go to [render.com](https://render.com)
2. Click "New Blueprint Instance"
3. Connect your GitHub repo
4. Select `render.yaml`
5. Click "Apply"

**Cost:** Free tier available, ~$7/month for production

---

## 🏃 Quick Local Test

Before deploying, test locally:

```powershell
# Windows
.\deploy.ps1 -Build -Environment production

# Linux/Mac
chmod +x deploy.sh
./deploy.sh production rebuild
```

Access at: http://localhost

---

## 📝 Pre-Deployment Checklist

- [ ] Set strong `JWT_SECRET` in `.env`
- [ ] Configure Google OAuth credentials
- [ ] Update `VITE_API_URL` in frontend `.env.production`
- [ ] Test all services locally first
- [ ] Have domain name ready (optional)
- [ ] SSL certificate (automatic on Railway/Render)

---

## 🌐 After Deployment

### 1. Update Google OAuth Redirect URIs

In [Google Cloud Console](https://console.cloud.google.com):
```
Authorized redirect URIs:
https://your-backend-url.com/login/oauth2/code/google
https://your-frontend-url.com/oauth-callback.html
```

### 2. Create Admin User

```bash
# Access your backend
curl -X POST https://your-backend.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@example.com",
    "password": "Admin@123",
    "roles": ["ROLE_ADMIN"]
  }'
```

### 3. Test the Deployment

```bash
# Backend health
curl https://your-backend.com/actuator/health

# Frontend
curl https://your-frontend.com/
```

---

## 🔧 Platform-Specific Instructions

### Railway Detailed Setup

1. **Create Railway Account**: [railway.app](https://railway.app)

2. **Deploy via GitHub**:
   - Push your code to GitHub
   - Connect Railway to your repo
   - Railway auto-detects Dockerfiles

3. **Add Services**:
   ```bash
   railway add mongodb
   railway add redis
   ```

4. **Set Environment Variables** (in Railway dashboard):
   - `JWT_SECRET`: Generate secure random string
   - `GOOGLE_CLIENT_ID`: Your OAuth client ID
   - `GOOGLE_CLIENT_SECRET`: Your OAuth secret

5. **Link Services**:
   - Backend → MongoDB: `${{MongoDB.DATABASE_URL}}`
   - Backend → Redis: `${{Redis.REDIS_URL}}`

---

### Render Detailed Setup

1. **Create Render Account**: [render.com](https://render.com)

2. **For MongoDB**: Use [MongoDB Atlas](https://cloud.mongodb.com) (free tier)

3. **Deploy Services**:
   - **Backend**: Web Service → Docker
   - **Frontend**: Static Site
   - **YOLOv3**: Web Service → Docker
   - **Redis**: Managed Redis (paid) or use Redis Cloud (free)

4. **Environment Variables** (set in each service):
   - Backend: All from `.env.example`
   - Frontend: `VITE_API_URL` = Backend URL

---

### VPS (DigitalOcean/AWS/Azure) Setup

1. **Create Droplet/Instance**:
   - Ubuntu 22.04 LTS
   - 2GB RAM minimum (4GB recommended)
   - 50GB storage

2. **Initial Setup**:
   ```bash
   # Update system
   sudo apt update && sudo apt upgrade -y
   
   # Install Docker
   curl -fsSL https://get.docker.com -o get-docker.sh
   sudo sh get-docker.sh
   sudo usermod -aG docker $USER
   
   # Install Docker Compose
   sudo apt install docker-compose -y
   ```

3. **Deploy Application**:
   ```bash
   git clone https://github.com/yourusername/supplytracker.git
   cd supplytracker
   cp .env.example .env
   nano .env  # Edit values
   chmod +x deploy.sh
   ./deploy.sh production rebuild
   ```

4. **Setup Nginx Reverse Proxy** (optional):
   ```bash
   sudo apt install nginx -y
   # Configure nginx to proxy to port 80
   ```

5. **Setup SSL with Let's Encrypt**:
   ```bash
   sudo apt install certbot python3-certbot-nginx -y
   sudo certbot --nginx -d yourdomain.com
   ```

---

## 💰 Cost Comparison

| Platform | Free Tier | Paid Tier | Best For |
|----------|-----------|-----------|----------|
| **Railway** | 500 hrs/month | $5-10/mo | Quick deployments |
| **Render** | Limited | $7/mo | GitHub auto-deploy |
| **DigitalOcean** | $200 credit | $12/mo | Full control |
| **Heroku** | Limited | $7/mo/dyno | Simple apps |
| **AWS** | 12mo free | Variable | Enterprise |

---

## 🆘 Troubleshooting

### Services Won't Start
```bash
# Check logs
docker-compose -f docker-compose.prod.yml logs

# Restart specific service
docker-compose -f docker-compose.prod.yml restart backend
```

### Can't Connect to MongoDB
```bash
# Check MongoDB is running
docker-compose -f docker-compose.prod.yml ps mongodb

# Test connection
docker exec -it supplytracker-mongodb mongosh
```

### Frontend Can't Reach Backend
Update `supplytracker-frontend/.env.production`:
```env
VITE_API_URL=https://your-backend-url.com
```
Then rebuild frontend:
```bash
cd supplytracker-frontend
npm run build
```

---

## 📚 Next Steps

1. ✅ Deploy using one of the methods above
2. ✅ Configure domain name and SSL
3. ✅ Create admin user
4. ✅ Test all features
5. ✅ Monitor with Grafana
6. ✅ Setup automated backups

**Need help?** Check the detailed [DEPLOYMENT.md](DEPLOYMENT.md) guide!

---

**Happy Deploying! 🎉**
