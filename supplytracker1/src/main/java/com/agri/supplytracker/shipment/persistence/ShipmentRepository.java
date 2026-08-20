package com.agri.supplytracker.shipment.persistence;
import com.agri.supplytracker.shipment.domain.Shipment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface ShipmentRepository extends MongoRepository<Shipment,String> {
    List<Shipment> findByLinesBatchIdIn(Collection<String> batchIds);
}
