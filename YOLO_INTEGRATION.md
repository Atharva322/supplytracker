# YOLOv3 Model Integration - Complete Guide

## 🎯 Architecture Overview

```
React Frontend (Port 5174)
    ↓ Upload Image
Spring Boot Backend (Port 8080) 
    ↓ Forward Request
Python FastAPI YOLOv3 Service (Port 8000)
    ↓ Return Detections
```

## 📦 Setup Instructions

### 1. Python YOLOv3 Service Setup

```bash
cd yolov3-service

# Create virtual environment
python -m venv venv

# Activate virtual environment
venv\Scripts\activate  # Windows
# source venv/bin/activate  # Linux/Mac

# Install dependencies
pip install -r requirements.txt

# Run the service
python app.py
```

Service will start on: **http://localhost:8000**

### 2. Spring Boot Backend Setup

Already configured! The `DetectionController.java` is ready.

Configuration in `application.properties`:
```properties
yolo.service.url=http://localhost:8000
```

### 3. React Frontend Integration

The `ObjectDetection.jsx` component is ready to use.

#### Add to App.jsx:

```jsx
import ObjectDetection from './components/ObjectDetection';

// Add a new tab for detection
{activeTab === 'detection' && <ObjectDetection />}

// Add button in navigation
<button
  onClick={() => setActiveTab('detection')}
  className={/* your styling */}
>
  🔍 Detection
</button>
```

---

## 🚀 Quick Start (All Services)

### Terminal 1 - Python YOLOv3 Service
```bash
cd yolov3-service
venv\Scripts\activate
python app.py
```

### Terminal 2 - Spring Boot Backend
```bash
cd supplytracker1
$env:GOOGLE_CLIENT_ID='your-client-id'
$env:GOOGLE_CLIENT_SECRET='your-secret'
java -jar target\supplytracker-1.0-SNAPSHOT.jar
```

### Terminal 3 - React Frontend
```bash
cd supplytracker-frontend
npm run dev
```

---

## 📡 API Endpoints

### Python Service (Port 8000)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Health check |
| `/health` | GET | Detailed status |
| `/detect` | POST | Object detection |
| `/quality-check` | POST | Quality inspection |
| `/batch-detect` | POST | Batch processing |

### Spring Boot (Port 8080)

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/detection/health` | GET | Check YOLO service | No |
| `/api/detection/detect` | POST | Detect objects | Yes |
| `/api/detection/quality-check` | POST | Quality check | Yes |
| `/api/detection/batch-detect` | POST | Batch detection | Yes |

---

## 🧪 Testing

### Test Python Service Directly:
```bash
curl -X POST "http://localhost:8000/detect" \
  -F "file=@test_image.jpg"
```

### Test via Spring Boot:
```bash
curl -X POST "http://localhost:8080/api/detection/detect" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@test_image.jpg"
```

---

## 🎨 Frontend Features

1. **Image Upload** - Drag & drop or click to upload
2. **Preview** - See image before detection
3. **Mode Selection**:
   - 🎯 Object Detection - Detect and count objects
   - ✅ Quality Check - Inspect quality and grade products
4. **Real-time Results**:
   - Bounding boxes on detected objects
   - Confidence scores
   - Quality grades (A, B, C, D)
   - Issue detection

---

## 🔧 Customization

### Add Custom Classes

Edit `yolov3-service/app.py`:

```python
AGRI_CLASSES = [
    'tomato', 'potato', 'apple', 'banana',
    'damaged', 'fresh', 'rotten', 'defect',
    # Add your custom classes here
]
```

### Train Custom Model

1. Prepare dataset with labeled images
2. Train YOLOv5 model:
```bash
python train.py --data custom.yaml --weights yolov5s.pt --epochs 100
```
3. Update `app.py` to load custom weights:
```python
model = torch.hub.load('ultralytics/yolov5', 'custom', path='models/best.pt')
```

---

## 🐛 Troubleshooting

### Python Service Won't Start
- Check Python version (3.8+)
- Install missing dependencies: `pip install -r requirements.txt`
- Check port 8000 is available

### Spring Boot Can't Connect
- Verify YOLOv3 service is running: http://localhost:8000/health
- Check `application.properties` has correct URL
- Check firewall settings

### React Upload Fails
- Verify JWT token is valid (login first)
- Check file size (< 10MB)
- Check file format (PNG, JPG, JPEG only)
- Open browser console for errors

---

## 📊 Performance

- **Detection Time**: ~200-500ms per image
- **Max File Size**: 10MB
- **Supported Formats**: PNG, JPG, JPEG
- **Concurrent Requests**: Up to 10 (configurable)

---

## 🔒 Security

- ✅ JWT authentication required
- ✅ File type validation
- ✅ File size limits
- ✅ CORS configured
- ✅ Role-based access (USER, ADMIN)

---

## 📈 Future Enhancements

1. **Batch Processing UI** - Upload multiple images
2. **History Tracking** - Save detection results to MongoDB
3. **Export Results** - Download as JSON/CSV
4. **Real-time Video** - Webcam detection
5. **Custom Training UI** - Upload dataset and train
6. **Analytics Dashboard** - Detection statistics

---

## 🤝 Integration with Products

Link detection results with products:

```java
@PostMapping("/products/{id}/detect")
public ResponseEntity<?> detectProductQuality(
    @PathVariable String id,
    @RequestParam MultipartFile file) {
    // Detect quality
    // Update product status based on results
    // Save detection history
}
```

---

## 📝 Example Response

### Object Detection:
```json
{
  "success": true,
  "detections": [
    {
      "class": "apple",
      "confidence": 0.95,
      "bbox": {"x1": 100, "y1": 200, "x2": 300, "y2": 400},
      "center": {"x": 200, "y": 300}
    }
  ],
  "count": 1,
  "image_with_boxes": "base64_string...",
  "image_size": {"width": 640, "height": 480}
}
```

### Quality Check:
```json
{
  "success": true,
  "quality_score": 85,
  "grade": "B",
  "issues": [
    {
      "type": "minor_defect",
      "severity": "low",
      "confidence": 0.75
    }
  ],
  "status": "approved"
}
```

---

## 🎓 Resources

- [YOLOv5 Documentation](https://docs.ultralytics.com/)
- [FastAPI Docs](https://fastapi.tiangolo.com/)
- [PyTorch Hub](https://pytorch.org/hub/)
- [Custom Dataset Training](https://github.com/ultralytics/yolov5/wiki/Train-Custom-Data)
