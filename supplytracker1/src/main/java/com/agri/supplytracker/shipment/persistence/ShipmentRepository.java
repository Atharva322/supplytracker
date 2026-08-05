package com.agri.supplytracker.shipment.persistence;
import com.agri.supplytracker.shipment.domain.Shipment;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface ShipmentRepository extends MongoRepository<Shipment,String> {}
