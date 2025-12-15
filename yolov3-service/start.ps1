# Start YOLOv3 Detection Service
Write-Host "🔍 Starting YOLOv3 Detection Service..." -ForegroundColor Green

# Navigate to directory
Set-Location "c:\Users\athar\Desktop\supplytracker\yolov3-service"

# Check if venv exists
if (-not (Test-Path "venv")) {
    Write-Host "❌ Virtual environment not found! Run setup.ps1 first" -ForegroundColor Red
    exit 1
}

# Activate virtual environment
Write-Host "🔌 Activating virtual environment..." -ForegroundColor Cyan
& ".\venv\Scripts\Activate.ps1"

# Start service
Write-Host "🚀 Starting FastAPI service on port 8000..." -ForegroundColor Green
Write-Host "`n✨ Service will be available at: http://localhost:8000" -ForegroundColor Yellow
Write-Host "📊 API docs at: http://localhost:8000/docs" -ForegroundColor Yellow
Write-Host "❤️  Health check: http://localhost:8000/health" -ForegroundColor Yellow
Write-Host "`nPress Ctrl+C to stop`n" -ForegroundColor Cyan

python app.py
