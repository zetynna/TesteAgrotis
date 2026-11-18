package com.agrotis.backend.controller;

import com.agrotis.backend.dto.CepAddressResponse;
import com.agrotis.backend.service.CepLookupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cep")
public class CepController {

    private final CepLookupService cepLookupService;

    public CepController(CepLookupService cepLookupService) {
        this.cepLookupService = cepLookupService;
    }

    @GetMapping("/{cep}")
    public CepAddressResponse findByCep(@PathVariable String cep) {
        return cepLookupService.findByCep(cep);
    }
}
