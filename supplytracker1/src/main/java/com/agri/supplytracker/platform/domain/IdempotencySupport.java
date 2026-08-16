package com.agri.supplytracker.platform.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

public final class IdempotencySupport {
    private IdempotencySupport() {}

    public static String hash(Object... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String canonical = String.join("\u001f", Arrays.stream(parts)
                .map(part -> Objects.toString(part, ""))
                .toList());
            byte[] bytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static void requireSameRequest(IdempotencyRecord record, String requestHash) {
        if (record.getRequestHash() != null && !record.getRequestHash().equals(requestHash)) {
            throw new IllegalStateException("Idempotency key already used with a different payload");
        }
    }
}
