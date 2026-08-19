package com.agri.supplytracker.inspection.api;

import com.agri.supplytracker.inspection.application.*;
import com.agri.supplytracker.inspection.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/inspection-jobs")
public class InspectionJobController {
    private final InspectionJobService service;
    public InspectionJobController(InspectionJobService service) { this.service = service; }

    public record UploadSlotRequest(@NotBlank String organizationId, String filename, @NotBlank String contentType,
                                    @Min(1) long sizeBytes) {}
    public record CreateInspectionJobRequest(@NotBlank String organizationId, String batchId, @NotBlank String objectKey,
                                             @NotBlank String inputChecksum, @NotBlank String contentType) {}
    public record ReviewInspectionJobRequest(@NotNull InspectionReviewActionType action, List<String> correctedLabels,
                                             String correctedClassification, String reason) {}

    @PostMapping("/upload-slot")
    public ObjectStorageService.UploadSlot uploadSlot(@Valid @RequestBody UploadSlotRequest request, Authentication auth) {
        return service.requestUploadSlot(request.organizationId(), request.filename(), request.contentType(), request.sizeBytes(), auth.getName());
    }

    @PostMapping
    public ResponseEntity<InspectionJob> create(@Valid @RequestBody CreateInspectionJobRequest request,
        @RequestHeader("Idempotency-Key") String key, Authentication auth) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.create(request.organizationId(), request.batchId(),
            request.objectKey(), request.inputChecksum(), request.contentType(), auth.getName(), key));
    }

    @GetMapping("/{jobId}")
    public InspectionJob get(@PathVariable String jobId, Authentication auth) { return service.get(jobId, auth.getName()); }

    @PostMapping("/{jobId}/reviews")
    public InspectionJob review(@PathVariable String jobId, @Valid @RequestBody ReviewInspectionJobRequest request, Authentication auth) {
        return service.review(jobId, request.action(), request.correctedLabels(), request.correctedClassification(), request.reason(), auth.getName());
    }

    @GetMapping("/{jobId}/reviews")
    public List<InspectionReviewAction> reviews(@PathVariable String jobId, Authentication auth) {
        return service.reviews(jobId, auth.getName());
    }

    @GetMapping
    public List<InspectionJob> list(@RequestParam String organizationId, Authentication auth) {
        return service.list(organizationId, auth.getName());
    }
}
