package com.agri.supplytracker.controller;

import com.agri.supplytracker.exception.ProductNotFoundException;
import com.agri.supplytracker.model.Product;
import com.agri.supplytracker.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
@CrossOrigin(origins = "http://localhost:5173") // reac de runs on diff origin than 8080
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repository;
    private final ProductStreamController streamController;

    @Autowired
    public ProductController(ProductRepository repository, ProductStreamController streamController) {
        this.repository = repository;
        this.streamController = streamController;
    }

    // GET dashboard statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        List<Product> allProducts = repository.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        
        // Total products
        stats.put("totalProducts", allProducts.size());
        
        // Count unique product types
        long uniqueTypes = allProducts.stream()
            .map(Product::getType)
            .distinct()
            .count();
        stats.put("uniqueTypes", uniqueTypes);
        
        // Count unique farms
        long uniqueFarms = allProducts.stream()
            .map(Product::getOriginFarmId)
            .distinct()
            .count();
        stats.put("uniqueFarms", uniqueFarms);
        
        // Products by type
        Map<String, Long> productsByType = allProducts.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Product::getType,
                java.util.stream.Collectors.counting()
            ));
        stats.put("productsByType", productsByType);
        
        // Recent products (last 5)
        List<Product> recentProducts = allProducts.stream()
            .sorted((p1, p2) -> p2.getHarvestDate().compareTo(p1.getHarvestDate()))
            .limit(5)
            .toList();
        stats.put("recentProducts", recentProducts);
        
        // Total tracking stages
        long totalTrackingStages = allProducts.stream()
            .mapToLong(p -> p.getTrackingHistory() != null ? p.getTrackingHistory().size() : 0)
            .sum();
        stats.put("totalTrackingStages", totalTrackingStages);
        
        return ResponseEntity.ok(stats);
    }

    // GET all products with pagination
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = repository.findAll(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", productPage.getContent());
        response.put("currentPage", productPage.getNumber());
        response.put("totalItems", productPage.getTotalElements());
        response.put("totalPages", productPage.getTotalPages());
        
        return ResponseEntity.ok(response);
    }

    // GET by id
    @Cacheable(value = "products", key = "#id")
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    // GET /api/products/search?name=Mango&type=Fruit&originFarmId=FARM001 ...
@GetMapping("/search")
public List<Product> searchProducts(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String batchId,
        @RequestParam(required = false) String originFarmId) {

    // Start with all products
    List<Product> results = repository.findAll();

    if (name != null && !name.isBlank()) {
        results = results.stream()
                .filter(p -> p.getName() != null &&
                             p.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }

    if (type != null && !type.isBlank()) {
        results = results.stream()
                .filter(p -> type.equalsIgnoreCase(p.getType()))
                .toList();
    }

    if (batchId != null && !batchId.isBlank()) {
        results = results.stream()
                .filter(p -> batchId.equalsIgnoreCase(p.getBatchId()))
                .toList();
    }

    if (originFarmId != null && !originFarmId.isBlank()) {
        results = results.stream()
                .filter(p -> originFarmId.equalsIgnoreCase(p.getOriginFarmId()))
                .toList();
    }

    return results;
}


    // POST create with validation (Admin only)
    @CacheEvict(value = "products", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody Product product) {
        try {
            Product savedProduct = repository.save(product);
            // Broadcast to SSE subscribers
            streamController.broadcastProductUpdate(savedProduct);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create product");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // PUT full update with validation (Admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable String id,
                                 @Valid @RequestBody Product updatedProduct) {

        return repository.findById(id)
                .map(existing -> {
                    existing.setName(updatedProduct.getName());
                    existing.setType(updatedProduct.getType());
                    existing.setBatchId(updatedProduct.getBatchId());
                    existing.setHarvestDate(updatedProduct.getHarvestDate());
                    existing.setOriginFarmId(updatedProduct.getOriginFarmId());
                    Product saved = repository.save(existing);
                    // Broadcast to SSE subscribers
                    streamController.broadcastProductUpdate(saved);
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // PATCH partial update (Admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public Product patchProduct(@PathVariable String id,
                                @RequestBody Map<String, Object> updates) {

        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (updates.containsKey("name")) {
            product.setName((String) updates.get("name"));
        }
        if (updates.containsKey("type")) {
            product.setType((String) updates.get("type"));
        }
        if (updates.containsKey("batchId")) {
            product.setBatchId((String) updates.get("batchId"));
        }
        if (updates.containsKey("harvestDate")) {
            product.setHarvestDate((String) updates.get("harvestDate"));
        }
        if (updates.containsKey("originFarmId")) {
            product.setOriginFarmId((String) updates.get("originFarmId"));
        }

        Product saved = repository.save(product);
        // Broadcast to SSE subscribers
        streamController.broadcastProductUpdate(saved);
        return saved;
    }

    // DELETE product (Admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        // If product doesn't exist → 404
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        // Delete and return 204 No Content
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // POST add tracking stage to a product
    // Permissions: ADMIN or stage-specific roles (FARMER for Farm, PROCESSOR for Processing, etc.)
    @PreAuthorize("hasAnyRole('ADMIN', 'FARMER', 'PROCESSOR', 'WAREHOUSE_MANAGER', 'DISTRIBUTOR', 'RETAILER')")
    @PostMapping("/{id}/tracking")
    public ResponseEntity<?> addTrackingStage(@PathVariable String id, 
                                               @Valid @RequestBody com.agri.supplytracker.model.TrackingStage trackingStage,
                                               Authentication authentication) {
        // Validate user can add this specific stage based on their role
        String userRole = authentication.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .filter(auth -> auth.startsWith("ROLE_"))
            .findFirst()
            .orElse("");
        
        // Check if user's role matches the stage they're trying to add
        if (!userRole.equals("ROLE_ADMIN")) {
            boolean authorized = false;
            String stage = trackingStage.getStage();
            
            if (userRole.equals("ROLE_FARMER") && stage.equalsIgnoreCase("Farm")) authorized = true;
            else if (userRole.equals("ROLE_PROCESSOR") && (stage.equalsIgnoreCase("Processing") || stage.equalsIgnoreCase("Quality Check"))) authorized = true;
            else if (userRole.equals("ROLE_WAREHOUSE_MANAGER") && stage.equalsIgnoreCase("Warehouse")) authorized = true;
            else if (userRole.equals("ROLE_DISTRIBUTOR") && stage.equalsIgnoreCase("Distribution")) authorized = true;
            else if (userRole.equals("ROLE_RETAILER") && stage.equalsIgnoreCase("Retail")) authorized = true;
            
            if (!authorized) {
                return ResponseEntity.status(403).body(Map.of("error", "You are not authorized to add this tracking stage"));
            }
        }
        
        return repository.findById(id)
                .map(product -> {
                    // Set timestamp if not provided
                    if (trackingStage.getTimestamp() == null) {
                        trackingStage.setTimestamp(java.time.LocalDateTime.now());
                    }
                    
                    // Add tracking stage to history
                    product.getTrackingHistory().add(trackingStage);
                    
                    // Update product's current location and status
                    product.setCurrentLocation(trackingStage.getLocation());
                    product.setStatus(trackingStage.getStage());
                    
                    Product saved = repository.save(product);
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // GET tracking history for a product
    @GetMapping("/{id}/tracking")
    public ResponseEntity<?> getTrackingHistory(@PathVariable String id) {
        return repository.findById(id)
                .map(product -> ResponseEntity.ok(product.getTrackingHistory()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
