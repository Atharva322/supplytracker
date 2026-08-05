package com.agri.supplytracker.organization.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("memberships")
@CompoundIndex(name = "org_user", def = "{'organizationId':1,'username':1}", unique = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Membership {
    public enum Role { OWNER, ADMIN, OPERATOR, VIEWER }
    @Id private String id;
    private String organizationId;
    @Indexed private String username;
    private Role role;
}
