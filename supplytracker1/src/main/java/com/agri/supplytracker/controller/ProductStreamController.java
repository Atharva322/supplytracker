package com.agri.supplytracker.controller;

import com.agri.supplytracker.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Authenticated, user-scoped product event stream. */
@RestController
@RequestMapping("/api/products")
public class ProductStreamController {
    private static final Logger log = LoggerFactory.getLogger(ProductStreamController.class);
    private static final long TIMEOUT_MS = Duration.ofMinutes(5).toMillis();
    private final Map<String, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProducts(Authentication authentication) {
        String username = authentication.getName();
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emittersByUser.computeIfAbsent(username, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> remove(username, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("type", "connected")));
        } catch (IOException error) {
            emitter.completeWithError(error);
        }
        return emitter;
    }

    /** Sends only to the authenticated actor that initiated the compatible v1 write. */
    public void sendProductUpdateToUser(Product product, String username) {
        List<SseEmitter> emitters = emittersByUser.getOrDefault(username, List.of());
        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name("product-updated").data(Map.of(
                    "productId", product.getId(),
                    "type", "PRODUCT_UPDATED"
                )));
            } catch (IOException error) {
                remove(username, emitter);
            }
        }
    }

    private void remove(String username, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUser.get(username);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) emittersByUser.remove(username);
        }
        log.debug("Closed product stream for {}", username);
    }
}
