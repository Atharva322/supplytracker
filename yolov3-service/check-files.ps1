# YOLOv3 Model Files Verification
# Checks if all required files are present

Write-Host "`nChecking YOLOv3 Model Files...`n" -ForegroundColor Cyan

$MODEL_DIR = "models"
$allGood = $true

# Check models directory
if (-not (Test-Path $MODEL_DIR)) {
    Write-Host "MISSING: models/ directory not found!" -ForegroundColor Red
    New-Item -ItemType Directory -Path $MODEL_DIR -Force | Out-Null
    $allGood = $false
} else {
    Write-Host "OK: models/ directory exists" -ForegroundColor Green
}

# Check yolov3.cfg
$cfgPath = Join-Path $MODEL_DIR "yolov3.cfg"
if (Test-Path $cfgPath) {
    $cfgSize = (Get-Item $cfgPath).Length
    $sizeKB = [math]::Round($cfgSize/1024, 2)
    Write-Host "OK: yolov3.cfg found - Size: $sizeKB KB" -ForegroundColor Green
} else {
    Write-Host "MISSING: yolov3.cfg NOT FOUND" -ForegroundColor Red
    Write-Host "   Action: Place yolov3.cfg in models/ folder" -ForegroundColor Yellow
    $allGood = $false
}

# Check yolov3.weights
$weightsPath = Join-Path $MODEL_DIR "yolov3.weights"
if (Test-Path $weightsPath) {
    $weightsSize = (Get-Item $weightsPath).Length
    $sizeMB = [math]::Round($weightsSize/1MB, 2)
    Write-Host "OK: yolov3.weights found - Size: $sizeMB MB" -ForegroundColor Green
} else {
    Write-Host "MISSING: yolov3.weights NOT FOUND" -ForegroundColor Red
    Write-Host "   Action: Place yolov3.weights in models/ folder" -ForegroundColor Yellow
    $allGood = $false
}

# Check classes.txt
$classesPath = Join-Path $MODEL_DIR "classes.txt"
if (Test-Path $classesPath) {
    $classes = Get-Content $classesPath | Where-Object { $_.Trim() -ne "" }
    $classCount = $classes.Count
    Write-Host "OK: classes.txt found - $classCount classes detected" -ForegroundColor Green
    Write-Host "`n   Your Classes:" -ForegroundColor Cyan
    $classes | ForEach-Object {
        Write-Host "   - $_" -ForegroundColor Gray
    }
} else {
    Write-Host "MISSING: classes.txt NOT FOUND" -ForegroundColor Red
    Write-Host "   Action: Create classes.txt with class names (one per line)" -ForegroundColor Yellow
    $allGood = $false
}

# Check Python environment
Write-Host "`nChecking Python Environment...`n" -ForegroundColor Cyan

if (Test-Path "venv") {
    Write-Host "OK: Virtual environment exists" -ForegroundColor Green
} else {
    Write-Host "INFO: Virtual environment not found" -ForegroundColor Yellow
    Write-Host "   Action: Run .\setup.ps1 to create it" -ForegroundColor Yellow
}

# Check Python installation
try {
    $pythonVersion = python --version 2>&1
    Write-Host "OK: Python installed - $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "WARNING: Python not found in PATH" -ForegroundColor Yellow
}

# Final result
Write-Host "`n================================================" -ForegroundColor Gray
if ($allGood) {
    Write-Host "SUCCESS: All model files are ready!" -ForegroundColor Green
    Write-Host "`nNext Steps:" -ForegroundColor Cyan
    Write-Host "   1. Run: .\setup.ps1    (setup Python environment)" -ForegroundColor White
    Write-Host "   2. Run: .\start.ps1    (start YOLOv3 service)" -ForegroundColor White
    Write-Host "   3. Test: http://localhost:8000/health" -ForegroundColor White
} else {
    Write-Host "ACTION REQUIRED: Some files are missing" -ForegroundColor Yellow
    Write-Host "`nNext Steps:" -ForegroundColor Cyan
    Write-Host "   1. Add missing files to models/ folder" -ForegroundColor White
    Write-Host "   2. Run: .\check-files.ps1 (check again)" -ForegroundColor White
    Write-Host "   3. Run: .\setup.ps1 (setup environment)" -ForegroundColor White
    Write-Host "   4. Run: .\start.ps1 (start service)" -ForegroundColor White
}
Write-Host "================================================`n" -ForegroundColor Gray
