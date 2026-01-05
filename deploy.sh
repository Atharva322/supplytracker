#!/bin/bash
# Quick Deployment Script for SupplyTracker (Linux/Mac)

set -e

ENVIRONMENT=${1:-local}
ACTION=${2:-up}

echo "🚀 SupplyTracker Deployment Script"
echo "Environment: $ENVIRONMENT"
echo ""

# Check if Docker is installed
check_docker() {
    if ! command -v docker &> /dev/null || ! command -v docker-compose &> /dev/null; then
        echo "❌ Docker is not installed or not running!"
        echo "Please install Docker: https://docs.docker.com/get-docker/"
        exit 1
    fi
    echo "✅ Docker is installed"
}

# Check if .env exists
check_env() {
    if [ ! -f .env ]; then
        echo "⚠️  .env file not found!"
        echo "Creating from .env.example..."
        cp .env.example .env
        echo "✅ Created .env file"
        echo "⚠️  Please edit .env with your actual values before continuing!"
        read -p "Press Enter after editing .env file"
    fi
}

# Stop services
stop_services() {
    echo "🛑 Stopping all services..."
    if [ "$ENVIRONMENT" == "production" ]; then
        docker-compose -f docker-compose.prod.yml down
    else
        docker-compose down
    fi
    echo "✅ All services stopped"
    exit 0
}

# Show logs
show_logs() {
    echo "📋 Showing logs..."
    if [ "$ENVIRONMENT" == "production" ]; then
        docker-compose -f docker-compose.prod.yml logs -f
    else
        docker-compose logs -f
    fi
    exit 0
}

# Build images
build_images() {
    echo "🔨 Building Docker images..."
    if [ "$ENVIRONMENT" == "production" ]; then
        docker-compose -f docker-compose.prod.yml build --no-cache
    else
        docker-compose build --no-cache
    fi
    echo "✅ Build complete"
}

# Start services
start_services() {
    echo "🚀 Starting services..."
    if [ "$ENVIRONMENT" == "production" ]; then
        docker-compose -f docker-compose.prod.yml up -d
    else
        docker-compose up -d
    fi
    
    echo ""
    echo "⏳ Waiting for services to be healthy..."
    sleep 10
    
    echo ""
    echo "✅ Deployment complete!"
    echo ""
    echo "📊 Services Status:"
    if [ "$ENVIRONMENT" == "production" ]; then
        docker-compose -f docker-compose.prod.yml ps
    else
        docker-compose ps
    fi
    
    echo ""
    echo "🌐 Access your application:"
    echo "   Frontend:   http://localhost"
    echo "   Backend:    http://localhost:8080"
    echo "   YOLOv3:     http://localhost:8000"
    echo "   Grafana:    http://localhost:3000 (admin/admin123)"
    echo "   Prometheus: http://localhost:9090"
    echo ""
}

# Main logic
check_docker

case "$ACTION" in
    down)
        stop_services
        ;;
    logs)
        show_logs
        ;;
    build)
        check_env
        build_images
        ;;
    up)
        check_env
        start_services
        ;;
    rebuild)
        check_env
        build_images
        start_services
        ;;
    *)
        echo "Usage: ./deploy.sh [local|production] [up|down|logs|build|rebuild]"
        exit 1
        ;;
esac
