package com.agrotis.backend.service;

import com.agrotis.backend.domain.Address;
import com.agrotis.backend.domain.Laboratory;
import com.agrotis.backend.dto.AddressDto;
import com.agrotis.backend.dto.LaboratoryDto;
import com.agrotis.backend.repository.LaboratoryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LaboratoryService {

    private static final Logger log = LoggerFactory.getLogger(LaboratoryService.class);
    private final LaboratoryRepository laboratoryRepository;

    public LaboratoryService(LaboratoryRepository laboratoryRepository) {
        this.laboratoryRepository = laboratoryRepository;
    }

    @Transactional(readOnly = true)
    public List<LaboratoryDto> listAll() {
        return laboratoryRepository.findAllByOrderByNomeAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public LaboratoryDto create(String nome, String inscricaoFiscal, AddressDto endereco) {
        Laboratory laboratory = new Laboratory(nome, inscricaoFiscal, toAddress(endereco));
        LaboratoryDto response = toDto(laboratoryRepository.save(laboratory));
        log.info("Laboratory created id={}", response.id());
        return response;
    }

    @Transactional
    public LaboratoryDto update(Long id, String nome, String inscricaoFiscal, AddressDto endereco) {
        Laboratory laboratory = laboratoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Laboratório não encontrado"));
        laboratory.setNome(nome);
        laboratory.setInscricaoFiscal(inscricaoFiscal);
        laboratory.setEndereco(toAddress(endereco));
        LaboratoryDto response = toDto(laboratoryRepository.save(laboratory));
        log.info("Laboratory updated id={}", response.id());
        return response;
    }

    @Transactional
    public void delete(Long id) {
        Laboratory laboratory = laboratoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Laboratório não encontrado"));
        if (!laboratory.getProdutores().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Transfira os produtores antes de excluir o laboratório.");
        }
        laboratoryRepository.delete(laboratory);
        log.info("Laboratory deleted id={}", id);
    }

    private LaboratoryDto toDto(Laboratory laboratory) {
        Address address = laboratory.getEndereco();
        return new LaboratoryDto(laboratory.getId(), laboratory.getNome(), laboratory.getInscricaoFiscal(),
            new AddressDto(address.getLogradouro(), address.getCidade(), address.getEstado(), address.getCep()));
    }

    private Address toAddress(AddressDto address) {
        return new Address(address.logradouro(), address.cidade(), address.estado(), address.cep());
    }
}
