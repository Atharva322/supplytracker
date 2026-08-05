package com.agri.supplytracker.shipment.persistence;
import com.agri.supplytracker.shipment.domain.CustodyTransfer;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface CustodyTransferRepository extends MongoRepository<CustodyTransfer,String> {}
