package com.agri.supplytracker.inspection.application;

import com.agri.supplytracker.inspection.domain.InspectionJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class YoloInspectionInferenceClient implements InspectionInferenceClient {
    private final RestTemplate restTemplate;
    private final String yoloServiceUrl;

    public YoloInspectionInferenceClient(@Value("${yolo.service.url:http://localhost:8000}") String yoloServiceUrl,
                                         @Value("${inspection.inference.connect-timeout-ms:5000}") int connectTimeoutMs,
                                         @Value("${inspection.inference.read-timeout-ms:60000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(factory);
        this.yoloServiceUrl = yoloServiceUrl;
    }

    @Override
    public InferenceResult analyze(InspectionJob job, byte[] imageBytes) {
        long started = System.nanoTime();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(imageBytes, job.getObjectKey()));
        ResponseEntity<Map> response = restTemplate.postForEntity(yoloServiceUrl + "/detect", new HttpEntity<>(body, headers), Map.class);
        Map<?, ?> result = Optional.ofNullable(response.getBody()).orElse(Map.of());
        List<String> labels = labels(result);
        double confidence = confidence(result);
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("success", Objects.toString(result.get("success"), ""));
        raw.put("count", Objects.toString(result.get("count"), "0"));
        raw.put("source", "yolo-http");
        return new InferenceResult(labels, raw, confidence, (System.nanoTime() - started) / 1_000_000);
    }

    private List<String> labels(Map<?, ?> result) {
        Object detections = result.get("detections");
        if (!(detections instanceof List<?> list)) return List.of("Unknown Product");
        List<String> labels = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> detection && detection.get("class") != null) labels.add(detection.get("class").toString());
        }
        return labels.isEmpty() ? List.of("Unknown Product") : labels;
    }

    private double confidence(Map<?, ?> result) {
        Object detections = result.get("detections");
        if (!(detections instanceof List<?> list) || list.isEmpty()) return 0.0;
        double max = 0.0;
        for (Object item : list) {
            if (item instanceof Map<?, ?> detection && detection.get("confidence") instanceof Number value) {
                max = Math.max(max, value.doubleValue());
            }
        }
        return max;
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;
        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename == null ? "inspection-image" : filename;
        }
        @Override public String getFilename() { return filename; }
    }
}
