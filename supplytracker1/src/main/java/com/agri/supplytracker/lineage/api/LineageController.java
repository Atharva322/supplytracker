package com.agri.supplytracker.lineage.api;

import com.agri.supplytracker.lineage.application.LineageService;
import com.agri.supplytracker.lineage.domain.LineageEdge;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v2/lineage")
public class LineageController {
    private final LineageService service;
    public LineageController(LineageService service) { this.service = service; }

    public record BatchQuantityRequest(@NotBlank String batchId, @NotNull @DecimalMin("0.001") BigDecimal quantity, @NotBlank String unit) {}
    public record SplitRequest(@NotEmpty List<@Valid BatchQuantityRequest> children) {}
    public record MergeRequest(@NotEmpty List<@Valid BatchQuantityRequest> parents, @NotBlank String childBatchId) {}
    public record SingleEdgeRequest(@NotBlank String childBatchId, @NotNull @DecimalMin("0.001") BigDecimal quantity, @NotBlank String unit) {}

    @PostMapping("/batches/{batchId}/split")
    public ResponseEntity<List<LineageEdge>> split(@PathVariable String batchId, @Valid @RequestBody SplitRequest request,
                                                   @RequestHeader("Idempotency-Key") String key, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.split(batchId, request.children().stream().map(this::toQuantity).toList(), auth.getName(), key));
    }

    @PostMapping("/merge")
    public ResponseEntity<List<LineageEdge>> merge(@Valid @RequestBody MergeRequest request,
                                                   @RequestHeader("Idempotency-Key") String key, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.merge(request.parents().stream().map(this::toQuantity).toList(), request.childBatchId(), auth.getName(), key));
    }

    @PostMapping("/batches/{batchId}/derive")
    public ResponseEntity<LineageEdge> derive(@PathVariable String batchId, @Valid @RequestBody SingleEdgeRequest request,
                                              @RequestHeader("Idempotency-Key") String key, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.derive(batchId, request.childBatchId(), request.quantity(), request.unit(), auth.getName(), key));
    }

    @PostMapping("/batches/{batchId}/consume")
    public ResponseEntity<LineageEdge> consume(@PathVariable String batchId, @Valid @RequestBody SingleEdgeRequest request,
                                               @RequestHeader("Idempotency-Key") String key, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.consume(batchId, request.childBatchId(), request.quantity(), request.unit(), auth.getName(), key));
    }

    @GetMapping("/batches/{batchId}/downstream")
    public List<LineageEdge> downstream(@PathVariable String batchId, Authentication auth) {
        return service.downstream(batchId, auth.getName());
    }

    @GetMapping("/batches/{batchId}/upstream")
    public List<LineageEdge> upstream(@PathVariable String batchId, Authentication auth) {
        return service.upstream(batchId, auth.getName());
    }

    @GetMapping("/batches/{batchId}/traverse")
    public LineageService.TraversalResult traverse(@PathVariable String batchId, Authentication auth) {
        service.downstream(batchId, auth.getName());
        return service.traverseDownstream(batchId);
    }

    private LineageService.BatchQuantity toQuantity(BatchQuantityRequest request) {
        return new LineageService.BatchQuantity(request.batchId(), request.quantity(), request.unit());
    }
}
