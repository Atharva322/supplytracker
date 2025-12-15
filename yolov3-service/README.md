# YOLOv3 Detection Service

Python FastAPI microservice for object detection using YOLOv3/YOLOv5 model.

## Setup

### 1. Create Virtual Environment

```bash
cd yolov3-service
python -m venv venv
venv\Scripts\activate  # Windows
# source venv/bin/activate  # Linux/Mac
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

### 3. Run the Service

```bash
python app.py
```

Or with uvicorn:

```bash
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

## API Endpoints

### Health Check
```
GET http://localhost:8000/
GET http://localhost:8000/health
```

### Object Detection
```
POST http://localhost:8000/detect
Content-Type: multipart/form-data
Body: file (image file)
```

### Batch Detection
```
POST http://localhost:8000/batch-detect
Content-Type: multipart/form-data
Body: files[] (multiple image files)
```

### Quality Check
```
POST http://localhost:8000/quality-check
Content-Type: multipart/form-data
Body: file (image file)
```

## Response Format

```json
{
  "success": true,
  "detections": [
    {
      "class": "apple",
      "confidence": 0.95,
      "bbox": {
        "x1": 100,
        "y1": 200,
        "x2": 300,
        "y2": 400
      },
      "center": {
        "x": 200,
        "y": 300
      }
    }
  ],
  "count": 1,
  "image_with_boxes": "base64_encoded_image",
  "image_size": {
    "width": 640,
    "height": 480
  }
}
```

## Testing

```bash
# Test with curl
curl -X POST "http://localhost:8000/detect" \
  -H "accept: application/json" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@path/to/image.jpg"
```

## Custom Training

To use a custom-trained model:

1. Train YOLOv5 on your agricultural dataset
2. Save the weights to `yolov3-service/models/best.pt`
3. Update `app.py` to load custom weights:

```python
model = torch.hub.load('ultralytics/yolov5', 'custom', path='models/best.pt')
```

## Port Configuration

Default: 8000
Change in `app.py` or run with: `uvicorn app:app --port YOUR_PORT`
