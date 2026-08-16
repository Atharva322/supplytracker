package com.agri.supplytracker.catalog;

import com.agri.supplytracker.catalog.application.BatchService;
import com.agri.supplytracker.catalog.domain.ProductBatch;
import com.agri.supplytracker.catalog.persistence.ProductBatchRepository;
import com.agri.supplytracker.organization.application.OrganizationService;
import com.agri.supplytracker.organization.domain.Organization;
import com.agri.supplytracker.organization.persistence.MembershipRepository;
import com.agri.supplytracker.organization.persistence.OrganizationRepository;
import com.agri.supplytracker.platform.persistence.IdempotencyRecordRepository;
import com.agri.supplytracker.platform.persistence.OutboxEventRepository;
import com.agri.supplytracker.traceability.persistence.TraceabilityEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "jwt.secret=integration-test-secret-with-at-least-32-bytes",
    "spring.data.mongodb.uri=mongodb://localhost:27018/agriproj_integration?directConnection=true",
    "app.bootstrap-admin.enabled=false"
})
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_INTEGRATION", matches = "true")
class BatchMongoIntegrationTest {
    @MockBean ClientRegistrationRepository clientRegistrationRepository;

    @Autowired BatchService batches;
    @Autowired OrganizationService organizations;
    @Autowired ProductBatchRepository batchRepository;
    @Autowired TraceabilityEventRepository eventRepository;
    @Autowired OutboxEventRepository outboxRepository;
    @Autowired IdempotencyRecordRepository idempotencyRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired MembershipRepository membershipRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxRepository.deleteAll();
        eventRepository.deleteAll();
        idempotencyRepository.deleteAll();
        batchRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void createPersistsBatchEventOutboxAndIdempotencyInReplicaSetTransaction() {
        Organization org = organizations.create("Integration Farms", "integration-farms", "alice");

        ProductBatch created = batches.create(org.getId(), "INT-B-1", "Mango", "Fruit",
            new BigDecimal("10.0"), "kg", LocalDate.of(2026, 8, 16), null, "alice", "create-key");
        ProductBatch replay = batches.create(org.getId(), "INT-B-1", "Mango", "Fruit",
            new BigDecimal("10.0"), "kg", LocalDate.of(2026, 8, 16), null, "alice", "create-key");

        assertEquals(created.getId(), replay.getId());
        assertEquals(1, batchRepository.count());
        assertEquals(1, eventRepository.findByBatchIdOrderBySequenceNumberAsc("INT-B-1").size());
        assertEquals(1, outboxRepository.count());
        assertEquals(1, idempotencyRepository.count());

        IllegalStateException conflict = assertThrows(IllegalStateException.class,
            () -> batches.create(org.getId(), "INT-B-2", "Mango", "Fruit",
                new BigDecimal("10.0"), "kg", LocalDate.of(2026, 8, 16), null, "alice", "create-key"));

        assertEquals("Idempotency key already used with a different payload", conflict.getMessage());
        assertEquals(1, batchRepository.count());
        assertEquals(1, outboxRepository.count());
    }
}
