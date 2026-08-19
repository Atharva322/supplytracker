package com.agri.supplytracker.inspection.application;

import com.agri.supplytracker.inspection.domain.InspectionDecision;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InspectionScoringService {
    private final String profileVersion;
    private final String thresholdVersion;
    private final double defaultThreshold;
    private final Map<String, Double> productThresholds;
    private final Set<String> policySensitiveLabels;

    public InspectionScoringService(@Value("${inspection.scoring.profile-version:calibrated-local-v1}") String profileVersion,
                                    @Value("${inspection.threshold.version:thresholds-local-v1}") String thresholdVersion,
                                    @Value("${inspection.review.confidence-threshold:0.60}") double defaultThreshold,
                                    @Value("${inspection.scoring.product-thresholds:}") String productThresholds,
                                    @Value("${inspection.policy-sensitive-labels:}") String policySensitiveLabels) {
        this.profileVersion = profileVersion;
        this.thresholdVersion = thresholdVersion;
        this.defaultThreshold = defaultThreshold;
        this.productThresholds = parseThresholds(productThresholds);
        this.policySensitiveLabels = parseLabels(policySensitiveLabels);
    }

    public Score score(String classification, List<String> labels, double confidence) {
        double threshold = thresholdFor(classification);
        boolean policySensitive = labels != null && labels.stream()
            .filter(Objects::nonNull)
            .map(label -> label.toLowerCase(Locale.ROOT))
            .anyMatch(policySensitiveLabels::contains);
        int qualityScore = (int) Math.round(Math.max(0.0, Math.min(1.0, confidence)) * 100);
        InspectionDecision decision = confidence < threshold || policySensitive ? InspectionDecision.REVIEW : InspectionDecision.APPROVE;
        String band = decision == InspectionDecision.APPROVE ? "PASS" : confidence < threshold / 2.0 ? "FAIL" : "REVIEW";
        return new Score(qualityScore, band, threshold, policySensitive, decision, profileVersion, thresholdVersion);
    }

    private double thresholdFor(String classification) {
        if (classification == null) return defaultThreshold;
        return productThresholds.getOrDefault(classification.toLowerCase(Locale.ROOT), defaultThreshold);
    }

    private Map<String, Double> parseThresholds(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();
        Map<String, Double> thresholds = new LinkedHashMap<>();
        for (String token : raw.split(",")) {
            String[] parts = token.split(":");
            if (parts.length == 2 && !parts[0].isBlank()) thresholds.put(parts[0].trim().toLowerCase(Locale.ROOT), Double.parseDouble(parts[1].trim()));
        }
        return thresholds;
    }

    private Set<String> parseLabels(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(label -> !label.isBlank())
            .map(label -> label.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    }

    public record Score(int qualityScore, String qualityBand, double reviewThreshold, boolean policySensitive,
                        InspectionDecision decision, String profileVersion, String thresholdVersion) {}
}
