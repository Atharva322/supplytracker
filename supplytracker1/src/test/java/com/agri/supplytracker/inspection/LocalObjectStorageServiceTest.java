package com.agri.supplytracker.inspection;

import com.agri.supplytracker.inspection.application.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalObjectStorageServiceTest {
    @TempDir Path tempDir;

    @Test
    void storeAndReadPreservesChecksumAndRejectsPathTraversal() {
        LocalObjectStorageService storage = new LocalObjectStorageService(1024, "/uploads", tempDir.toString());

        ObjectStorageService.StoredObject stored = storage.store("inspections/alice/image.jpg", new byte[]{1, 2, 3}, "image/jpeg");
        ObjectStorageService.StoredObject read = storage.read("inspections/alice/image.jpg");

        assertEquals(stored.checksum(), read.checksum());
        assertArrayEquals(new byte[]{1, 2, 3}, read.bytes());
        assertThrows(IllegalArgumentException.class, () -> storage.store("../escape.jpg", new byte[]{1}, "image/jpeg"));
    }

    @Test
    void rejectsInvalidContentTypeAndOversizedUpload() {
        LocalObjectStorageService storage = new LocalObjectStorageService(2, "/uploads", tempDir.toString());

        assertThrows(IllegalArgumentException.class, () -> storage.store("inspections/a.txt", new byte[]{1}, "text/plain"));
        assertThrows(IllegalArgumentException.class, () -> storage.store("inspections/a.jpg", new byte[]{1, 2, 3}, "image/jpeg"));
    }
}
