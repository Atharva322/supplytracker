package com.agri.supplytracker.lineage.persistence;

import com.agri.supplytracker.lineage.domain.LineageEdge;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LineageEdgeRepository extends MongoRepository<LineageEdge, String> {
    List<LineageEdge> findByParentBatchId(String parentBatchId);
    List<LineageEdge> findByChildBatchId(String childBatchId);
}
