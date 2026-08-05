package com.agri.supplytracker.controller;

import com.agri.supplytracker.model.Product;
import com.agri.supplytracker.model.TrackingStage;
import com.agri.supplytracker.dto.*;
import com.agri.supplytracker.service.ProductQueryService;
import com.agri.supplytracker.service.ProductService;
import com.agri.supplytracker.service.ProductMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService products;
    private final ProductQueryService queries;
    private final ProductMapper mapper;

    public ProductController(ProductService products, ProductQueryService queries, ProductMapper mapper) {
        this.products = products;
        this.queries = queries;
        this.mapper = mapper;
    }

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats() {
        return queries.dashboardStats();
    }

    @GetMapping
    public Map<String, Object> getAllProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "name") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir) {
        Page<Product> result = queries.page(page, size, sortBy, sortDir);
        return Map.of(
            "products", result.getContent().stream().map(mapper::toResponse).toList(),
            "currentPage", result.getNumber(),
            "totalItems", result.getTotalElements(),
            "totalPages", result.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable String id) {
        return mapper.toResponse(products.get(id));
    }

    @GetMapping("/search")
    public List<ProductResponse> searchProducts(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String batchId,
        @RequestParam(required = false) String originFarmId) {
        return queries.search(name, type, batchId, originFarmId).stream().map(mapper::toResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductWriteRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(products.create(mapper.toDomain(request), auth.getName())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable String id, @Valid @RequestBody ProductWriteRequest request, Authentication auth) {
        return mapper.toResponse(products.replace(id, mapper.toDomain(request), auth.getName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ProductResponse patchProduct(@PathVariable String id, @RequestBody Map<String, Object> updates, Authentication auth) {
        return mapper.toResponse(products.patch(id, updates, auth.getName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        products.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FARMER', 'PROCESSOR', 'WAREHOUSE_MANAGER', 'DISTRIBUTOR', 'RETAILER')")
    @PostMapping("/{id}/tracking")
    public ProductResponse addTrackingStage(@PathVariable String id, @Valid @RequestBody TrackingStage stage, Authentication auth) {
        assertStagePermission(stage, auth);
        return mapper.toResponse(products.addTrackingStage(id, stage, auth.getName()));
    }

    @GetMapping("/{id}/tracking")
    public List<TrackingStage> getTrackingHistory(@PathVariable String id) {
        return products.get(id).getTrackingHistory();
    }

    private void assertStagePermission(TrackingStage stage, Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (admin) return;
        String value = stage.getStage() == null ? "" : stage.getStage();
        boolean allowed = auth.getAuthorities().stream().anyMatch(authority -> switch (authority.getAuthority()) {
            case "ROLE_FARMER" -> value.equalsIgnoreCase("Farm");
            case "ROLE_PROCESSOR" -> value.equalsIgnoreCase("Processing") || value.equalsIgnoreCase("Quality Check");
            case "ROLE_WAREHOUSE_MANAGER" -> value.equalsIgnoreCase("Warehouse");
            case "ROLE_DISTRIBUTOR" -> value.equalsIgnoreCase("Distribution");
            case "ROLE_RETAILER" -> value.equalsIgnoreCase("Retail");
            default -> false;
        });
        if (!allowed) throw new org.springframework.security.access.AccessDeniedException("Not authorized for this tracking stage");
    }
}
