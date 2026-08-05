package com.agri.supplytracker.controller;

import com.agri.supplytracker.service.ClassifierService;
import com.agri.supplytracker.service.BedrockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/detection")
public class DetectionController {

    @Value("${yolo.service.url:http://localhost:8000}")
    private String yoloServiceUrl;

    private final RestTemplate restTemplate;

    public DetectionController() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000); // 10s connect
        factory.setReadTimeout(60_000);    // 60s read (YOLO inference on CPU)
        this.restTemplate = new RestTemplate(factory);
    }
    
    @Autowired
    private ClassifierService classifierService;
    
    @Autowired
    private BedrockService bedrockService;

    @PostMapping("/detect")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> detectObjects(
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "File is empty"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "File must be an image"));
            }

            // Prepare multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = 
                new HttpEntity<>(body, headers);

            // Call Python YOLOv3 service
            ResponseEntity<Map> response = restTemplate.postForEntity(
                yoloServiceUrl + "/detect",
                requestEntity,
                Map.class
            );

            Map<String, Object> yoloResult = response.getBody();
            
            // Extract labels from YOLO response
            List<String> labels = extractLabels(yoloResult);
            
            // Add classification using ClassifierService
            String classification = classifierService.classifyProduct(labels);
            yoloResult.put("classification", classification);
            
            // Add AI description using Bedrock
            try {
                String description = bedrockService.generateImageDescription(labels);
                yoloResult.put("aiDescription", description);
            } catch (Exception e) {
                yoloResult.put("aiDescription", "Unable to generate description: " + e.getMessage());
            }

            return ResponseEntity.ok(yoloResult);

        } catch (RestClientResponseException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Image could not be analyzed. Please use a clear, well-lit photo.");
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Detection failed: " + e.getMessage());
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/quality-check")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> qualityCheck(
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "File is empty"));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                yoloServiceUrl + "/quality-check",
                requestEntity,
                Map.class
            );

            Map<String, Object> qualityResult = response.getBody();

            List<String> labels = extractLabels(qualityResult);

            String classification = classifierService.classifyProduct(labels);
            qualityResult.put("classification", classification);

            try {
                String description = bedrockService.generateImageDescription(labels);
                qualityResult.put("aiDescription", description);
            } catch (Exception e) {
                qualityResult.put("aiDescription", "Unable to generate description: " + e.getMessage());
            }

            return ResponseEntity.ok(qualityResult);

        } catch (RestClientResponseException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Image could not be analyzed. Please use a clear, well-lit photo.");
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Quality check failed: " + e.getMessage());
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeImage(
            @RequestParam("image") MultipartFile image) {
        
        try {
            // Validate file
            if (image.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "File is empty"));
            }

            // Validate file type
            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "File must be an image"));
            }

            // Prepare multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(image));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = 
                new HttpEntity<>(body, headers);

            // Call Python YOLO service
            ResponseEntity<Map> response = restTemplate.postForEntity(
                yoloServiceUrl + "/detect",
                requestEntity,
                Map.class
            );

            Map<String, Object> yoloResult = response.getBody();
            
            // Extract labels from YOLO response
            List<String> labels = extractLabels(yoloResult);
            
            // Add classification using ClassifierService
            String classification = classifierService.classifyProduct(labels);
            
            // Add AI description using Bedrock
            String description;
            try {
                description = bedrockService.generateImageDescription(labels);
            } catch (Exception e) {
                description = "Unable to generate description: " + e.getMessage();
            }
            
            // Build response
            Map<String, Object> result = new HashMap<>();
            result.put("imageUrl", yoloResult.get("imageUrl"));
            result.put("labels", labels);
            result.put("classification", classification);
            result.put("aiDescription", description);
            result.put("status", "success");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", e.getMessage());
            error.put("status", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    // Helper method to extract labels from YOLO response
    private List<String> extractLabels(Map<String, Object> yoloResult) {
        List<String> labels = new ArrayList<>();
        
        if (yoloResult != null && yoloResult.containsKey("detections")) {
            Object detectionsObj = yoloResult.get("detections");
            if (detectionsObj instanceof List) {
                List<?> detections = (List<?>) detectionsObj;
                for (Object detection : detections) {
                    if (detection instanceof Map) {
                        Map<?, ?> detectionMap = (Map<?, ?>) detection;
                        if (detectionMap.containsKey("class")) {
                            labels.add(detectionMap.get("class").toString());
                        }
                    }
                }
            }
        }
        
        // Fallback: if no detections, use a default label
        if (labels.isEmpty()) {
            labels.add("Unknown Product");
        }
        
        return labels;
    }

    // Helper class for multipart file handling
    private static class MultipartFileResource extends ByteArrayResource {
        private final String filename;

        public MultipartFileResource(MultipartFile multipartFile) throws IOException {
            super(multipartFile.getBytes());
            this.filename = multipartFile.getOriginalFilename();
        }

        @Override
        public String getFilename() {
            return this.filename;
        }
    }
}
