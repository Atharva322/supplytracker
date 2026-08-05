package com.agri.supplytracker.shipment.api;

import com.agri.supplytracker.shipment.application.CustodyService;
import com.agri.supplytracker.shipment.domain.CustodyTransfer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v2/custody-transfers")
public class CustodyController {
    private final CustodyService service;
    public CustodyController(CustodyService service){this.service=service;}
    public record OfferRequest(@NotBlank String batchId,@NotBlank String recipientOrganizationId,
        @NotNull @DecimalMin("0.001") BigDecimal quantity,@NotBlank String unit){}
    @PostMapping
    public ResponseEntity<CustodyTransfer> offer(@Valid @RequestBody OfferRequest request,@RequestHeader("Idempotency-Key") String key,Authentication auth){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.offer(request.batchId(),request.recipientOrganizationId(),request.quantity(),request.unit(),auth.getName(),key));
    }
    @PostMapping("/{id}/accept")
    public CustodyTransfer accept(@PathVariable String id,@RequestHeader("Idempotency-Key") String key,Authentication auth){return service.accept(id,auth.getName(),key);}
}
