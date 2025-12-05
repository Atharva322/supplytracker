package com.agri.supplytracker.repository;

import com.agri.supplytracker.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {

    // already have: extends MongoRepository...

    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByType(String type);
    List<Product> findByBatchId(String batchId);
    List<Product> findByOriginFarmId(String originFarmId);
}


