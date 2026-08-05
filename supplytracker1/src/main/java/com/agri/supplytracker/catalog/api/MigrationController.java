package com.agri.supplytracker.catalog.api;
import com.agri.supplytracker.catalog.application.LegacyProductMigrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
@RestController
@RequestMapping("/api/v2/migrations")
public class MigrationController {
    private final LegacyProductMigrationService service;
    public MigrationController(LegacyProductMigrationService service){this.service=service;}
    public record LegacyMigrationRequest(@NotBlank String organizationId,@NotNull @DecimalMin("0.001") BigDecimal defaultQuantity,@NotBlank String defaultUnit,boolean dryRun){}
    @PostMapping("/legacy-products")
    public LegacyProductMigrationService.MigrationReport migrate(@Valid @RequestBody LegacyMigrationRequest request, Authentication auth){
        return service.migrate(request.organizationId(),request.defaultQuantity(),request.defaultUnit(),request.dryRun(),auth.getName());
    }
}
