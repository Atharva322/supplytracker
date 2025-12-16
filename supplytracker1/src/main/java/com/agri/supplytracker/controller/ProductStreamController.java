package com.agri.supplytracker.controller;

import com.agri.supplytracker.model.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller for Server-Sent Events (SSE) streaming of product updates
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class ProductStreamController {

    private static final Logger log = LoggerFactory.getLogger(ProductStreamController.class);

    // List of active SSE emitters
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public ProductStreamController() {
        // Send heartbeat every 30 seconds to keep connections alive
        executor.scheduleAtFixedRate(this::sendHeartbeat, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * SSE endpoint for streaming product updates
     * Clients can connect to this endpoint to receive real-time product updates
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("permitAll()") // Allow all users to subscribe
    public SseEmitter streamProducts(@RequestParam(required = false) String token) {
        log.info("New SSE connection established. Total connections: {}", emitters.size() + 1);
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // No timeout
        
        emitters.add(emitter);
        
        // Remove emitter when completed or timed out
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((ex) -> emitters.remove(emitter));
        
        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                    .data("{\"type\":\"connected\",\"message\":\"Connected to product updates stream\"}"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    /**
     * Method to broadcast product updates to all connected clients
     * Call this when a product is created or updated
     */
    public void broadcastProductUpdate(Product product) {
        log.info("Broadcasting product update to {} subscribers: {}", emitters.size(), product.getName());
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        
        emitters.forEach(emitter -> {
            try {
                // Send as unnamed event to trigger onmessage handler
                emitter.send(SseEmitter.event()
                        .data(objectMapper.writeValueAsString(product)));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });
        
        // Remove dead emitters
        emitters.removeAll(deadEmitters);
    }

    /**
     * Send heartbeat to keep connections alive
     */
    private void sendHeartbeat() {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .comment("heartbeat"));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });
        
        emitters.removeAll(deadEmitters);
    }
}
