# Setup script for YOLOv3 Detection Service
Write-Host "🚀 Setting up YOLOv3 Detection Service..." -ForegroundColor Green

# Navigate to yolov3-service directory
Set-Location "c:\Users\athar\Desktop\supplytracker\yolov3-service"

# Check if Python is installed
Write-Host "`n📦 Checking Python installation..." -ForegroundColor Cyan
try {
    $pythonVersion = python --version
    Write-Host "✅ Python found: $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Python not found! Please install Python 3.8 or higher" -ForegroundColor Red
    exit 1
}

# Create virtual environment
Write-Host "`n🔧 Creating virtual environment..." -ForegroundColor Cyan
if (Test-Path "venv") {
    Write-Host "⚠️  Virtual environment already exists, skipping..." -ForegroundColor Yellow
} else {
    python -m venv venv
    Write-Host "✅ Virtual environment created" -ForegroundColor Green
}

# Activate virtual environment
Write-Host "`n🔌 Activating virtual environment..." -ForegroundColor Cyan
& ".\venv\Scripts\Activate.ps1"
Write-Host "✅ Virtual environment activated" -ForegroundColor Green

# Upgrade pip
Write-Host "`n⬆️  Upgrading pip..." -ForegroundColor Cyan
python -m pip install --upgrade pip --quiet
Write-Host "✅ Pip upgraded" -ForegroundColor Green

# Install dependencies
Write-Host "`n📥 Installing dependencies (this may take a few minutes)..." -ForegroundColor Cyan
pip install -r requirements.txt
Write-Host "✅ Dependencies installed" -ForegroundColor Green

# Test installation
Write-Host "`n🧪 Testing installation..." -ForegroundColor Cyan
python -c "import fastapi, torch, cv2; print('✅ All modules imported successfully')"

Write-Host "`n✨ Setup complete!" -ForegroundColor Green
Write-Host "`nTo start the YOLOv3 service, run:" -ForegroundColor Cyan
Write-Host "    python app.py" -ForegroundColor Yellow
Write-Host "`nOr with uvicorn:" -ForegroundColor Cyan
Write-Host "    uvicorn app:app --host 0.0.0.0 --port 8000 --reload" -ForegroundColor Yellow
