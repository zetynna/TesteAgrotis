package com.agrotis.backend.controller;

import com.agrotis.backend.dto.CatalogEntityRequest;
import com.agrotis.backend.dto.PropertyDto;
import com.agrotis.backend.service.PropertyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    public List<PropertyDto> list() {
        return propertyService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyDto create(@Valid @RequestBody CatalogEntityRequest request) {
        return propertyService.create(request.nome(), request.inscricaoFiscal(), request.endereco());
    }

    @PutMapping("/{id}")
    public PropertyDto update(@PathVariable Long id,
                              @Valid @RequestBody CatalogEntityRequest request) {
        return propertyService.update(id, request.nome(), request.inscricaoFiscal(), request.endereco());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        propertyService.delete(id);
    }
}
