package com.agri.supplytracker.inspection.persistence;

import com.agri.supplytracker.inspection.domain.InspectionRetrainingCandidate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InspectionRetrainingCandidateRepository extends MongoRepository<InspectionRetrainingCandidate, String> {
    List<InspectionRetrainingCandidate> findByJobIdOrderByCreatedAtAsc(String jobId);
}
