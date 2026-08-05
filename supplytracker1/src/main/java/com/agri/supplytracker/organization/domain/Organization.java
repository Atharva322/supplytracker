package com.agri.supplytracker.organization.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("organizations")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Organization {
    @Id private String id;
    @Indexed(unique = true) private String slug;
    private String name;
    private Instant createdAt;
}
