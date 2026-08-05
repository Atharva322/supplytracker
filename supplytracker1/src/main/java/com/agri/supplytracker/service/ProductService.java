package com.agri.supplytracker.service;

import com.agri.supplytracker.controller.ProductStreamController;
import com.agri.supplytracker.exception.ProductNotFoundException;
import com.agri.supplytracker.model.Product;
import com.agri.supplytracker.model.TrackingStage;
import com.agri.supplytracker.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ProductService {
    private final ProductRepository repository;
    private final ProductStreamController stream;
    private final NotificationService notifications;

    public ProductService(ProductRepository repository, ProductStreamController stream, NotificationService notifications) {
        this.repository = repository;
        this.stream = stream;
        this.notifications = notifications;
    }

    public Product get(String id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product create(Product product, String actor) {
        Product saved = repository.save(product);
        stream.sendProductUpdateToUser(saved, actor);
        notifications.notifyAdmins("PRODUCT_CREATED", "New Product Created",
            "Product '" + saved.getName() + "' created", saved.getId(), saved.getName(), actor, actor);
        return saved;
    }

    public Product replace(String id, Product input, String actor) {
        Product existing = get(id);
        existing.setName(input.getName());
        existing.setType(input.getType());
        existing.setBatchId(input.getBatchId());
        existing.setHarvestDate(input.getHarvestDate());
        existing.setOriginFarmId(input.getOriginFarmId());
        return saveAndNotify(existing, actor);
    }

    public Product patch(String id, Map<String, Object> updates, String actor) {
        Product product = get(id);
        if (updates.containsKey("name")) product.setName((String) updates.get("name"));
        if (updates.containsKey("type")) product.setType((String) updates.get("type"));
        if (updates.containsKey("batchId")) product.setBatchId((String) updates.get("batchId"));
        if (updates.containsKey("harvestDate")) product.setHarvestDate((String) updates.get("harvestDate"));
        if (updates.containsKey("originFarmId")) product.setOriginFarmId((String) updates.get("originFarmId"));
        if (updates.containsKey("originFarmName")) product.setOriginFarmName((String) updates.get("originFarmName"));
        if (updates.containsKey("currentLocation")) product.setCurrentLocation((String) updates.get("currentLocation"));
        if (updates.containsKey("destination")) product.setDestination((String) updates.get("destination"));
        if (updates.containsKey("status")) product.setStatus((String) updates.get("status"));
        return saveAndNotify(product, actor);
    }

    public void delete(String id) {
        if (!repository.existsById(id)) throw new ProductNotFoundException(id);
        repository.deleteById(id);
    }

    public Product addTrackingStage(String id, TrackingStage stage, String actor) {
        Product product = get(id);
        if (stage.getTimestamp() == null) stage.setTimestamp(LocalDateTime.now());
        product.getTrackingHistory().add(stage);
        product.setCurrentLocation(stage.getLocation());
        product.setStatus(stage.getStage());
        Product saved = saveAndNotify(product, actor);
        notifications.notifyAdmins("TRACKING_STAGE_ADDED", "Tracking Stage Added",
            "Stage '" + stage.getStage() + "' added to product '" + product.getName() + "'",
            product.getId(), product.getName(), actor, actor);
        return saved;
    }

    private Product saveAndNotify(Product product, String actor) {
        Product saved = repository.save(product);
        stream.sendProductUpdateToUser(saved, actor);
        return saved;
    }
}
