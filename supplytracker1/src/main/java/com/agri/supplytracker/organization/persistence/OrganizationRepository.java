package com.agri.supplytracker.organization.persistence;
import com.agri.supplytracker.organization.domain.Organization;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface OrganizationRepository extends MongoRepository<Organization, String> { boolean existsBySlug(String slug); }
