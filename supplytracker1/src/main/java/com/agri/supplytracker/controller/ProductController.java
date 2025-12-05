package com.agri.supplytracker.controller;

import com.agri.supplytracker.exception.ProductNotFoundException;
import com.agri.supplytracker.model.Product;
import com.agri.supplytracker.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;
@CrossOrigin(origins = "http://localhost:5173") // reac de runs on diff origin than 8080
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repository;

    @Autowired
    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    // GET all products
    @GetMapping
    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    // GET by id
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


    // POST create
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@RequestBody Product product) {
        return repository.save(product);
    }

    // PUT full update
    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable String id,
                                 @RequestBody Product updatedProduct) {

        return repository.findById(id)
                .map(existing -> {
                    existing.setName(updatedProduct.getName());
                    existing.setType(updatedProduct.getType());
                    existing.setBatchId(updatedProduct.getBatchId());
                    existing.setHarvestDate(updatedProduct.getHarvestDate());
                    existing.setOriginFarmId(updatedProduct.getOriginFarmId());
                    return repository.save(existing);
                })
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    // PATCH partial update
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

        return repository.save(product);
    }

    // DELETE product
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable String id) {
        if (!repository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        repository.deleteById(id);
    }

    @DeleteMapping("/{id}")
public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
    // If product doesn’t exist → 404
    if (!repository.existsById(id)) {
        return ResponseEntity.notFound().build();
    }

    // Delete and return 204 No Content
    repository.deleteById(id);
    return ResponseEntity.noContent().build();
}

}
