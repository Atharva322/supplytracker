package com.agri.supplytracker.dto;
import jakarta.validation.constraints.*;
public record ProductWriteRequest(
    @NotBlank String name,
    @NotBlank String type,
    @NotBlank String batchId,
    @NotBlank @Pattern(regexp="\\d{4}-\\d{2}-\\d{2}") String harvestDate,
    @NotBlank String originFarmId,
    String originFarmName,
    String currentLocation,
    String destination,
    String status
) {}
