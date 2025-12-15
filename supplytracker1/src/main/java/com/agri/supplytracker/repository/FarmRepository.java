package com.agri.supplytracker.repository;

import com.agri.supplytracker.model.Farm;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FarmRepository extends MongoRepository<Farm, String> {
    Optional<Farm> findByName(String name);
}
