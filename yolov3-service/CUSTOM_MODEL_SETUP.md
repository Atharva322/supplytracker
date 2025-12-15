# Custom YOLOv3 Model Integration Guide

## What You Need

You have ✅:
1. `yolov3.cfg` - Your model configuration file
2. `yolov3.weights` - Your trained weights

You need ❓:
3. **`classes.txt`** - Text file with your class names (one per line)

## Step 1: Create classes.txt

Create a file named `classes.txt` with your class names, **one class per line**, in the **exact order** they were during training.

Example:
```
fresh_tomato
rotten_tomato
fresh_potato
damaged_potato
fresh_onion
```

**Important**: 
- The order MUST match your training configuration
- No empty lines
- No special characters or extra spaces
- Case-sensitive (usually lowercase)

## Step 2: Place Your Files

Put all three files in the `models/` folder:

```
yolov3-service/
├── models/
│   ├── yolov3.cfg          ← Your config file
│   ├── yolov3.weights      ← Your weights file (can be large, 200+ MB)
│   └── classes.txt         ← Create this file!
├── app.py
├── requirements.txt
└── ...
```

## Step 3: Install Dependencies

```powershell
cd c:\Users\athar\Desktop\supplytracker\yolov3-service
.\setup.ps1
```

This installs:
- FastAPI (web framework)
- OpenCV (for YOLOv3 inference)
- NumPy (numerical operations)
- Uvicorn (ASGI server)

**No PyTorch needed!** ✅ OpenCV DNN handles everything.

## Step 4: Start the Service

```powershell
.\start.ps1
```

Or manually:
```powershell
.\venv\Scripts\activate
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

## Step 5: Verify Model Loading

Check the console output. You should see:

```
📋 Loaded 12 classes: ['fresh_tomato', 'rotten_tomato', ...]
🔄 Loading YOLOv3 model...
💻 Using CPU (Install CUDA for GPU acceleration)
✅ Custom YOLOv3 model loaded successfully!
📊 Model info:
   - Config: models/yolov3.cfg
   - Weights: models/yolov3.weights
   - Classes: 12
   - Confidence threshold: 0.5
   - NMS threshold: 0.4
```

## Step 6: Test the Service

### Test 1: Health Check
```powershell
curl http://localhost:8000/health
```

Expected response:
```json
{
  "status": "healthy",
  "model": "Custom YOLOv3",
  "classes": ["fresh_tomato", "rotten_tomato", ...],
  "total_classes": 12,
  "backend": "OpenCV DNN (CPU)",
  "config": {
    "confidence_threshold": 0.5,
    "nms_threshold": 0.4
  },
  "ready": true
}
```

### Test 2: Object Detection
```powershell
curl -X POST "http://localhost:8000/detect" -F "file=@test-image.jpg"
```

## Configuration Options

Edit `app.py` to adjust detection parameters:

```python
# Detection parameters (around line 36)
CONFIDENCE_THRESHOLD = 0.5  # Minimum confidence (0.0 - 1.0)
NMS_THRESHOLD = 0.4         # Non-Maximum Suppression threshold
```

Lower confidence = more detections (but more false positives)
Higher confidence = fewer detections (but more accurate)

## GPU Acceleration (Optional)

For faster inference, install CUDA-enabled OpenCV:

```powershell
pip uninstall opencv-python
pip install opencv-contrib-python
```

Requires:
- NVIDIA GPU
- CUDA Toolkit installed
- cuDNN installed

## Troubleshooting

### Error: "Config file not found"
- Make sure `yolov3.cfg` is in `yolov3-service/models/` folder
- Check file name spelling (case-sensitive on some systems)

### Error: "Weights file not found"
- Make sure `yolov3.weights` is in `yolov3-service/models/` folder
- File can be large (200-300 MB), ensure download completed

### Error: "Classes file not found"
- **Create `classes.txt`** in `yolov3-service/models/` folder
- Add one class name per line
- Match the order from your training

### Model loads but no detections
- Lower `CONFIDENCE_THRESHOLD` (e.g., 0.3)
- Check if your image contains objects from your trained classes
- Verify class names in `classes.txt` match training

### Service won't start
- Check port 8000 is not in use: `Get-NetTCPConnection -LocalPort 8000`
- Activate virtual environment first
- Check all dependencies installed: `pip list`

## What Changed from Default YOLOv5

✅ **Removed PyTorch** - Lighter and faster
✅ **Uses OpenCV DNN** - No heavy ML frameworks needed
✅ **Supports custom .cfg and .weights** - Your trained model!
✅ **GPU acceleration** - If CUDA available
✅ **Detailed logging** - See exactly what's loading

## Next Steps

Once your service is running:

1. **Test with Spring Boot**: Rebuild backend and call `/api/detection/detect`
2. **Integrate with React**: Use the ObjectDetection component
3. **Test end-to-end**: Upload image from React → Spring Boot → Python → Results

## File Structure After Setup

```
yolov3-service/
├── models/
│   ├── yolov3.cfg          ✅ Your file
│   ├── yolov3.weights      ✅ Your file (large)
│   ├── classes.txt         ⚠️  CREATE THIS!
│   ├── classes.txt.example  (reference)
│   └── README.md
├── venv/                   (created by setup.ps1)
├── app.py                  (updated for custom model)
├── requirements.txt        (simplified)
├── setup.ps1
├── start.ps1
└── README.md
```

---

**Need help?** Check the console logs when starting the service - they're very detailed!
