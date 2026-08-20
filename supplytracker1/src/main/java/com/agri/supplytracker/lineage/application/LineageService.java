package com.agri.supplytracker.lineage.application;

import com.agri.supplytracker.catalog.application.BatchService;
import com.agri.supplytracker.catalog.domain.ProductBatch;
import com.agri.supplytracker.lineage.domain.*;
import com.agri.supplytracker.lineage.persistence.LineageEdgeRepository;
import com.agri.supplytracker.platform.domain.*;
import com.agri.supplytracker.platform.persistence.IdempotencyRecordRepository;
import com.agri.supplytracker.traceability.domain.TraceEventType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class LineageService {
    private final LineageEdgeRepository edges;
    private final IdempotencyRecordRepository idempotency;
    private final BatchService batches;
    private final int defaultMaxDepth;
    private final int defaultMaxNodes;

    public LineageService(LineageEdgeRepository edges, IdempotencyRecordRepository idempotency, BatchService batches,
                          @Value("${lineage.traversal.max-depth:12}") int defaultMaxDepth,
                          @Value("${lineage.traversal.max-nodes:1000}") int defaultMaxNodes) {
        this.edges = edges; this.idempotency = idempotency; this.batches = batches;
        this.defaultMaxDepth = defaultMaxDepth; this.defaultMaxNodes = defaultMaxNodes;
    }

    @Transactional
    public List<LineageEdge> split(String parentBatchId, List<BatchQuantity> children, String actor, String key) {
        requireKey(key);
        if (children == null || children.size() < 2) throw new IllegalArgumentException("split requires at least two children");
        String requestHash = IdempotencySupport.hash("lineage.split", parentBatchId, children);
        Optional<IdempotencyRecord> replay = idempotency.findByActorAndKey(actor, key);
        if (replay.isPresent()) { IdempotencySupport.requireSameRequest(replay.get(), requestHash); return downstream(parentBatchId, actor); }
        ProductBatch parent = batches.get(parentBatchId, actor);
        assertConservation(parent.getQuantity(), parent.getUnit(), children);
        List<LineageEdge> saved = new ArrayList<>();
        for (BatchQuantity child : children) saved.add(createEdge(parent, child.batchId(), LineageOperation.SPLIT, child.quantity(), child.unit(), actor, Map.of("operation", "split")));
        batches.appendEvent(parent, TraceEventType.LINEAGE_SPLIT, actor, Map.of("children", String.join(",", children.stream().map(BatchQuantity::batchId).toList())));
        saveKey(actor, key, requestHash, "LINEAGE_SPLIT", parentBatchId);
        return saved;
    }

    @Transactional
    public List<LineageEdge> merge(List<BatchQuantity> parents, String childBatchId, String actor, String key) {
        requireKey(key);
        if (parents == null || parents.size() < 2) throw new IllegalArgumentException("merge requires at least two parents");
        String requestHash = IdempotencySupport.hash("lineage.merge", parents, childBatchId);
        Optional<IdempotencyRecord> replay = idempotency.findByActorAndKey(actor, key);
        if (replay.isPresent()) { IdempotencySupport.requireSameRequest(replay.get(), requestHash); return upstream(childBatchId, actor); }
        ProductBatch child = batches.get(childBatchId, actor);
        assertConservation(child.getQuantity(), child.getUnit(), parents);
        List<LineageEdge> saved = new ArrayList<>();
        for (BatchQuantity parent : parents) {
            ProductBatch parentBatch = batches.get(parent.batchId(), actor);
            saved.add(createEdge(parentBatch, childBatchId, LineageOperation.MERGE, parent.quantity(), parent.unit(), actor, Map.of("operation", "merge")));
            batches.appendEvent(parentBatch, TraceEventType.LINEAGE_MERGE, actor, Map.of("child", childBatchId));
        }
        batches.appendEvent(child, TraceEventType.LINEAGE_MERGE, actor, Map.of("parents", String.join(",", parents.stream().map(BatchQuantity::batchId).toList())));
        saveKey(actor, key, requestHash, "LINEAGE_MERGE", childBatchId);
        return saved;
    }

    @Transactional
    public LineageEdge derive(String parentBatchId, String childBatchId, BigDecimal quantity, String unit, String actor, String key) {
        return single(parentBatchId, childBatchId, quantity, unit, LineageOperation.DERIVE, actor, key);
    }

    @Transactional
    public LineageEdge consume(String parentBatchId, String childBatchId, BigDecimal quantity, String unit, String actor, String key) {
        return single(parentBatchId, childBatchId, quantity, unit, LineageOperation.CONSUME, actor, key);
    }

    public List<LineageEdge> downstream(String batchId, String actor) {
        batches.get(batchId, actor);
        return edges.findByParentBatchId(batchId);
    }

    public List<LineageEdge> upstream(String batchId, String actor) {
        batches.get(batchId, actor);
        return edges.findByChildBatchId(batchId);
    }

    public TraversalResult traverseDownstream(String sourceBatchId) {
        return traverseDownstream(sourceBatchId, defaultMaxDepth, defaultMaxNodes);
    }

    public TraversalResult traverseDownstream(String sourceBatchId, int maxDepth, int maxNodes) {
        Set<String> visited = new LinkedHashSet<>();
        Map<String, String> explanations = new LinkedHashMap<>();
        Deque<NodeDepth> queue = new ArrayDeque<>();
        visited.add(sourceBatchId); explanations.put(sourceBatchId, "source batch"); queue.add(new NodeDepth(sourceBatchId, 0));
        int edgesVisited = 0; int maxDepthReached = 0; boolean truncated = false; String reason = "";
        while (!queue.isEmpty()) {
            NodeDepth current = queue.removeFirst();
            maxDepthReached = Math.max(maxDepthReached, current.depth());
            if (current.depth() >= maxDepth) { truncated = true; reason = "max depth reached"; continue; }
            for (LineageEdge edge : edges.findByParentBatchId(current.batchId())) {
                edgesVisited++;
                if (visited.size() >= maxNodes) { truncated = true; reason = "max nodes reached"; break; }
                if (visited.add(edge.getChildBatchId())) {
                    explanations.put(edge.getChildBatchId(), edge.getOperation().name() + " from " + edge.getParentBatchId());
                    queue.addLast(new NodeDepth(edge.getChildBatchId(), current.depth() + 1));
                }
            }
            if (truncated && "max nodes reached".equals(reason)) break;
        }
        return new TraversalResult(visited, explanations, RecallTraversalStats.builder()
            .nodesVisited(visited.size()).edgesVisited(edgesVisited).maxDepthReached(maxDepthReached)
            .truncated(truncated).truncationReason(reason).build());
    }

    private LineageEdge single(String parentBatchId, String childBatchId, BigDecimal quantity, String unit,
                               LineageOperation operation, String actor, String key) {
        requireKey(key);
        String requestHash = IdempotencySupport.hash("lineage." + operation.name().toLowerCase(Locale.ROOT), parentBatchId, childBatchId, quantity, unit);
        Optional<IdempotencyRecord> replay = idempotency.findByActorAndKey(actor, key);
        if (replay.isPresent()) { IdempotencySupport.requireSameRequest(replay.get(), requestHash); return downstream(parentBatchId, actor).stream().filter(edge -> childBatchId.equals(edge.getChildBatchId())).findFirst().orElseThrow(); }
        ProductBatch parent = batches.get(parentBatchId, actor);
        requirePositive(quantity); requireSameUnit(parent.getUnit(), unit);
        if (quantity.compareTo(parent.getQuantity()) > 0) throw new IllegalArgumentException("Derived quantity cannot exceed parent quantity");
        LineageEdge saved = createEdge(parent, childBatchId, operation, quantity, unit, actor, Map.of("operation", operation.name().toLowerCase(Locale.ROOT)));
        batches.appendEvent(parent, operation == LineageOperation.DERIVE ? TraceEventType.LINEAGE_DERIVE : TraceEventType.LINEAGE_CONSUME, actor, Map.of("child", childBatchId));
        saveKey(actor, key, requestHash, "LINEAGE_" + operation.name(), parentBatchId);
        return saved;
    }

    private LineageEdge createEdge(ProductBatch parent, String childBatchId, LineageOperation operation, BigDecimal quantity, String unit, String actor, Map<String, String> metadata) {
        ProductBatch child = batches.get(childBatchId, actor);
        requireSameUnit(parent.getUnit(), unit); requireSameUnit(child.getUnit(), unit); requirePositive(quantity);
        if (hasPath(childBatchId, parent.getBatchId())) throw new IllegalStateException("Lineage edge would create a cycle");
        return edges.save(LineageEdge.builder().parentBatchId(parent.getBatchId()).childBatchId(childBatchId)
            .operation(operation).organizationId(parent.getOrganizationId()).quantity(quantity).unit(unit)
            .actor(actor).createdAt(Instant.now()).metadata(metadata).build());
    }

    private boolean hasPath(String start, String target) {
        Set<String> visited = new HashSet<>(); Deque<String> queue = new ArrayDeque<>(); queue.add(start);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) continue;
            if (current.equals(target)) return true;
            edges.findByParentBatchId(current).forEach(edge -> queue.add(edge.getChildBatchId()));
        }
        return false;
    }

    private void assertConservation(BigDecimal expected, String unit, List<BatchQuantity> parts) {
        BigDecimal total = BigDecimal.ZERO;
        for (BatchQuantity part : parts) { requirePositive(part.quantity()); requireSameUnit(unit, part.unit()); total = total.add(part.quantity()); }
        if (expected.compareTo(total) != 0) throw new IllegalArgumentException("Lineage quantity must be conserved");
    }

    private void requirePositive(BigDecimal value) { if (value == null || value.signum() <= 0) throw new IllegalArgumentException("Quantity must be positive"); }
    private void requireSameUnit(String left, String right) { if (!Objects.equals(left, right)) throw new IllegalArgumentException("Lineage units must match"); }
    private void requireKey(String key) { if (key == null || key.isBlank()) throw new IllegalArgumentException("Idempotency-Key header is required"); }
    private void saveKey(String actor, String key, String hash, String type, String id) { idempotency.save(IdempotencyRecord.builder().actor(actor).key(key).requestHash(hash).resourceType(type).resourceId(id).createdAt(Instant.now()).build()); }

    public record BatchQuantity(String batchId, BigDecimal quantity, String unit) {}
    private record NodeDepth(String batchId, int depth) {}
    public record TraversalResult(Set<String> batchIds, Map<String, String> explanations, RecallTraversalStats stats) {}
}
