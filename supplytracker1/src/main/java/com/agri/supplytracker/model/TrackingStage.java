package com.agri.supplytracker.model;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrackingStage {
    
    @NotBlank(message = "Stage name is required")
    private String stage; // e.g., "Farm", "Processing", "Warehouse", "Distribution", "Retail"
    
    @NotBlank(message = "Location is required")
    private String location;
    
    @NotBlank(message = "Handler is required")
    private String handler; // Person or company handling at this stage
    
    private LocalDateTime timestamp;
    
    private String notes; // Optional additional information
}
