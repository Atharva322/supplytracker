package com.agri.supplytracker.traceability.persistence;
import com.agri.supplytracker.traceability.domain.TraceabilityEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface TraceabilityEventRepository extends MongoRepository<TraceabilityEvent, String> {
    List<TraceabilityEvent> findByBatchIdOrderBySequenceNumberAsc(String batchId);
    Optional<TraceabilityEvent> findTopByBatchIdOrderBySequenceNumberDesc(String batchId);
}
