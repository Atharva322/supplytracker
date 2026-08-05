package com.agri.supplytracker.platform.persistence;
import com.agri.supplytracker.platform.domain.OutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {}
