package com.agri.supplytracker.inspection.persistence;

import com.agri.supplytracker.inspection.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Optional;

public interface InspectionQueueMessageRepository extends MongoRepository<InspectionQueueMessage, String> {
    Optional<InspectionQueueMessage> findFirstByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(Iterable<InspectionQueueStatus> statuses, Instant now);
}
