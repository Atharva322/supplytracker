package com.agri.supplytracker.inspection.persistence;

import com.agri.supplytracker.inspection.domain.InspectionReviewAction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InspectionReviewActionRepository extends MongoRepository<InspectionReviewAction, String> {
    List<InspectionReviewAction> findByJobIdOrderByCreatedAtAsc(String jobId);
}
