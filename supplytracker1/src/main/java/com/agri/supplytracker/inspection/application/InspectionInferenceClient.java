package com.agri.supplytracker.inspection.application;

import com.agri.supplytracker.inspection.domain.InspectionJob;

import java.util.List;
import java.util.Map;

public interface InspectionInferenceClient {
    InferenceResult analyze(InspectionJob job, byte[] imageBytes);

    record InferenceResult(List<String> labels, Map<String, String> rawResult, double confidence, long latencyMs) {}
}
