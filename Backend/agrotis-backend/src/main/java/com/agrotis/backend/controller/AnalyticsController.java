package com.agrotis.backend.controller;

import com.agrotis.backend.dto.AnalyticsLaboratoryResponse;
import com.agrotis.backend.service.LaboratoryAnalyticsService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api")
@Validated
public class AnalyticsController {

    private final LaboratoryAnalyticsService laboratoryAnalyticsService;

    public AnalyticsController(LaboratoryAnalyticsService laboratoryAnalyticsService) {
        this.laboratoryAnalyticsService = laboratoryAnalyticsService;
    }

    @GetMapping("/analytics/laboratories")
    public List<AnalyticsLaboratoryResponse> laboratories(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicialInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicialFim,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFinalInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFinalFim,
        @RequestParam(required = false) String buscaTexto,
        @RequestParam @NotNull @Min(0) Integer qtdMinimaProdutores
    ) {
        return laboratoryAnalyticsService.analyze(
            dataInicialInicio,
            dataInicialFim,
            dataFinalInicio,
            dataFinalFim,
            buscaTexto,
            qtdMinimaProdutores
        );
    }
}
