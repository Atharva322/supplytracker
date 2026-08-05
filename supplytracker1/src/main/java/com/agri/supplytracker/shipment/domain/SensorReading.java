package com.agri.supplytracker.shipment.domain;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
@Document("sensor_readings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SensorReading {
    @Id private String id;
    @Indexed(unique=true) private String readingId;
    @Indexed private String shipmentId;
    private String deviceId;
    private BigDecimal temperatureC;
    private BigDecimal humidityPercent;
    private Instant observedAt;
    private Instant receivedAt;
}
