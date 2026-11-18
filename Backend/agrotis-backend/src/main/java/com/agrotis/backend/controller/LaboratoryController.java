package com.agrotis.backend.controller;

import com.agrotis.backend.dto.CatalogEntityRequest;
import com.agrotis.backend.dto.LaboratoryDto;
import com.agrotis.backend.service.LaboratoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/laboratories")
public class LaboratoryController {

    private final LaboratoryService laboratoryService;

    public LaboratoryController(LaboratoryService laboratoryService) {
        this.laboratoryService = laboratoryService;
    }

    @GetMapping
    public List<LaboratoryDto> list() {
        return laboratoryService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LaboratoryDto create(@Valid @RequestBody CatalogEntityRequest request) {
        return laboratoryService.create(request.nome(), request.inscricaoFiscal(), request.endereco());
    }

    @PutMapping("/{id}")
    public LaboratoryDto update(@PathVariable Long id, @Valid @RequestBody CatalogEntityRequest request) {
        return laboratoryService.update(id, request.nome(), request.inscricaoFiscal(), request.endereco());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        laboratoryService.delete(id);
    }
}
