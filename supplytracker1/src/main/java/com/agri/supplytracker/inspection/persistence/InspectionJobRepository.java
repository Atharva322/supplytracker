package com.agri.supplytracker.inspection.persistence;

import com.agri.supplytracker.inspection.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InspectionJobRepository extends MongoRepository<InspectionJob, String> {
    List<InspectionJob> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);
    Optional<InspectionJob> findFirstByStatusOrderByQueuedAtAsc(InspectionJobStatus status);
    Optional<InspectionJob> findFirstByStatusAndNextAttemptAtLessThanEqualOrderByQueuedAtAsc(InspectionJobStatus status, Instant now);
}
