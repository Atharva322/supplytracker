package com.agri.supplytracker.organization.persistence;
import com.agri.supplytracker.organization.domain.Membership;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface MembershipRepository extends MongoRepository<Membership, String> {
    Optional<Membership> findByOrganizationIdAndUsername(String organizationId, String username);
    List<Membership> findByUsername(String username);
    List<Membership> findByOrganizationId(String organizationId);
}
