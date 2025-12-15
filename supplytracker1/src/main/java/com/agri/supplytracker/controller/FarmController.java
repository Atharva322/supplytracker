package com.agri.supplytracker.controller;

import com.agri.supplytracker.model.Farm;
import com.agri.supplytracker.repository.FarmRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/farms")
public class FarmController {

    private final FarmRepository repository;

    @Autowired
    public FarmController(FarmRepository repository) {
        this.repository = repository;
    }

    // GET all farms
    @GetMapping
    public List<Farm> getAllFarms() {
        return repository.findAll();
    }

    // GET farm by id
    @GetMapping("/{id}")
    public ResponseEntity<Farm> getFarmById(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST create farm (Admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createFarm(@Valid @RequestBody Farm farm) {
        try {
            Farm savedFarm = repository.save(farm);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedFarm);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create farm");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // PUT update farm (Admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFarm(@PathVariable String id, @Valid @RequestBody Farm updatedFarm) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setName(updatedFarm.getName());
                    existing.setLocation(updatedFarm.getLocation());
                    existing.setOwner(updatedFarm.getOwner());
                    existing.setContactInfo(updatedFarm.getContactInfo());
                    existing.setDescription(updatedFarm.getDescription());
                    Farm saved = repository.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // DELETE farm (Admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFarm(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
