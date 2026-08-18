package com.agri.supplytracker.inspection.application;

import java.time.Instant;

public interface ObjectStorageService {
    UploadSlot createUploadSlot(String actor, String filename, String contentType, long sizeBytes);
    StoredObject store(String objectKey, byte[] bytes, String contentType);
    StoredObject read(String objectKey);

    record UploadSlot(String objectKey, String uploadUrl, Instant expiresAt, long maxSizeBytes) {}
    record StoredObject(String objectKey, byte[] bytes, String contentType, String checksum, long sizeBytes) {}
}
