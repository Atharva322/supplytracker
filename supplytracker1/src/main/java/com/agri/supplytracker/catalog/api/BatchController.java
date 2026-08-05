package com.agri.supplytracker.catalog.api;

import com.agri.supplytracker.catalog.application.BatchService;
import com.agri.supplytracker.catalog.domain.*;
import com.agri.supplytracker.traceability.domain.TraceabilityEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v2/batches")
public class BatchController {
    private final BatchService service;
    public BatchController(BatchService service) { this.service = service; }
    public record CreateBatchRequest(@NotBlank String organizationId, @NotBlank String batchId, @NotBlank String productName,
        @NotBlank String productType, @NotNull @DecimalMin("0.001") BigDecimal quantity, @NotBlank String unit,
        @NotNull LocalDate harvestDate, String facilityId) {}
    public record TransitionRequest(@NotNull BatchStatus status, Long expectedVersion) {}

    @PostMapping
    public ResponseEntity<ProductBatch> create(@Valid @RequestBody CreateBatchRequest request,
        @RequestHeader("Idempotency-Key") String key, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.organizationId(), request.batchId(), request.productName(),
            request.productType(), request.quantity(), request.unit(), request.harvestDate(), request.facilityId(), auth.getName(), key));
    }
    @GetMapping public List<ProductBatch> list(@RequestParam String organizationId, Authentication auth) { return service.list(organizationId, auth.getName()); }
    @GetMapping("/{batchId}") public ProductBatch get(@PathVariable String batchId, Authentication auth) { return service.get(batchId, auth.getName()); }
    @GetMapping("/{batchId}/timeline") public List<TraceabilityEvent> timeline(@PathVariable String batchId, Authentication auth) { return service.timeline(batchId, auth.getName()); }
    @PostMapping("/{batchId}/transitions")
    public ProductBatch transition(@PathVariable String batchId, @Valid @RequestBody TransitionRequest request,
        @RequestHeader("Idempotency-Key") String key, Authentication auth) {
        return service.transition(batchId, request.status(), request.expectedVersion(), auth.getName(), key);
    }
}
