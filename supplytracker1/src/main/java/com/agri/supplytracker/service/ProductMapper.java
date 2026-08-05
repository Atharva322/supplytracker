package com.agri.supplytracker.service;
import com.agri.supplytracker.dto.*;
import com.agri.supplytracker.model.Product;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
@Component
public class ProductMapper {
    public Product toDomain(ProductWriteRequest r){return Product.builder().name(r.name()).type(r.type()).batchId(r.batchId()).harvestDate(r.harvestDate())
        .originFarmId(r.originFarmId()).originFarmName(r.originFarmName()).currentLocation(r.currentLocation()).destination(r.destination()).status(r.status())
        .trackingHistory(new ArrayList<>()).build();}
    public ProductResponse toResponse(Product p){return new ProductResponse(p.getId(),p.getName(),p.getType(),p.getBatchId(),p.getHarvestDate(),p.getOriginFarmId(),
        p.getOriginFarmName(),p.getCurrentLocation(),p.getDestination(),p.getStatus(),p.getTrackingHistory());}
}
