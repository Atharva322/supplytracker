package com.agri.supplytracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BedrockService {

    private static final Logger logger = LoggerFactory.getLogger(BedrockService.class);

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.bedrock.model-id:amazon.titan-text-express-v1}")
    private String modelId;

    private BedrockRuntimeClient bedrockClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            bedrockClient = BedrockRuntimeClient.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
            logger.info("BedrockRuntimeClient initialised for region {}", awsRegion);
        } catch (Exception e) {
            logger.warn("Could not initialise Bedrock client – AI descriptions will use fallback: {}", e.getMessage());
            bedrockClient = null;
        }
    }

    public String generateImageDescription(List<String> labels) {
        if (bedrockClient == null) {
            return buildFallbackDescription(labels);
        }
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputText", buildPrompt(labels));

            String requestJson = objectMapper.writeValueAsString(requestBody);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(requestJson))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();

            Map<?, ?> responseMap = objectMapper.readValue(responseBody, Map.class);
            if (responseMap.containsKey("results")) {
                List<?> results = (List<?>) responseMap.get("results");
                if (!results.isEmpty() && results.get(0) instanceof Map<?, ?> result) {
                    Object outputText = result.get("outputText");
                    if (outputText != null) {
                        return outputText.toString();
                    }
                }
            }
            return buildFallbackDescription(labels);
        } catch (Exception e) {
            logger.error("Error calling AWS Bedrock: {}", e.getMessage());
            return buildFallbackDescription(labels);
        }
    }

    private String buildPrompt(List<String> labels) {
        return "Describe the following detected items in a supply chain context: "
                + String.join(", ", labels)
                + ". Provide a brief 1-2 sentence product description for inventory purposes.";
    }

    private String buildFallbackDescription(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return "No items detected in the image.";
        }
        return "Detected items: " + String.join(", ", labels) + ".";
    }
}
