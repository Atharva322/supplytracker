package com.agri.supplytracker.graphql;

import com.agri.supplytracker.controller.ProductStreamController;
import com.agri.supplytracker.model.Product;
import com.agri.supplytracker.model.TrackingStage;
import com.agri.supplytracker.repository.ProductRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class ProductGraphQLController {

    private final ProductRepository productRepository;
    private final ProductStreamController streamController;
    private final Sinks.Many<Product> productSink;
    private final Sinks.Many<Product> productCreatedSink;
    private final Sinks.Many<ProductStatusUpdate> statusUpdateSink;

    public ProductGraphQLController(ProductRepository productRepository, ProductStreamController streamController) {
        this.productRepository = productRepository;
        this.streamController = streamController;
        this.productSink = Sinks.many().multicast().onBackpressureBuffer();
        this.productCreatedSink = Sinks.many().multicast().onBackpressureBuffer();
        this.statusUpdateSink = Sinks.many().multicast().onBackpressureBuffer();
    }

    // ==================== QUERIES ====================

    @QueryMapping
    public List<Product> products() {
        return productRepository.findAll();
    }

    @QueryMapping
    public Product product(@Argument String id) {
        return productRepository.findById(id).orElse(null);
    }

    @QueryMapping
    public List<Product> searchProducts(@Argument String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return productRepository.findAll().stream()
                .filter(p -> p.getName().toLowerCase().contains(lowerKeyword) ||
                           p.getType().toLowerCase().contains(lowerKeyword))
                .toList();
    }

    @QueryMapping
    public List<Product> productsByStatus(@Argument String status) {
        return productRepository.findAll().stream()
                .filter(p -> status.equalsIgnoreCase(p.getStatus()))
                .toList();
    }

    @QueryMapping
    public List<Product> productsByFarm(@Argument String farmId) {
        return productRepository.findAll().stream()
                .filter(p -> farmId.equals(p.getOriginFarmId()))
                .toList();
    }

    // ==================== MUTATIONS ====================

    @MutationMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public Product createProduct(@Argument Map<String, Object> input) {
        Product product = Product.builder()
                .name((String) input.get("name"))
                .type((String) input.get("type"))
                .batchId((String) input.get("batchId"))
                .harvestDate((String) input.get("harvestDate"))
                .originFarmId((String) input.get("originFarmId"))
                .originFarmName((String) input.get("originFarmName"))
                .currentLocation((String) input.get("currentLocation"))
                .destination((String) input.get("destination"))
                .status((String) input.getOrDefault("status", "AT_FARM"))
                .trackingHistory(new ArrayList<>())
                .build();

        Product savedProduct = productRepository.save(product);
        
        // Emit subscription event
        productCreatedSink.tryEmitNext(savedProduct);
        
        // Broadcast to SSE subscribers
        streamController.broadcastProductUpdate(savedProduct);
        
        return savedProduct;
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public Product updateProduct(@Argument String id, @Argument Map<String, Object> input) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (input.containsKey("name")) product.setName((String) input.get("name"));
        if (input.containsKey("type")) product.setType((String) input.get("type"));
        if (input.containsKey("batchId")) product.setBatchId((String) input.get("batchId"));
        if (input.containsKey("harvestDate")) product.setHarvestDate((String) input.get("harvestDate"));
        if (input.containsKey("originFarmId")) product.setOriginFarmId((String) input.get("originFarmId"));
        if (input.containsKey("originFarmName")) product.setOriginFarmName((String) input.get("originFarmName"));
        if (input.containsKey("currentLocation")) product.setCurrentLocation((String) input.get("currentLocation"));
        if (input.containsKey("destination")) product.setDestination((String) input.get("destination"));
        if (input.containsKey("status")) product.setStatus((String) input.get("status"));

        Product updatedProduct = productRepository.save(product);
        
        // Emit subscription event
        productSink.tryEmitNext(updatedProduct);
        
        // Broadcast to SSE subscribers
        streamController.broadcastProductUpdate(updatedProduct);
        
        return updatedProduct;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public boolean deleteProduct(@Argument String id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public Product addTrackingStage(@Argument String productId, @Argument Map<String, Object> stage) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        // Parse timestamp string to LocalDateTime if provided, otherwise use current time
        LocalDateTime timestamp = LocalDateTime.now();
        if (stage.containsKey("timestamp")) {
            String timestampStr = (String) stage.get("timestamp");
            if (timestampStr != null && !timestampStr.isEmpty()) {
                timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME);
            }
        }

        TrackingStage trackingStage = TrackingStage.builder()
                .stage((String) stage.get("stage"))
                .location((String) stage.get("location"))
                .timestamp(timestamp)
                .notes((String) stage.get("description"))
                .handler((String) stage.get("handledBy"))
                .build();

        product.getTrackingHistory().add(trackingStage);
        Product updatedProduct = productRepository.save(product);
        
        // Emit subscription event
        productSink.tryEmitNext(updatedProduct);
        
        // Broadcast to SSE subscribers
        streamController.broadcastProductUpdate(updatedProduct);
        
        return updatedProduct;
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public Product updateProductStatus(@Argument String id, @Argument String status, @Argument String location) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        String oldStatus = product.getStatus();
        product.setStatus(status);
        
        if (location != null) {
            product.setCurrentLocation(location);
        }

        Product updatedProduct = productRepository.save(product);
        
        // Emit subscription events
        productSink.tryEmitNext(updatedProduct);
        
        // Broadcast to SSE subscribers
        streamController.broadcastProductUpdate(updatedProduct);
        
        ProductStatusUpdate statusUpdate = new ProductStatusUpdate(
                id,
                oldStatus,
                status,
                location,
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        );
        statusUpdateSink.tryEmitNext(statusUpdate);
        
        return updatedProduct;
    }

    // ==================== SUBSCRIPTIONS ====================

    @SubscriptionMapping
    public Flux<Product> productUpdated() {
        return productSink.asFlux();
    }

    @SubscriptionMapping
    public Flux<Product> productCreated() {
        return productCreatedSink.asFlux();
    }

    @SubscriptionMapping
    public Flux<ProductStatusUpdate> productStatusChanged(@Argument String productId) {
        if (productId != null) {
            return statusUpdateSink.asFlux()
                    .filter(update -> productId.equals(update.productId()));
        }
        return statusUpdateSink.asFlux();
    }

    // Inner class for status updates
    public record ProductStatusUpdate(
            String productId,
            String oldStatus,
            String newStatus,
            String location,
            String timestamp
    ) {}
}
