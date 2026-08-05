package com.agri.supplytracker.service;

import com.agri.supplytracker.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import org.bson.Document;

@Service
public class ProductQueryService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORT_FIELDS = Set.of("name", "type", "batchId", "harvestDate", "status");
    private final MongoTemplate mongoTemplate;

    public ProductQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Page<Product> page(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        String safeSort = SORT_FIELDS.contains(sortBy) ? sortBy : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, safeSort));
        Query query = new Query().with(pageable);
        List<Product> products = mongoTemplate.find(query, Product.class);
        long count = mongoTemplate.count(new Query(), Product.class);
        return new PageImpl<>(products, pageable, count);
    }

    public List<Product> search(String name, String type, String batchId, String originFarmId) {
        List<Criteria> criteria = new ArrayList<>();
        if (hasText(name)) criteria.add(Criteria.where("name").regex(java.util.regex.Pattern.quote(name), "i"));
        if (hasText(type)) criteria.add(Criteria.where("type").regex("^"+java.util.regex.Pattern.quote(type)+"$", "i"));
        if (hasText(batchId)) criteria.add(Criteria.where("batchId").regex("^"+java.util.regex.Pattern.quote(batchId)+"$", "i"));
        if (hasText(originFarmId)) criteria.add(Criteria.where("originFarmId").regex("^"+java.util.regex.Pattern.quote(originFarmId)+"$", "i"));
        Query query = new Query();
        if (!criteria.isEmpty()) query.addCriteria(new Criteria().andOperator(criteria));
        query.limit(MAX_PAGE_SIZE);
        return mongoTemplate.find(query, Product.class);
    }

    public List<Product> searchKeyword(String keyword) {
        String safe = keyword == null ? "" : java.util.regex.Pattern.quote(keyword);
        Query query = Query.query(new Criteria().orOperator(
            Criteria.where("name").regex(safe, "i"), Criteria.where("type").regex(safe, "i"))).limit(MAX_PAGE_SIZE);
        return mongoTemplate.find(query, Product.class);
    }

    public List<Product> byStatus(String status) {
        return mongoTemplate.find(Query.query(Criteria.where("status").is(status)).limit(MAX_PAGE_SIZE), Product.class);
    }

    public List<Product> byFarm(String farmId) {
        return mongoTemplate.find(Query.query(Criteria.where("originFarmId").is(farmId)).limit(MAX_PAGE_SIZE), Product.class);
    }

    public Map<String, Object> dashboardStats() {
        long total = mongoTemplate.count(new Query(), Product.class);
        List<String> types = mongoTemplate.query(Product.class).distinct("type").as(String.class).all();
        List<String> farms = mongoTemplate.query(Product.class).distinct("originFarmId").as(String.class).all();
        Query recentQuery = new Query().with(Sort.by(Sort.Direction.DESC, "harvestDate")).limit(5);
        Map<String,Long> productsByType=new LinkedHashMap<>();
        for(String type:types) productsByType.put(type,mongoTemplate.count(Query.query(Criteria.where("type").is(type)),Product.class));
        Aggregation aggregation=Aggregation.newAggregation(
            Aggregation.unwind("trackingHistory"),
            Aggregation.group().count().as("total")
        );
        AggregationResults<Document> stageResult=mongoTemplate.aggregate(aggregation,"products",Document.class);
        Document totals=stageResult.getUniqueMappedResult();
        long trackingStages=totals==null?0L:((Number)totals.getOrDefault("total",0)).longValue();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("totalProducts",total); result.put("uniqueTypes",types.size()); result.put("uniqueFarms",farms.size());
        result.put("productsByType",productsByType); result.put("recentProducts",mongoTemplate.find(recentQuery,Product.class));
        result.put("totalTrackingStages",trackingStages); return result;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
