package com.agrotis.backend.service;

import com.agrotis.backend.domain.Address;
import com.agrotis.backend.domain.Laboratory;
import com.agrotis.backend.domain.Producer;
import com.agrotis.backend.domain.Property;
import com.agrotis.backend.dto.IntegrationRequest;
import com.agrotis.backend.dto.InscricaoFiscalValidator;
import com.agrotis.backend.repository.LaboratoryRepository;
import com.agrotis.backend.repository.ProducerRepository;
import com.agrotis.backend.repository.PropertyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationPayloadProcessor {

    private final ProducerRepository producerRepository;
    private final PropertyRepository propertyRepository;
    private final LaboratoryRepository laboratoryRepository;

    public IntegrationPayloadProcessor(ProducerRepository producerRepository,
                                       PropertyRepository propertyRepository,
                                       LaboratoryRepository laboratoryRepository) {
        this.producerRepository = producerRepository;
        this.propertyRepository = propertyRepository;
        this.laboratoryRepository = laboratoryRepository;
    }

    @Transactional
    public void process(IntegrationRequest.EntityType entityType, JsonNode payload) {
        switch (entityType) {
            case PRODUCER -> upsertProducer(payload);
            case PROPERTY -> upsertProperty(payload);
            case LABORATORY -> upsertLaboratory(payload);
        }
    }

    private Producer upsertProducer(JsonNode payload) {
        String fiscalId = requiredText(payload, "inscricaoFiscal");
        validateFiscalId(fiscalId);
        Producer producer = producerRepository.findByInscricaoFiscal(fiscalId).orElseGet(Producer::new);
        producer.setNome(requiredText(payload, "nome"));
        producer.setInscricaoFiscal(fiscalId);
        producer.setEndereco(address(payload.get("endereco")));
        producer.setDataInicial(toUtc(payload.path("dataInicial")));
        producer.setDataFinal(toUtc(payload.path("dataFinal")));
        producer.setObservacoes(optionalText(payload, "observacoes"));

        JsonNode laboratoryNode = payload.get("laboratorio");
        Laboratory laboratory;
        if (laboratoryNode != null && laboratoryNode.isObject()) {
            laboratory = upsertLaboratory(laboratoryNode);
        } else if (payload.hasNonNull("laboratorioId")) {
            laboratory = laboratoryRepository.findById(payload.path("laboratorioId").asLong())
                .orElseThrow(() -> new EntityNotFoundException("Laboratório não encontrado"));
        } else {
            throw new IllegalArgumentException("Informe laboratorio ou laboratorioId.");
        }
        producer.setLaboratorio(laboratory);
        producer = producerRepository.saveAndFlush(producer);

        JsonNode propertiesNode = payload.get("propriedades");
        if (propertiesNode != null) {
            if (!propertiesNode.isArray()) {
                throw new IllegalArgumentException("propriedades deve ser uma lista.");
            }
            List<Property> properties = new ArrayList<>();
            for (JsonNode propertyNode : propertiesNode) {
                Property property = propertyNode.isNumber()
                    ? propertyRepository.findById(propertyNode.asLong())
                        .orElseThrow(() -> new EntityNotFoundException("Propriedade não encontrada"))
                    : upsertProperty(propertyNode);
                if (property.getProdutor() != null
                    && !property.getProdutor().getId().equals(producer.getId())) {
                    throw new IllegalArgumentException(
                        "A propriedade já pertence a outro produtor e não pode ser reassociada automaticamente.");
                }
                property.setProdutor(producer);
                properties.add(property);
            }
            propertyRepository.saveAll(properties);
        }
        return producer;
    }

    private Property upsertProperty(JsonNode payload) {
        String fiscalId = requiredText(payload, "inscricaoFiscal");
        validateFiscalId(fiscalId);
        Property property = propertyRepository.findByInscricaoFiscal(fiscalId).orElseGet(Property::new);
        property.setNome(requiredText(payload, "nome"));
        property.setInscricaoFiscal(fiscalId);
        property.setEndereco(address(payload.get("endereco")));
        String producerFiscalId = optionalText(payload, "produtorInscricaoFiscal");
        if (producerFiscalId != null && !producerFiscalId.isBlank()) {
            validateFiscalId(producerFiscalId);
            Producer owner = producerRepository.findByInscricaoFiscal(producerFiscalId)
                .orElseThrow(() -> new EntityNotFoundException("Produtor responsável não encontrado"));
            if (property.getProdutor() != null && !property.getProdutor().getId().equals(owner.getId())) {
                throw new IllegalArgumentException(
                    "A propriedade já pertence a outro produtor e não pode ser reassociada automaticamente.");
            }
            property.setProdutor(owner);
        }
        return propertyRepository.saveAndFlush(property);
    }

    private Laboratory upsertLaboratory(JsonNode payload) {
        String fiscalId = requiredText(payload, "inscricaoFiscal");
        validateFiscalId(fiscalId);
        Laboratory laboratory = laboratoryRepository.findByInscricaoFiscal(fiscalId).orElseGet(Laboratory::new);
        laboratory.setNome(requiredText(payload, "nome"));
        laboratory.setInscricaoFiscal(fiscalId);
        laboratory.setEndereco(address(payload.get("endereco")));
        return laboratoryRepository.saveAndFlush(laboratory);
    }

    private Address address(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("endereco deve ser informado como objeto.");
        }
        return new Address(
            requiredText(node, "logradouro"),
            requiredText(node, "cidade"),
            requiredText(node, "estado"),
            requiredText(node, "cep")
        );
    }

    private String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório.");
        }
        return value;
    }

    private String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private java.time.LocalDateTime toUtc(JsonNode value) {
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value.asText()).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private void validateFiscalId(String value) {
        if (!new InscricaoFiscalValidator().isValid(value, null)) {
            throw new IllegalArgumentException("inscricaoFiscal deve ser um CPF ou CNPJ válido.");
        }
    }
}
