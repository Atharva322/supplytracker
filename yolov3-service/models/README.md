# YOLOv3 Custom Model Files

Place your custom trained YOLOv3 model files in this directory:

## Required Files

### 1. yolov3.cfg
Your YOLOv3 configuration file containing:
- Network architecture
- Layer definitions
- Input size (typically 416x416 or 608x608)
- Number of classes

### 2. yolov3.weights
Your trained model weights file
- Contains the learned parameters
- Generated from your training process

### 3. classes.txt
**IMPORTANT**: You need to create this file with your class names.

Format: One class name per line, matching the order used during training.

Example `classes.txt`:
```
fresh_tomato
rotten_tomato
fresh_potato
damaged_potato
fresh_apple
damaged_apple
```

## File Structure
```
models/
├── yolov3.cfg          (your config file)
├── yolov3.weights      (your weights file)
├── classes.txt         (create this - one class per line)
└── README.md          (this file)
```

## How to Get classes.txt

If you trained your model using:

### Darknet/AlexeyAB
- Your classes were defined in `obj.names` or `coco.names`
- Copy that file and rename it to `classes.txt`

### Custom Training
- List all your class names in the exact order used during training
- One class per line
- No empty lines
- Example:
  ```
  tomato
  potato
  onion
  carrot
  ```

## Testing Your Model

After placing all three files, start the service:
```bash
cd yolov3-service
.\start.ps1
```

Check the logs to verify model loaded successfully:
- ✅ Should see: "Custom YOLOv3 model loaded successfully!"
- Should show number of classes loaded
- Shows GPU/CPU usage

Test with curl:
```bash
curl http://localhost:8000/health
```

Expected response:
```json
{
  "status": "healthy",
  "model": "Custom YOLOv3",
  "classes": ["your", "class", "names"],
  "backend": "OpenCV DNN",
  "version": "1.0.0"
}
```
