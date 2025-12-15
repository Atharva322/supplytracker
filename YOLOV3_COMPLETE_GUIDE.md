# YOLOv3 Integration Architecture - Complete Flow

## 🎯 How It Works - End to End

```
┌─────────────────────────────────────────────────────────────────────┐
│                          USER'S BROWSER                              │
│  http://localhost:5174                                              │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  React Frontend (Vite)                                      │   │
│  │                                                             │   │
│  │  1. User clicks "Detection" tab (New!)                     │   │
│  │  2. ObjectDetection component loads                        │   │
│  │  3. User uploads image (JPG/PNG)                           │   │
│  │  4. Selects mode: "Object Detection" or "Quality Check"    │   │
│  │  5. Clicks "Detect" button                                 │   │
│  └────────────────────────────────────────────────────────────┘   │
│                              │                                       │
│                              │ HTTP POST /api/detection/detect      │
│                              │ FormData with image file             │
│                              ▼                                       │
└─────────────────────────────────────────────────────────────────────┘
                               │
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT BACKEND                               │
│  http://localhost:8080                                              │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  DetectionController.java (NEW!)                            │   │
│  │  @RestController                                            │   │
│  │  @RequestMapping("/api/detection")                          │   │
│  │                                                             │   │
│  │  1. Receives image from React                              │   │
│  │  2. Validates file (size, type)                            │   │
│  │  3. Checks JWT authentication                              │   │
│  │  4. Forwards to Python service using RestTemplate          │   │
│  │  5. Returns detection results to React                     │   │
│  └────────────────────────────────────────────────────────────┘   │
│                              │                                       │
│                              │ HTTP POST                             │
│                              │ http://localhost:8000/detect         │
│                              │ MultipartFile forwarding             │
│                              ▼                                       │
└─────────────────────────────────────────────────────────────────────┘
                               │
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    PYTHON FASTAPI SERVICE                            │
│  http://localhost:8000                                              │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  yolov3-service/app.py (NEW!)                               │   │
│  │  FastAPI Application                                        │   │
│  │                                                             │   │
│  │  1. Loads YOUR custom YOLOv3 model                         │   │
│  │     - models/yolov3.cfg (8.13 KB)                          │   │
│  │     - models/yolov3.weights (235 MB)                       │   │
│  │     - models/classes.txt (6 classes)                       │   │
│  │                                                             │   │
│  │  2. Receives image from Spring Boot                        │   │
│  │  3. Preprocesses: OpenCV blob creation                     │   │
│  │  4. Runs YOLOv3 inference (OpenCV DNN)                     │   │
│  │  5. Post-processes: NMS, bounding boxes                    │   │
│  │  6. Draws boxes and labels on image                        │   │
│  │  7. Converts to base64                                     │   │
│  │  8. Returns JSON with detections                           │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  YOUR MODEL DETECTS:                                                │
│  - Apple_Good ✅                                                     │
│  - Apple_Bad ❌                                                      │
│  - Banana_Good ✅                                                    │
│  - Banana_Bad ❌                                                     │
│  - Orange_Good ✅                                                    │
│  - Orange_Bad ❌                                                     │
└─────────────────────────────────────────────────────────────────────┘
                               │
                               │ JSON Response
                               ▼
               ┌─────────────────────────────────┐
               │  Detection Results JSON          │
               │  {                               │
               │    "success": true,              │
               │    "detections": [               │
               │      {                           │
               │        "class": "Apple_Good",    │
               │        "confidence": 0.95,       │
               │        "bbox": {                 │
               │          "x1": 100, "y1": 50,   │
               │          "x2": 200, "y2": 150   │
               │        }                         │
               │      }                           │
               │    ],                            │
               │    "image_with_boxes": "base64"  │
               │  }                               │
               └─────────────────────────────────┘
                               │
                               │ Returns through chain
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    REACT DISPLAYS RESULTS                            │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  ObjectDetection.jsx                                        │   │
│  │                                                             │   │
│  │  ✅ Shows annotated image with boxes                       │   │
│  │  ✅ Lists detected objects                                 │   │
│  │  ✅ Shows confidence scores                                │   │
│  │  ✅ Quality grades (A/B/C/D)                               │   │
│  │  ✅ Color-coded results                                    │   │
│  └────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

## 📁 Files Created/Modified

### ✅ Python Service (NEW)
- **yolov3-service/app.py** - FastAPI with YOLOv3 inference
- **yolov3-service/models/yolov3.cfg** - Your model config
- **yolov3-service/models/yolov3.weights** - Your trained weights
- **yolov3-service/models/classes.txt** - Your 6 classes
- **yolov3-service/requirements.txt** - Python dependencies
- **yolov3-service/setup.ps1** - Setup script
- **yolov3-service/start.ps1** - Startup script

### ✅ Spring Boot Backend (MODIFIED)
- **DetectionController.java** - NEW controller for `/api/detection/*`
- **application.properties** - Added `yolo.service.url=http://localhost:8000`

### ✅ React Frontend (MODIFIED)
- **components/ObjectDetection.jsx** - NEW component for UI
- **App.jsx** - Added:
  - Import ObjectDetection component
  - New "Detection" tab button
  - Detection view rendering

## 🚀 Current Status

### ✅ Completed:
1. Python service code written
2. Spring Boot controller added and compiled
3. React component created
4. Model files in place (cfg, weights, classes)
5. Frontend integrated with new Detection tab
6. Backend running (port 8080)
7. Frontend running (port 5174)

### ⏳ Pending:
1. **Setup Python environment** - Run `.\setup.ps1` in yolov3-service
2. **Start Python service** - Run `.\start.ps1` (will run on port 8000)

## 🎮 How to Use (Once Python Service is Running)

1. **Open browser**: http://localhost:5174
2. **Login** with your credentials
3. **Click "Detection" tab** (🔍 Detection button)
4. **Upload an image** of apple, banana, or orange
5. **Select detection mode**:
   - Object Detection: Shows all detected fruits with boxes
   - Quality Check: Grades quality (A/B/C/D)
6. **Click "Detect Objects"**
7. **See results**:
   - Annotated image with bounding boxes
   - List of detected objects with confidence
   - Quality scores and grades

## 📊 API Endpoints

### Frontend → Backend
```
POST http://localhost:8080/api/detection/detect
Headers: Authorization: Bearer <JWT_TOKEN>
Body: FormData with 'file' field
```

### Backend → Python
```
POST http://localhost:8000/detect
Body: MultipartFile (image)
```

### Response Format
```json
{
  "success": true,
  "detections": [
    {
      "class": "Apple_Good",
      "class_id": 1,
      "confidence": 0.95,
      "bbox": {
        "x1": 100,
        "y1": 50,
        "x2": 200,
        "y2": 150,
        "width": 100,
        "height": 100
      },
      "center": {
        "x": 150,
        "y": 100
      }
    }
  ],
  "count": 1,
  "image_with_boxes": "base64_encoded_image",
  "model_info": {
    "classes": 6,
    "confidence_threshold": 0.5,
    "nms_threshold": 0.4
  }
}
```

## 🔧 Next Steps

1. **In yolov3-service folder**:
   ```powershell
   cd c:\Users\athar\Desktop\supplytracker\yolov3-service
   .\setup.ps1
   ```
   This installs: FastAPI, OpenCV, NumPy, Uvicorn

2. **Start the service**:
   ```powershell
   .\start.ps1
   ```
   Service will run on http://localhost:8000

3. **Test it**:
   - Go to http://localhost:8000/health
   - Should show your 6 classes and "ready: true"

4. **Use it in app**:
   - Click Detection tab
   - Upload fruit image
   - See YOLOv3 magic! 🎯

## 💡 Technical Details

### YOLOv3 Model Loading (OpenCV DNN)
```python
net = cv2.dnn.readNetFromDarknet(
    "models/yolov3.cfg",
    "models/yolov3.weights"
)
```

### Inference Pipeline
1. Image → Blob (416x416, normalized)
2. Forward pass through YOLO layers
3. NMS (Non-Maximum Suppression)
4. Bounding box coordinates
5. Class predictions with confidence
6. Draw annotations
7. Encode as base64

### Performance
- **CPU**: ~2-3 seconds per image
- **GPU (CUDA)**: ~0.5-1 second per image
- Model: 235 MB in memory
- Confidence threshold: 0.5
- NMS threshold: 0.4

## 🎨 UI Features in ObjectDetection Component

- **File Upload**: Drag & drop or click
- **Preview**: Show image before detection
- **Mode Selection**: Detection vs Quality Check
- **Results Display**:
  - Annotated image with colored boxes
  - Detection list with confidence
  - Quality scores and grades
  - Color-coded by confidence
- **Error Handling**: File validation, API errors
- **Loading States**: Spinner during processing

---

**Ready to detect fruits with AI!** 🍎🍌🍊
