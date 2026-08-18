package com.agri.supplytracker.inspection.api;

import com.agri.supplytracker.inspection.application.ObjectStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v2/inspection-uploads")
public class InspectionUploadController {
    private final ObjectStorageService storage;

    public InspectionUploadController(ObjectStorageService storage) { this.storage = storage; }

    @PutMapping(value = "/**", consumes = MediaType.ALL_VALUE)
    public UploadResponse upload(HttpServletRequest request, @RequestBody byte[] body) throws IOException {
        String prefix = "/api/v2/inspection-uploads/";
        String uri = request.getRequestURI();
        String objectKey = uri.startsWith(prefix) ? uri.substring(prefix.length()) : uri;
        String contentType = request.getContentType();
        ObjectStorageService.StoredObject stored = storage.store(objectKey, body, contentType);
        return new UploadResponse(stored.objectKey(), stored.contentType(), stored.checksum(), stored.sizeBytes());
    }

    public record UploadResponse(String objectKey, String contentType, String checksum, long sizeBytes) {}
}
