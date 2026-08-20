package com.agri.supplytracker.lineage.persistence;

import com.agri.supplytracker.lineage.domain.RecallCase;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RecallCaseRepository extends MongoRepository<RecallCase, String> {
    List<RecallCase> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);
}
