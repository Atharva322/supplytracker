package com.agri.supplytracker.inspection.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class LocalObjectStorageService implements ObjectStorageService {
    private final long maxSizeBytes;
    private final String uploadBaseUrl;
    private final Path root;

    public LocalObjectStorageService(@Value("${inspection.upload.max-size-bytes:10485760}") long maxSizeBytes,
                                     @Value("${inspection.upload.local-base-url:/api/v2/inspection-uploads}") String uploadBaseUrl,
                                     @Value("${inspection.upload.local-root:./data/inspection-uploads}") String localRoot) {
        this.maxSizeBytes = maxSizeBytes;
        this.uploadBaseUrl = uploadBaseUrl;
        this.root = Paths.get(localRoot).toAbsolutePath().normalize();
    }

    @Override
    public UploadSlot createUploadSlot(String actor, String filename, String contentType, long sizeBytes) {
        if (sizeBytes <= 0 || sizeBytes > maxSizeBytes) throw new IllegalArgumentException("Image exceeds the upload size limit");
        if (contentType == null || !contentType.startsWith("image/")) throw new IllegalArgumentException("Inspection input must be an image");
        String safeName = filename == null ? "image" : filename.replaceAll("[^A-Za-z0-9._-]", "_");
        String objectKey = "inspections/" + actor + "/" + UUID.randomUUID() + "/" + safeName;
        return new UploadSlot(objectKey, uploadBaseUrl + "/" + objectKey, Instant.now().plusSeconds(900), maxSizeBytes);
    }

    @Override
    public StoredObject store(String objectKey, byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("Upload body is empty");
        if (bytes.length > maxSizeBytes) throw new IllegalArgumentException("Image exceeds the upload size limit");
        if (contentType == null || !contentType.startsWith("image/")) throw new IllegalArgumentException("Inspection input must be an image");
        try {
            Path target = resolveObjectKey(objectKey);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            return new StoredObject(objectKey, bytes, contentType, checksum(bytes), bytes.length);
        } catch (FileAlreadyExistsException e) {
            throw new IllegalStateException("Object key already exists");
        } catch (IOException e) {
            throw new IllegalStateException("Could not store inspection object", e);
        }
    }

    @Override
    public StoredObject read(String objectKey) {
        try {
            Path target = resolveObjectKey(objectKey);
            byte[] bytes = Files.readAllBytes(target);
            return new StoredObject(objectKey, bytes, Files.probeContentType(target), checksum(bytes), bytes.length);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read inspection object", e);
        }
    }

    private Path resolveObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) throw new IllegalArgumentException("objectKey is required");
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Invalid object key");
        return target;
    }

    private String checksum(byte[] bytes) {
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
