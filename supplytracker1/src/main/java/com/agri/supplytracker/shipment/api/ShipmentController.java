package com.agri.supplytracker.shipment.api;

import com.agri.supplytracker.shipment.application.ShipmentService;
import com.agri.supplytracker.shipment.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v2/shipments")
public class ShipmentController {
    private final ShipmentService service;
    public ShipmentController(ShipmentService service){this.service=service;}
    public record CreateShipmentRequest(@NotBlank String custodyTransferId,@NotNull BigDecimal minimumTemperatureC,@NotNull BigDecimal maximumTemperatureC){}
    public record SensorReadingRequest(@NotBlank String readingId,@NotBlank String deviceId,
        @NotNull @DecimalMin("-100") @DecimalMax("100") BigDecimal temperatureC,
        @DecimalMin("0") @DecimalMax("100") BigDecimal humidityPercent,@NotNull Instant observedAt){}

    @PostMapping
    public ResponseEntity<Shipment> create(@Valid @RequestBody CreateShipmentRequest request,@RequestHeader("Idempotency-Key") String key,Authentication auth){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.custodyTransferId(),request.minimumTemperatureC(),request.maximumTemperatureC(),auth.getName(),key));
    }
    @GetMapping("/{id}") public Shipment get(@PathVariable String id,Authentication auth){return service.getAuthorized(id,auth.getName());}
    @PostMapping("/{id}/dispatch") public Shipment dispatch(@PathVariable String id,@RequestHeader("Idempotency-Key") String key,Authentication auth){return service.dispatch(id,auth.getName(),key);}
    @PostMapping("/{id}/receive") public Shipment receive(@PathVariable String id,@RequestHeader("Idempotency-Key") String key,Authentication auth){return service.receive(id,auth.getName(),key);}
    @PostMapping("/{id}/sensor-readings") public ResponseEntity<SensorReading> ingest(@PathVariable String id,@Valid @RequestBody SensorReadingRequest request,Authentication auth){
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.ingest(id,request.readingId(),request.deviceId(),request.temperatureC(),request.humidityPercent(),request.observedAt(),auth.getName()));
    }
    @GetMapping("/{id}/sensor-readings") public List<SensorReading> readings(@PathVariable String id,Authentication auth){return service.readings(id,auth.getName());}
    @GetMapping("/{id}/incidents") public List<ColdChainIncident> incidents(@PathVariable String id,Authentication auth){return service.incidents(id,auth.getName());}
}
