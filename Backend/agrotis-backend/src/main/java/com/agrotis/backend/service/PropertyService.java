package com.agrotis.backend.service;

import com.agrotis.backend.domain.Address;
import com.agrotis.backend.domain.Property;
import com.agrotis.backend.dto.AddressDto;
import com.agrotis.backend.dto.PropertyDto;
import com.agrotis.backend.repository.PropertyRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PropertyService {

    private static final Logger log = LoggerFactory.getLogger(PropertyService.class);
    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Transactional(readOnly = true)
    public List<PropertyDto> listAll() {
        return propertyRepository.findAllByOrderByNomeAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public PropertyDto create(String nome, String inscricaoFiscal, AddressDto endereco) {
        Property property = new Property(nome, inscricaoFiscal, toAddress(endereco));
        PropertyDto response = toDto(propertyRepository.save(property));
        log.info("Property created id={}", response.id());
        return response;
    }

    @Transactional
    public PropertyDto update(Long id, String nome, String inscricaoFiscal, AddressDto endereco) {
        Property property = propertyRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Propriedade não encontrada"));
        property.setNome(nome);
        property.setInscricaoFiscal(inscricaoFiscal);
        property.setEndereco(toAddress(endereco));
        PropertyDto response = toDto(propertyRepository.save(property));
        log.info("Property updated id={}", response.id());
        return response;
    }

    @Transactional
    public void delete(Long id) {
        Property property = propertyRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Propriedade não encontrada"));
        if (property.getProdutor() != null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT,
                "Desvincule a propriedade do produtor antes de excluí-la.");
        }
        propertyRepository.delete(property);
        log.info("Property deleted id={}", id);
    }

    private PropertyDto toDto(Property property) {
        return new PropertyDto(
            property.getId(),
            property.getNome(),
            property.getInscricaoFiscal(),
            new AddressDto(property.getEndereco().getLogradouro(), property.getEndereco().getCidade(), property.getEndereco().getEstado(), property.getEndereco().getCep()),
            property.getProdutor() == null ? null : property.getProdutor().getId(),
            property.getProdutor() == null ? null : property.getProdutor().getNome()
        );
    }

    private Address toAddress(AddressDto addressDto) {
        return new Address(addressDto.logradouro(), addressDto.cidade(), addressDto.estado(), addressDto.cep());
    }
}
