package com.agri.supplytracker.lineage.api;

import com.agri.supplytracker.lineage.application.RecallService;
import com.agri.supplytracker.lineage.domain.RecallCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/recalls")
public class RecallController {
    private final RecallService service;
    public RecallController(RecallService service) { this.service = service; }

    public record CreateRecallRequest(@NotBlank String sourceBatchId, @NotBlank String reason, boolean simulation) {}
    public record AcknowledgeRecallRequest(@NotBlank String organizationId, String note) {}
    public record ResolveRecallRequest(@NotBlank String resolution) {}

    @PostMapping
    public ResponseEntity<RecallCase> create(@Valid @RequestBody CreateRecallRequest request,
                                             @RequestHeader("Idempotency-Key") String key, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.sourceBatchId(), request.reason(), request.simulation(), auth.getName(), key));
    }

    @GetMapping("/{recallId}")
    public RecallCase get(@PathVariable String recallId, Authentication auth) {
        return service.get(recallId, auth.getName());
    }

    @GetMapping
    public List<RecallCase> list(@RequestParam String organizationId, Authentication auth) {
        return service.list(organizationId, auth.getName());
    }

    @PostMapping("/{recallId}/acknowledgments")
    public RecallCase acknowledge(@PathVariable String recallId, @Valid @RequestBody AcknowledgeRecallRequest request, Authentication auth) {
        return service.acknowledge(recallId, request.organizationId(), request.note(), auth.getName());
    }

    @PostMapping("/{recallId}/resolution")
    public RecallCase resolve(@PathVariable String recallId, @Valid @RequestBody ResolveRecallRequest request, Authentication auth) {
        return service.resolve(recallId, request.resolution(), auth.getName());
    }
}
