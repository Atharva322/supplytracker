from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import cv2
import numpy as np
from PIL import Image
import io
import base64
from typing import List, Dict
import os

app = FastAPI(title="YOLOv3 Detection Service", version="1.0.0")

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://localhost:5173", "http://localhost:5174"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global variables for model
net = None
class_names = []
output_layers = []
colors = []

# Model file paths
MODEL_DIR = "models"
CFG_PATH = os.path.join(MODEL_DIR, "yolov3.cfg")
WEIGHTS_PATH = os.path.join(MODEL_DIR, "yolov3.weights")
CLASSES_PATH = os.path.join(MODEL_DIR, "classes.txt")

# Detection parameters
CONFIDENCE_THRESHOLD = 0.5
NMS_THRESHOLD = 0.4

def load_yolov3_model():
    """Load custom YOLOv3 model using OpenCV DNN"""
    global net, class_names, output_layers, colors
    
    try:
        # Check if model files exist
        if not os.path.exists(CFG_PATH):
            print(f"⚠️  Config file not found: {CFG_PATH}")
            print("Please place your yolov3.cfg file in the models/ directory")
            return False
            
        if not os.path.exists(WEIGHTS_PATH):
            print(f"⚠️  Weights file not found: {WEIGHTS_PATH}")
            print("Please place your yolov3.weights file in the models/ directory")
            return False
            
        if not os.path.exists(CLASSES_PATH):
            print(f"⚠️  Classes file not found: {CLASSES_PATH}")
            print("Please place your classes.txt file in the models/ directory")
            return False
        
        # Load class names
        with open(CLASSES_PATH, 'r') as f:
            class_names = [line.strip() for line in f.readlines()]
        
        print(f"📋 Loaded {len(class_names)} classes: {class_names}")
        
        # Generate random colors for each class
        colors = np.random.uniform(0, 255, size=(len(class_names), 3))
        
        # Load YOLOv3 network
        print("🔄 Loading YOLOv3 model...")
        net = cv2.dnn.readNetFromDarknet(CFG_PATH, WEIGHTS_PATH)
        
        # Use GPU if available (CUDA)
        if cv2.cuda.getCudaEnabledDeviceCount() > 0:
            net.setPreferableBackend(cv2.dnn.DNN_BACKEND_CUDA)
            net.setPreferableTarget(cv2.dnn.DNN_TARGET_CUDA)
            print("🚀 Using GPU acceleration (CUDA)")
        else:
            net.setPreferableBackend(cv2.dnn.DNN_BACKEND_OPENCV)
            net.setPreferableTarget(cv2.dnn.DNN_TARGET_CPU)
            print("💻 Using CPU (Install CUDA for GPU acceleration)")
        
        # Get output layer names
        layer_names = net.getLayerNames()
        output_layers = [layer_names[i - 1] for i in net.getUnconnectedOutLayers()]
        
        print("✅ Custom YOLOv3 model loaded successfully!")
        print(f"📊 Model info:")
        print(f"   - Config: {CFG_PATH}")
        print(f"   - Weights: {WEIGHTS_PATH}")
        print(f"   - Classes: {len(class_names)}")
        print(f"   - Confidence threshold: {CONFIDENCE_THRESHOLD}")
        print(f"   - NMS threshold: {NMS_THRESHOLD}")
        return True
        
    except Exception as e:
        print(f"❌ Error loading model: {e}")
        import traceback
        traceback.print_exc()
        return False

@app.on_event("startup")
async def startup_event():
    """Load model on startup"""
    print("🚀 Starting YOLOv3 Detection Service...")
    load_yolov3_model()

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": "YOLOv3 Detection Service",
        "status": "running",
        "model_loaded": net is not None,
        "version": "1.0.0",
        "endpoints": ["/health", "/detect", "/quality-check", "/batch-detect"]
    }

@app.get("/health")
async def health_check():
    """Detailed health check"""
    return {
        "status": "healthy" if net is not None else "model_not_loaded",
        "model": "Custom YOLOv3",
        "classes": class_names if class_names else [],
        "total_classes": len(class_names) if class_names else 0,
        "backend": "OpenCV DNN with CUDA" if cv2.cuda.getCudaEnabledDeviceCount() > 0 else "OpenCV DNN (CPU)",
        "config": {
            "confidence_threshold": CONFIDENCE_THRESHOLD,
            "nms_threshold": NMS_THRESHOLD
        },
        "ready": net is not None
    }

def detect_objects_in_image(img):
    """
    Perform object detection on an image using custom YOLOv3
    """
    height, width, channels = img.shape
    
    # Prepare image for YOLOv3
    blob = cv2.dnn.blobFromImage(img, 1/255.0, (416, 416), swapRB=True, crop=False)
    net.setInput(blob)
    
    # Forward pass
    layer_outputs = net.forward(output_layers)
    
    # Parse detections
    class_ids = []
    confidences = []
    boxes = []
    
    for output in layer_outputs:
        for detection in output:
            scores = detection[5:]
            class_id = np.argmax(scores)
            confidence = scores[class_id]
            
            if confidence > CONFIDENCE_THRESHOLD:
                # Scale bounding box coordinates back to original image size
                center_x = int(detection[0] * width)
                center_y = int(detection[1] * height)
                w = int(detection[2] * width)
                h = int(detection[3] * height)
                
                # Calculate top-left corner
                x = int(center_x - w / 2)
                y = int(center_y - h / 2)
                
                boxes.append([x, y, w, h])
                confidences.append(float(confidence))
                class_ids.append(class_id)
    
    # Apply Non-Maximum Suppression
    indices = cv2.dnn.NMSBoxes(boxes, confidences, CONFIDENCE_THRESHOLD, NMS_THRESHOLD)
    
    detections = []
    if len(indices) > 0:
        for i in indices.flatten():
            x, y, w, h = boxes[i]
            
            detection = {
                "class": class_names[class_ids[i]],
                "class_id": int(class_ids[i]),
                "confidence": round(confidences[i], 3),
                "bbox": {
                    "x1": int(x),
                    "y1": int(y),
                    "x2": int(x + w),
                    "y2": int(y + h),
                    "width": int(w),
                    "height": int(h)
                },
                "center": {
                    "x": int(x + w / 2),
                    "y": int(y + h / 2)
                }
            }
            detections.append(detection)
    
    return detections

def draw_detections(img, detections):
    """
    Draw bounding boxes and labels on image
    """
    img_with_boxes = img.copy()
    
    for det in detections:
        x1 = det["bbox"]["x1"]
        y1 = det["bbox"]["y1"]
        x2 = det["bbox"]["x2"]
        y2 = det["bbox"]["y2"]
        class_name = det["class"]
        confidence = det["confidence"]
        class_id = det["class_id"]
        
        # Get color for this class
        color = colors[class_id].tolist()
        
        # Draw rectangle
        cv2.rectangle(img_with_boxes, (x1, y1), (x2, y2), color, 2)
        
        # Draw label background
        label = f"{class_name}: {confidence:.2f}"
        (label_width, label_height), baseline = cv2.getTextSize(
            label, cv2.FONT_HERSHEY_SIMPLEX, 0.5, 1
        )
        cv2.rectangle(
            img_with_boxes,
            (x1, y1 - label_height - 10),
            (x1 + label_width, y1),
            color,
            -1
        )
        
        # Draw label text
        cv2.putText(
            img_with_boxes,
            label,
            (x1, y1 - 5),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.5,
            (0, 0, 0),
            1
        )
    
    return img_with_boxes

@app.post("/detect")
async def detect_objects(file: UploadFile = File(...)):
    """
    Detect objects in uploaded image using custom YOLOv3
    Returns: Detection results with bounding boxes, confidence, and classes
    """
    if net is None:
        raise HTTPException(status_code=503, detail="Model not loaded. Check server logs.")
    
    try:
        # Read image file
        contents = await file.read()
        nparr = np.frombuffer(contents, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if img is None:
            raise HTTPException(status_code=400, detail="Invalid image file")
        
        # Perform detection
        detections = detect_objects_in_image(img)
        
        # Draw bounding boxes
        img_with_boxes = draw_detections(img, detections)
        
        # Convert to base64 for sending to frontend
        _, buffer = cv2.imencode('.jpg', img_with_boxes)
        img_base64 = base64.b64encode(buffer).decode('utf-8')
        
        return JSONResponse({
            "success": True,
            "detections": detections,
            "count": len(detections),
            "image_with_boxes": img_base64,
            "image_size": {
                "width": img.shape[1],
                "height": img.shape[0]
            },
            "model_info": {
                "classes": len(class_names),
                "confidence_threshold": CONFIDENCE_THRESHOLD,
                "nms_threshold": NMS_THRESHOLD
            }
        })
        
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Detection failed: {str(e)}")

@app.post("/batch-detect")
async def batch_detect(files: List[UploadFile] = File(...)):
    """
    Detect objects in multiple images
    """
    if net is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    
    results = []
    for file in files:
        try:
            contents = await file.read()
            nparr = np.frombuffer(contents, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            
            if img is not None:
                detections = detect_objects_in_image(img)
                
                results.append({
                    "filename": file.filename,
                    "detections": detections,
                    "count": len(detections)
                })
        except Exception as e:
            results.append({
                "filename": file.filename,
                "error": str(e)
            })
    
    return JSONResponse({
        "success": True,
        "total_images": len(files),
        "results": results
    })

@app.post("/quality-check")
async def quality_check(file: UploadFile = File(...)):
    """
    Specialized endpoint for quality inspection
    Detects defects, damages, or quality issues in agricultural products
    """
    if net is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    
    try:
        contents = await file.read()
        nparr = np.frombuffer(contents, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        # Perform detection
        detections = detect_objects_in_image(img)
        
        # Analyze quality based on detected classes
        quality_score = 100
        issues = []
        
        for det in detections:
            cls_name = det["class"].lower()
            confidence = det["confidence"]
            
            # Check for quality issues (customize based on your classes)
            if any(keyword in cls_name for keyword in ['damaged', 'rotten', 'spoiled', 'bad']):
                quality_score -= 30
                issues.append({
                    "type": cls_name,
                    "severity": "high",
                    "confidence": confidence,
                    "location": det["center"]
                })
            elif any(keyword in cls_name for keyword in ['defect', 'bruised', 'blemish']):
                quality_score -= 20
                issues.append({
                    "type": cls_name,
                    "severity": "medium",
                    "confidence": confidence,
                    "location": det["center"]
                })
            elif any(keyword in cls_name for keyword in ['minor', 'slight']):
                quality_score -= 10
                issues.append({
                    "type": cls_name,
                    "severity": "low",
                    "confidence": confidence,
                    "location": det["center"]
                })
        
        quality_score = max(0, quality_score)
        
        # Draw quality check results
        img_with_boxes = draw_detections(img, detections)
        _, buffer = cv2.imencode('.jpg', img_with_boxes)
        img_base64 = base64.b64encode(buffer).decode('utf-8')
        
        return JSONResponse({
            "success": True,
            "quality_score": quality_score,
            "grade": "A" if quality_score >= 90 else "B" if quality_score >= 70 else "C" if quality_score >= 50 else "D",
            "issues": issues,
            "detections": detections,
            "status": "approved" if quality_score >= 70 else "rejected",
            "image_with_boxes": img_base64
        })
        
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Quality check failed: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000, reload=True)
