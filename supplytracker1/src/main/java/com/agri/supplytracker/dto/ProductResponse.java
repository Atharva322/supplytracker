package com.agri.supplytracker.dto;
import com.agri.supplytracker.model.TrackingStage;
import java.util.List;
public record ProductResponse(String id,String name,String type,String batchId,String harvestDate,String originFarmId,
    String originFarmName,String currentLocation,String destination,String status,List<TrackingStage> trackingHistory) {}
