package com.agri.supplytracker.shipment.persistence;
import com.agri.supplytracker.shipment.domain.SensorReading;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface SensorReadingRepository extends MongoRepository<SensorReading,String> { Optional<SensorReading> findByReadingId(String readingId); List<SensorReading> findByShipmentIdOrderByObservedAtAsc(String shipmentId); }
