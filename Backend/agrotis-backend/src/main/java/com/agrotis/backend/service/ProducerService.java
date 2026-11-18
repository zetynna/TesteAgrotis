package com.agrotis.backend.service;

import com.agrotis.backend.domain.Address;
import com.agrotis.backend.domain.Laboratory;
import com.agrotis.backend.domain.Producer;
import com.agrotis.backend.domain.Property;
import com.agrotis.backend.dto.AddressDto;
import com.agrotis.backend.dto.LaboratoryDto;
import com.agrotis.backend.dto.ProducerRequest;
import com.agrotis.backend.dto.ProducerResponse;
import com.agrotis.backend.dto.PropertyDto;
import com.agrotis.backend.repository.LaboratoryRepository;
import com.agrotis.backend.repository.ProducerRepository;
import com.agrotis.backend.repository.PropertyRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProducerService {

    private static final Logger log = LoggerFactory.getLogger(ProducerService.class);
    private final ProducerRepository producerRepository;
    private final PropertyRepository propertyRepository;
    private final LaboratoryRepository laboratoryRepository;

    public ProducerService(ProducerRepository producerRepository,
                           PropertyRepository propertyRepository,
                           LaboratoryRepository laboratoryRepository) {
        this.producerRepository = producerRepository;
        this.propertyRepository = propertyRepository;
        this.laboratoryRepository = laboratoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ProducerResponse> listAll() {
        return producerRepository.findAllByOrderByNomeAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ProducerResponse create(ProducerRequest request) {
        Laboratory laboratory = laboratoryRepository.findById(request.laboratorioId())
            .orElseThrow(() -> new EntityNotFoundException("Laboratório não encontrado"));

        List<Property> properties = request.propriedadeIds().stream().distinct()
            .map(id -> propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Propriedade não encontrada: " + id)))
            .toList();
        ensurePropertiesAvailable(properties, null);

        Producer producer = new Producer(
            request.nome(),
            request.inscricaoFiscal(),
            toAddress(request.endereco()),
            laboratory,
            toUtcLocalDateTime(request.dataInicial()),
            toUtcLocalDateTime(request.dataFinal()),
            request.observacoes()
        );
        Producer saved = producerRepository.saveAndFlush(producer);
        assignProperties(saved, properties);
        log.info("Producer created id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ProducerResponse update(Long id, ProducerRequest request) {
        Producer producer = producerRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Produtor não encontrado"));

        Laboratory laboratory = laboratoryRepository.findById(request.laboratorioId())
            .orElseThrow(() -> new EntityNotFoundException("Laboratório não encontrado"));

        List<Property> properties = request.propriedadeIds().stream().distinct()
            .map(propertyId -> propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Propriedade não encontrada: " + propertyId)))
            .toList();

        ensurePropertiesAvailable(properties, producer.getId());

        producer.setNome(request.nome());
        producer.setInscricaoFiscal(request.inscricaoFiscal());
        producer.setEndereco(toAddress(request.endereco()));
        producer.setLaboratorio(laboratory);
        producer.setDataInicial(toUtcLocalDateTime(request.dataInicial()));
        producer.setDataFinal(toUtcLocalDateTime(request.dataFinal()));
        producer.setObservacoes(request.observacoes());
        producer = producerRepository.saveAndFlush(producer);
        assignProperties(producer, properties);
        log.info("Producer updated id={}", producer.getId());
        return toResponse(producer);
    }

    @Transactional
    public void delete(Long id) {
        Producer producer = producerRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Produtor não encontrado"));
        List<Property> properties = new ArrayList<>(producer.getPropriedades());
        properties.forEach(property -> property.setProdutor(null));
        propertyRepository.saveAll(properties);
        producerRepository.delete(producer);
        log.info("Producer deleted id={}", id);
    }

    private ProducerResponse toResponse(Producer producer) {
        return new ProducerResponse(
            producer.getId(),
            producer.getNome(),
            producer.getInscricaoFiscal(),
            toAddressDto(producer.getEndereco()),
            producer.getPropriedades().stream().map(property -> toPropertyDto(property, producer)).toList(),
            toLaboratoryDto(producer.getLaboratorio()),
            producer.getDataInicial(),
            producer.getDataFinal(),
            producer.getObservacoes()
        );
    }

    private void assignProperties(Producer producer, List<Property> selected) {
        List<Long> selectedIds = selected.stream().map(Property::getId).toList();
        List<Property> current = new ArrayList<>(producer.getPropriedades());
        current.stream()
            .filter(property -> !selectedIds.contains(property.getId()))
            .forEach(property -> property.setProdutor(null));
        selected.forEach(property -> property.setProdutor(producer));
        producer.setPropriedades(selected);
        propertyRepository.saveAll(current.stream()
            .filter(property -> !selectedIds.contains(property.getId()))
            .toList());
        propertyRepository.saveAll(selected);
    }

    private void ensurePropertiesAvailable(List<Property> properties, Long producerId) {
        for (Property property : properties) {
            if (property.getProdutor() != null
                && !property.getProdutor().getId().equals(producerId)) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "A propriedade " + property.getId() + " já pertence a outro produtor.");
            }
        }
    }

    private PropertyDto toPropertyDto(Property property, Producer owner) {
        return new PropertyDto(
            property.getId(),
            property.getNome(),
            property.getInscricaoFiscal(),
            toAddressDto(property.getEndereco()),
            owner.getId(),
            owner.getNome()
        );
    }

    private LaboratoryDto toLaboratoryDto(Laboratory laboratory) {
        return new LaboratoryDto(
            laboratory.getId(),
            laboratory.getNome(),
            laboratory.getInscricaoFiscal(),
            toAddressDto(laboratory.getEndereco())
        );
    }

    private AddressDto toAddressDto(Address address) {
        return new AddressDto(address.getLogradouro(), address.getCidade(), address.getEstado(), address.getCep());
    }

    private Address toAddress(AddressDto dto) {
        return new Address(dto.logradouro(), dto.cidade(), dto.estado(), dto.cep());
    }

    private java.time.LocalDateTime toUtcLocalDateTime(java.time.OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
