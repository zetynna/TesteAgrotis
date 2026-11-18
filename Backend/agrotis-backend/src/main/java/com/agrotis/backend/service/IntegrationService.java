package com.agrotis.backend.service;

import com.agrotis.backend.domain.SyncBatch;
import com.agrotis.backend.domain.SyncStatus;
import com.agrotis.backend.dto.IntegrationRequest;
import com.agrotis.backend.dto.SyncBatchResponse;
import com.agrotis.backend.repository.SyncBatchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IntegrationService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationService.class);

    private final SyncBatchRepository syncBatchRepository;
    private final IntegrationPayloadProcessor payloadProcessor;
    private final ObjectMapper objectMapper;
    private final int maxAttempts;
    private final long baseBackoffSeconds;
    private final long maxBackoffSeconds;
    private final long inProgressTimeoutSeconds;

    public IntegrationService(
        SyncBatchRepository syncBatchRepository,
        IntegrationPayloadProcessor payloadProcessor,
        ObjectMapper objectMapper,
        @Value("${agrotis.integration.max-attempts:5}") int maxAttempts,
        @Value("${agrotis.integration.base-backoff-seconds:2}") long baseBackoffSeconds,
        @Value("${agrotis.integration.max-backoff-seconds:300}") long maxBackoffSeconds,
        @Value("${agrotis.integration.in-progress-timeout-seconds:900}") long inProgressTimeoutSeconds
    ) {
        this.syncBatchRepository = syncBatchRepository;
        this.payloadProcessor = payloadProcessor;
        this.objectMapper = objectMapper;
        this.maxAttempts = maxAttempts;
        this.baseBackoffSeconds = baseBackoffSeconds;
        this.maxBackoffSeconds = maxBackoffSeconds;
        this.inProgressTimeoutSeconds = inProgressTimeoutSeconds;
    }

    @Transactional
    public SyncBatchResponse start(IntegrationRequest request) {
        if (request.schemaVersion() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schemaVersion não suportado. Use a versão 1.");
        }
        String payload = canonicalJson(request.payload());
        String hash = sha256(payload);
        var existing = syncBatchRepository.findFirstBySourceAndExternalReferenceOrderByCreatedAtAsc(
            request.source(), request.externalReference());
        if (existing.isPresent()) {
            SyncBatch batch = existing.get();
            if (!batch.getPayloadHash().equals(hash)
                || !batch.getEntityType().equals(request.entityType().name())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A chave source + externalReference já foi usada com outro payload.");
            }
            log.info("Integration duplicate acknowledged source={} reference={} batchId={} status={}",
                batch.getSource(), batch.getExternalReference(), batch.getId(), batch.getStatus());
            return toResponse(batch);
        }

        SyncBatch batch = new SyncBatch(request.source(), request.externalReference(),
            request.entityType().name(), payload, hash,
            sha256(request.source() + "\u0000" + request.externalReference()));
        batch.setStatus(SyncStatus.PENDING);
        batch = syncBatchRepository.saveAndFlush(batch);
        log.info("Integration accepted source={} reference={} type={} batchId={}",
            batch.getSource(), batch.getExternalReference(), batch.getEntityType(), batch.getId());
        return toResponse(batch);
    }

    @Transactional(readOnly = true)
    public List<SyncBatchResponse> listBatches() {
        return syncBatchRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SyncBatchResponse> listDlq() {
        return syncBatchRepository.findAllByOrderByCreatedAtDesc().stream()
            .filter(batch -> batch.getStatus() == SyncStatus.DLQ)
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public SyncBatchResponse reprocess(Long batchId) {
        SyncBatch batch = syncBatchRepository.findById(batchId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Lote não encontrado"));
        if (batch.getStatus() != SyncStatus.DLQ && batch.getStatus() != SyncStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Somente lotes em FAILED ou DLQ podem ser reprocessados.");
        }
        batch.requeue();
        batch.setAttemptCount(0);
        batch = syncBatchRepository.save(batch);
        log.info("Integration manually requeued source={} reference={} batchId={}",
            batch.getSource(), batch.getExternalReference(), batch.getId());
        return toResponse(batch);
    }

    @Scheduled(fixedDelayString = "${agrotis.integration.poll-delay-ms:1000}")
    public void processDueBatches() {
        syncBatchRepository.findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            List.of(SyncStatus.PENDING, SyncStatus.FAILED, SyncStatus.IN_PROGRESS), LocalDateTime.now())
            .forEach(batch -> processOne(batch.getId()));
    }

    public void processOne(Long batchId) {
        SyncBatch batch = syncBatchRepository.findById(batchId).orElse(null);
        if (batch == null || (batch.getStatus() != SyncStatus.PENDING && batch.getStatus() != SyncStatus.FAILED
            && batch.getStatus() != SyncStatus.IN_PROGRESS)) {
            return;
        }
        batch.setStatus(SyncStatus.IN_PROGRESS);
        batch.setAttemptCount(batch.getAttemptCount() + 1);
        batch.setNextAttemptAt(LocalDateTime.now().plusSeconds(inProgressTimeoutSeconds));
        batch = syncBatchRepository.saveAndFlush(batch);
        log.info("Integration attempt started source={} reference={} type={} batchId={} attempt={}",
            batch.getSource(), batch.getExternalReference(), batch.getEntityType(),
            batch.getId(), batch.getAttemptCount());

        try {
            JsonNode payload = objectMapper.readTree(batch.getPayload());
            payloadProcessor.process(IntegrationRequest.EntityType.valueOf(batch.getEntityType()), payload);
            batch.setStatus(SyncStatus.SUCCEEDED);
            batch.setErrorMessage(null);
            batch.setNextAttemptAt(null);
            syncBatchRepository.save(batch);
            log.info("Integration attempt completed source={} reference={} batchId={} attempt={}",
                batch.getSource(), batch.getExternalReference(), batch.getId(), batch.getAttemptCount());
        } catch (Exception exception) {
            boolean permanent = exception instanceof IllegalArgumentException
                || exception instanceof jakarta.persistence.EntityNotFoundException;
            boolean exhausted = batch.getAttemptCount() >= maxAttempts;
            batch.setErrorMessage(safeError(exception));
            if (permanent || exhausted) {
                batch.setStatus(SyncStatus.DLQ);
                batch.setNextAttemptAt(null);
            } else {
                batch.setStatus(SyncStatus.FAILED);
                long delay = Math.min(maxBackoffSeconds,
                    baseBackoffSeconds * (1L << Math.min(batch.getAttemptCount() - 1, 30)));
                batch.setNextAttemptAt(LocalDateTime.now().plusSeconds(delay));
            }
            syncBatchRepository.save(batch);
            log.warn("Integration attempt failed source={} reference={} batchId={} attempt={} status={} error={}",
                batch.getSource(), batch.getExternalReference(), batch.getId(),
                batch.getAttemptCount(), batch.getStatus(), batch.getErrorMessage());
        }
    }

    private String canonicalJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(sortRecursively(node));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload JSON inválido.");
        }
    }

    private JsonNode sortRecursively(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = objectMapper.createObjectNode();
            java.util.TreeSet<String> names = new java.util.TreeSet<>();
            node.fieldNames().forEachRemaining(names::add);
            names.forEach(name -> sorted.set(name, sortRecursively(node.get(name))));
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode sorted = objectMapper.createArrayNode();
            node.forEach(value -> sorted.add(sortRecursively(value)));
            return sorted;
        }
        return node;
    }

    private String sha256(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private String safeError(Exception exception) {
        if (!(exception instanceof IllegalArgumentException)
            && !(exception instanceof jakarta.persistence.EntityNotFoundException)) {
            return exception.getClass().getSimpleName() + " (falha transitória)";
        }
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private SyncBatchResponse toResponse(SyncBatch batch) {
        return new SyncBatchResponse(
            batch.getId(), batch.getSchemaVersion(), batch.getEntityType(),
            batch.getSource(), batch.getExternalReference(), batch.getStatus(),
            batch.getErrorMessage(), batch.getAttemptCount(), batch.getCreatedAt(), batch.getUpdatedAt()
        );
    }
}
