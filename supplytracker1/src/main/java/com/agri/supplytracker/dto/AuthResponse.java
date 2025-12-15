package com.agri.supplytracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private Set<String> roles;
    private String message;
    private String stageProfile;
    private String location;
    private String associatedFarmId;
    
    public AuthResponse(String token, String username, Set<String> roles, String message) {
        this.token = token;
        this.username = username;
        this.roles = roles;
        this.message = message;
    }
}
