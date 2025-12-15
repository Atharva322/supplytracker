package com.agri.supplytracker.model;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("farms")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Farm {
    @Id
    private String id;
    
    @NotBlank(message = "Farm name is required")
    private String name;
    
    @NotBlank(message = "Location is required")
    private String location;
    
    @NotBlank(message = "Owner name is required")
    private String owner;
    
    private String contactInfo; // Phone, email, etc.
    
    private String description; // Optional farm details
}
