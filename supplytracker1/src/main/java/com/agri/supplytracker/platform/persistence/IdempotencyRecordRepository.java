package com.agri.supplytracker.platform.persistence;
import com.agri.supplytracker.platform.domain.IdempotencyRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
public interface IdempotencyRecordRepository extends MongoRepository<IdempotencyRecord, String> { Optional<IdempotencyRecord> findByActorAndKey(String actor, String key); }
