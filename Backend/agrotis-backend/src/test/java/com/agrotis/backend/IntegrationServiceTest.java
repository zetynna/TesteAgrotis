package com.agrotis.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agrotis.backend.domain.SyncStatus;
import com.agrotis.backend.dto.IntegrationRequest;
import com.agrotis.backend.repository.LaboratoryRepository;
import com.agrotis.backend.repository.ProducerRepository;
import com.agrotis.backend.repository.PropertyRepository;
import com.agrotis.backend.repository.SyncBatchRepository;
import com.agrotis.backend.service.IntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@TestPropertySource(properties = {
    "agrotis.integration.poll-delay-ms=60000",
    "spring.datasource.url=jdbc:h2:mem:agrotis-integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1"
})
class IntegrationServiceTest {

    @Autowired private IntegrationService integrationService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SyncBatchRepository syncBatchRepository;
    @Autowired private ProducerRepository producerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private LaboratoryRepository laboratoryRepository;

    @Test
    void ingestionIsIdempotentBySourceAndReferenceAndRejectsChangedPayload() {
        String reference = UUID.randomUUID().toString();
        ObjectNode firstPayload = objectMapper.createObjectNode();
        firstPayload.put("nome", "laboratório");
        firstPayload.put("inscricaoFiscal", "12.345.678/0001-95");
        ObjectNode reorderedPayload = objectMapper.createObjectNode();
        reorderedPayload.put("inscricaoFiscal", "12.345.678/0001-95");
        reorderedPayload.put("nome", "laboratório");

        var first = integrationService.start(request(reference, IntegrationRequest.EntityType.LABORATORY, firstPayload));
        var retry = integrationService.start(request(reference, IntegrationRequest.EntityType.LABORATORY, reorderedPayload));
        assertEquals(first.id(), retry.id());
        assertThrows(ResponseStatusException.class, () -> integrationService.start(request(
            reference, IntegrationRequest.EntityType.LABORATORY,
            objectMapper.createObjectNode().put("nome", "payload diferente"))));
    }

    @Test
    void producerPayloadUpsertsByFiscalRegistrationAndLinksOwnedProperties() throws Exception {
        String producerFiscal = "333.444.555-08";
        String propertyFiscal = "444.555.666-19";
        String laboratoryFiscal = "55.566.677/0001-83";
        String payloadJson = """
            {
              "nome":"Produtor integrado",
              "inscricaoFiscal":"%s",
              "endereco":{"logradouro":"Rua A","cidade":"Curitiba","estado":"PR","cep":"80000000"},
              "dataInicial":"2026-06-11T12:00:00Z",
              "dataFinal":"2026-06-15T18:00:00Z",
              "observacoes":"Teste de reconciliação",
              "laboratorio":{"nome":"Laboratório integrado","inscricaoFiscal":"%s",
                "endereco":{"logradouro":"Rua B","cidade":"Curitiba","estado":"PR","cep":"80000000"}},
              "propriedades":[{"nome":"Fazenda integrada","inscricaoFiscal":"%s",
                "endereco":{"logradouro":"Rua C","cidade":"Curitiba","estado":"PR","cep":"80000000"}}]
            }
            """.formatted(producerFiscal, laboratoryFiscal, propertyFiscal);
        ObjectNode payload = (ObjectNode) objectMapper.readTree(payloadJson);
        String reference = UUID.randomUUID().toString();

        var batch = integrationService.start(request(reference, IntegrationRequest.EntityType.PRODUCER, payload));
        integrationService.processOne(batch.id());
        var firstProducer = producerRepository.findByInscricaoFiscal(producerFiscal).orElseThrow();
        var property = propertyRepository.findByInscricaoFiscal(propertyFiscal).orElseThrow();
        assertEquals(firstProducer.getId(), property.getProdutor().getId());
        assertTrue(laboratoryRepository.findByInscricaoFiscal(laboratoryFiscal).isPresent());
        assertEquals(SyncStatus.SUCCEEDED, syncBatchRepository.findById(batch.id()).orElseThrow().getStatus());

        payload.put("nome", "Produtor atualizado");
        var secondBatch = integrationService.start(request(
            UUID.randomUUID().toString(), IntegrationRequest.EntityType.PRODUCER, payload));
        integrationService.processOne(secondBatch.id());
        var updatedProducer = producerRepository.findByInscricaoFiscal(producerFiscal).orElseThrow();
        assertEquals(firstProducer.getId(), updatedProducer.getId());
        assertEquals("Produtor atualizado", updatedProducer.getNome());

        payload.put("nome", "Segundo produtor");
        payload.put("inscricaoFiscal", "222.333.444-05");
        var conflictingBatch = integrationService.start(request(
            UUID.randomUUID().toString(), IntegrationRequest.EntityType.PRODUCER, payload));
        integrationService.processOne(conflictingBatch.id());
        assertEquals(SyncStatus.DLQ,
            syncBatchRepository.findById(conflictingBatch.id()).orElseThrow().getStatus());
        assertEquals(firstProducer.getId(),
            propertyRepository.findByInscricaoFiscal(propertyFiscal).orElseThrow().getProdutor().getId());
        assertTrue(producerRepository.findByInscricaoFiscal("222.333.444-05").isEmpty());
    }

    @Test
    void permanentFailureMovesToDlqAndCanBeManuallyRequeued() {
        String reference = UUID.randomUUID().toString();
        var batch = integrationService.start(request(reference, IntegrationRequest.EntityType.LABORATORY,
            objectMapper.createObjectNode().put("nome", "sem inscrição fiscal")));

        integrationService.processOne(batch.id());
        var failed = syncBatchRepository.findById(batch.id()).orElseThrow();
        assertEquals(SyncStatus.DLQ, failed.getStatus());
        assertEquals(1, failed.getAttemptCount());

        assertEquals(SyncStatus.PENDING, integrationService.reprocess(batch.id()).status());
        integrationService.processOne(batch.id());
        assertEquals(SyncStatus.DLQ, syncBatchRepository.findById(batch.id()).orElseThrow().getStatus());
    }

    @Test
    void standalonePropertyPayloadCanLinkToExistingProducerWithoutReassignment() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("nome", "Propriedade recebida");
        payload.put("inscricaoFiscal", "333.444.555-08");
        payload.put("produtorInscricaoFiscal", "666.777.888-30");
        payload.set("endereco", objectMapper.createObjectNode()
            .put("logradouro", "Estrada Rural")
            .put("cidade", "Lavras")
            .put("estado", "MG")
            .put("cep", "37200000"));
        var batch = integrationService.start(request(
            UUID.randomUUID().toString(), IntegrationRequest.EntityType.PROPERTY, payload));

        integrationService.processOne(batch.id());

        var property = propertyRepository.findByInscricaoFiscal("333.444.555-08").orElseThrow();
        assertEquals(SyncStatus.SUCCEEDED, syncBatchRepository.findById(batch.id()).orElseThrow().getStatus());
        assertEquals(producerRepository.findByInscricaoFiscal("666.777.888-30").orElseThrow().getId(),
            property.getProdutor().getId());
    }

    private IntegrationRequest request(String reference, IntegrationRequest.EntityType type,
                                       com.fasterxml.jackson.databind.JsonNode payload) {
        return new IntegrationRequest(1, "integration-test", reference, type, payload);
    }
}
