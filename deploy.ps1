# Quick Deployment Script for SupplyTracker
# This script helps you deploy the entire application stack

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet('local', 'production')]
    [string]$Environment = 'local',
    
    [Parameter(Mandatory=$false)]
    [switch]$Build,
    
    [Parameter(Mandatory=$false)]
    [switch]$Down,
    
    [Parameter(Mandatory=$false)]
    [switch]$Logs
)

$ErrorActionPreference = "Stop"

Write-Host "🚀 SupplyTracker Deployment Script" -ForegroundColor Cyan
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host ""

# Function to check if .env exists
function Check-EnvFile {
    if (-not (Test-Path ".env")) {
        Write-Host "⚠️  .env file not found!" -ForegroundColor Yellow
        Write-Host "Creating from .env.example..." -ForegroundColor Yellow
        Copy-Item ".env.example" ".env"
        Write-Host "✅ Created .env file" -ForegroundColor Green
        Write-Host "⚠️  Please edit .env with your actual values before continuing!" -ForegroundColor Red
        Write-Host ""
        Read-Host "Press Enter after editing .env file"
    }
}

# Function to check Docker
function Check-Docker {
    try {
        docker --version | Out-Null
        docker-compose --version | Out-Null
        Write-Host "✅ Docker is installed" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ Docker is not installed or not running!" -ForegroundColor Red
        Write-Host "Please install Docker Desktop: https://www.docker.com/products/docker-desktop" -ForegroundColor Yellow
        exit 1
    }
}

# Main deployment logic
if ($Down) {
    Write-Host "🛑 Stopping all services..." -ForegroundColor Red
    if ($Environment -eq 'production') {
        docker-compose -f docker-compose.prod.yml down
    } else {
        docker-compose down
    }
    Write-Host "✅ All services stopped" -ForegroundColor Green
    exit 0
}

if ($Logs) {
    Write-Host "📋 Showing logs..." -ForegroundColor Cyan
    if ($Environment -eq 'production') {
        docker-compose -f docker-compose.prod.yml logs -f
    } else {
        docker-compose logs -f
    }
    exit 0
}

Check-Docker
Check-EnvFile

if ($Build) {
    Write-Host "🔨 Building Docker images..." -ForegroundColor Cyan
    if ($Environment -eq 'production') {
        docker-compose -f docker-compose.prod.yml build --no-cache
    } else {
        docker-compose build --no-cache
    }
    Write-Host "✅ Build complete" -ForegroundColor Green
}

Write-Host "🚀 Starting services..." -ForegroundColor Cyan
if ($Environment -eq 'production') {
    docker-compose -f docker-compose.prod.yml up -d
} else {
    docker-compose up -d
}

Write-Host ""
Write-Host "⏳ Waiting for services to be healthy..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "✅ Deployment complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Services Status:" -ForegroundColor Cyan
if ($Environment -eq 'production') {
    docker-compose -f docker-compose.prod.yml ps
} else {
    docker-compose ps
}

Write-Host ""
Write-Host "🌐 Access your application:" -ForegroundColor Cyan
Write-Host "   Frontend:   http://localhost" -ForegroundColor White
Write-Host "   Backend:    http://localhost:8080" -ForegroundColor White
Write-Host "   YOLOv3:     http://localhost:8000" -ForegroundColor White
Write-Host "   Grafana:    http://localhost:3000 (admin/admin123)" -ForegroundColor White
Write-Host "   Prometheus: http://localhost:9090" -ForegroundColor White
Write-Host ""
Write-Host "📋 View logs: .\deploy.ps1 -Logs" -ForegroundColor Yellow
Write-Host "🛑 Stop all:  .\deploy.ps1 -Down" -ForegroundColor Yellow
Write-Host ""
