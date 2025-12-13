package com.agri.supplytracker.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product {
    @Id
    private String id;
    
    @NotBlank(message = "Product name is required")
    private String name;
    
    @NotBlank(message = "Product type is required")
    private String type;
    
    @NotBlank(message = "Batch ID is required")
    private String batchId;
    
    @NotBlank(message = "Harvest date is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Harvest date must be in YYYY-MM-DD format")
    private String harvestDate;
    
    @NotBlank(message = "Origin farm ID is required")
    private String originFarmId;
}
