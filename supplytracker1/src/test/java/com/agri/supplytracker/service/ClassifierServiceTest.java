package com.agri.supplytracker.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClassifierServiceTest {

    private final ClassifierService classifierService = new ClassifierService();

    @Test
    void classifiesKnownFruitIgnoringCase() {
        assertThat(classifierService.classifyProduct(List.of("Apple"))).isEqualTo("Fruits");
    }

    @Test
    void returnsUnknownForMissingLabels() {
        assertThat(classifierService.classifyProduct(null)).isEqualTo("Unknown");
        assertThat(classifierService.classifyProduct(List.of())).isEqualTo("Unknown");
    }

    @Test
    void preservesCurrentBestEffortFallbackContract() {
        assertThat(classifierService.classifyProduct(List.of("CRATE"))).isEqualTo("Crate");
    }
}
