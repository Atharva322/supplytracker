package com.agri.supplytracker.shipment.persistence;
import com.agri.supplytracker.shipment.domain.ColdChainIncident;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface ColdChainIncidentRepository extends MongoRepository<ColdChainIncident,String> { List<ColdChainIncident> findByShipmentId(String shipmentId); }
