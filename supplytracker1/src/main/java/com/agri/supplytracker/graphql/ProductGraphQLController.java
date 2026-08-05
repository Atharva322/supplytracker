package com.agri.supplytracker.graphql;

import com.agri.supplytracker.model.Product;
import com.agri.supplytracker.model.TrackingStage;
import com.agri.supplytracker.service.ProductQueryService;
import com.agri.supplytracker.service.ProductService;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** GraphQL remains a compatibility/read API; business writes delegate to the same application service as REST. */
@Controller
public class ProductGraphQLController {
    private final ProductService products;
    private final ProductQueryService queries;
    public ProductGraphQLController(ProductService products, ProductQueryService queries) { this.products=products; this.queries=queries; }

    @QueryMapping public List<Product> products() { return queries.page(0,100,"name","asc").getContent(); }
    @QueryMapping public Product product(@Argument String id) { try { return products.get(id); } catch (RuntimeException e) { return null; } }
    @QueryMapping public List<Product> searchProducts(@Argument String keyword) { return queries.searchKeyword(keyword); }
    @QueryMapping public List<Product> productsByStatus(@Argument String status) { return queries.byStatus(status); }
    @QueryMapping public List<Product> productsByFarm(@Argument String farmId) { return queries.byFarm(farmId); }

    @MutationMapping @PreAuthorize("hasRole('ADMIN')")
    public Product createProduct(@Argument Map<String,Object> input, Authentication auth) {
        Product product=Product.builder().name((String)input.get("name")).type((String)input.get("type"))
            .batchId((String)input.get("batchId")).harvestDate((String)input.get("harvestDate")).originFarmId((String)input.get("originFarmId"))
            .originFarmName((String)input.get("originFarmName")).currentLocation((String)input.get("currentLocation")).destination((String)input.get("destination"))
            .status((String)input.getOrDefault("status","AT_FARM")).trackingHistory(new ArrayList<>()).build();
        return products.create(product,auth.getName());
    }

    @MutationMapping @PreAuthorize("hasRole('ADMIN')")
    public Product updateProduct(@Argument String id,@Argument Map<String,Object> input,Authentication auth) { return products.patch(id,input,auth.getName()); }
    @MutationMapping @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteProduct(@Argument String id) { products.delete(id); return true; }
    @MutationMapping @PreAuthorize("hasRole('ADMIN')")
    public Product addTrackingStage(@Argument String productId,@Argument Map<String,Object> stage,Authentication auth) {
        LocalDateTime timestamp=LocalDateTime.now();
        if(stage.get("timestamp") instanceof String value && !value.isBlank()) timestamp=LocalDateTime.parse(value,DateTimeFormatter.ISO_DATE_TIME);
        TrackingStage tracking=TrackingStage.builder().stage((String)stage.get("stage")).location((String)stage.get("location"))
            .timestamp(timestamp).notes((String)stage.get("notes")).handler((String)stage.get("handler")).build();
        return products.addTrackingStage(productId,tracking,auth.getName());
    }
    @MutationMapping @PreAuthorize("hasRole('ADMIN')")
    public Product updateProductStatus(@Argument String id,@Argument String status,@Argument String location,Authentication auth) {
        Map<String,Object> patch=new HashMap<>(); patch.put("status",status); if(location!=null)patch.put("currentLocation",location);
        return products.patch(id,patch,auth.getName());
    }
}
