package com.agri.supplytracker.organization.persistence;
import com.agri.supplytracker.organization.domain.Facility;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface FacilityRepository extends MongoRepository<Facility, String> { List<Facility> findByOrganizationId(String organizationId); }
