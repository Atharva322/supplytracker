package com.agri.supplytracker.shipment.domain;
import lombok.*;
import java.math.BigDecimal;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ShipmentLine { private String batchId; private BigDecimal quantity; private String unit; }
