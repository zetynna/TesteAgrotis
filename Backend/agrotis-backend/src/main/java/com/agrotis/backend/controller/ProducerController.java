package com.agrotis.backend.controller;

import com.agrotis.backend.dto.ProducerRequest;
import com.agrotis.backend.dto.ProducerResponse;
import com.agrotis.backend.service.ProducerService;
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
@RequestMapping("/api/producers")
public class ProducerController {

    private final ProducerService producerService;

    public ProducerController(ProducerService producerService) {
        this.producerService = producerService;
    }

    @GetMapping
    public List<ProducerResponse> list() {
        return producerService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProducerResponse create(@Valid @RequestBody ProducerRequest request) {
        return producerService.create(request);
    }

    @PutMapping("/{id}")
    public ProducerResponse update(@PathVariable Long id, @Valid @RequestBody ProducerRequest request) {
        return producerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        producerService.delete(id);
    }
}
