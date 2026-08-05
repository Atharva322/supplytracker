package com.agri.supplytracker.organization.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("facilities")
@CompoundIndex(name = "org_facility_code", def = "{'organizationId':1,'code':1}", unique = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Facility {
    @Id private String id;
    private String organizationId;
    private String code;
    private String name;
    private String type;
    private String address;
}
