package com.agrotis.backend.service;

import com.agrotis.backend.domain.Laboratory;
import com.agrotis.backend.domain.Producer;
import com.agrotis.backend.domain.Property;
import com.agrotis.backend.dto.AnalyticsLaboratoryResponse;
import com.agrotis.backend.repository.LaboratoryRepository;
import com.agrotis.backend.repository.PropertyRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LaboratoryAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(LaboratoryAnalyticsService.class);
    private final LaboratoryRepository laboratoryRepository;
    private final PropertyRepository propertyRepository;
    private final FinancialCalculationService financialCalculationService;

    public LaboratoryAnalyticsService(LaboratoryRepository laboratoryRepository,
                                      PropertyRepository propertyRepository,
                                      FinancialCalculationService financialCalculationService) {
        this.laboratoryRepository = laboratoryRepository;
        this.propertyRepository = propertyRepository;
        this.financialCalculationService = financialCalculationService;
    }

    public List<AnalyticsLaboratoryResponse> analyze(
        LocalDateTime dataInicialInicio,
        LocalDateTime dataInicialFim,
        LocalDateTime dataFinalInicio,
        LocalDateTime dataFinalFim,
        String buscaTexto,
        Integer qtdMinimaProdutores
    ) {
        if (qtdMinimaProdutores == null || qtdMinimaProdutores < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "qtdMinimaProdutores é obrigatório e deve ser maior ou igual a zero.");
        }
        validateRange(dataInicialInicio, dataInicialFim, "dataInicial");
        validateRange(dataFinalInicio, dataFinalFim, "dataFinal");
        boolean initialDateFilter = dataInicialInicio != null || dataInicialFim != null;

        List<Laboratory> laboratories = laboratoryRepository.findAllByOrderByNomeAsc();
        List<Long> producerIds = laboratories.stream().flatMap(laboratory -> laboratory.getProdutores().stream())
            .map(Producer::getId).filter(Objects::nonNull).distinct().toList();
        java.util.Map<Long, List<Property>> propertiesByProducer = producerIds.isEmpty()
            ? java.util.Map.of()
            : propertyRepository.findAllByProdutor_IdIn(producerIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(property -> property.getProdutor().getId()));

        List<AggregatedLaboratory> results = laboratories.stream()
            .map(laboratory -> toResponse(laboratory, dataInicialInicio, dataInicialFim, dataFinalInicio,
                dataFinalFim, buscaTexto, qtdMinimaProdutores, propertiesByProducer))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt((AggregatedLaboratory result) -> result.response().totalProdutores())
                .reversed()
                .thenComparing(result -> initialDateFilter ? result.earliestInitialDate() : null,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(result -> result.response().id() == null
                    ? Long.MAX_VALUE : result.response().id()))
            .toList();
        log.info("Laboratory analytics completed resultCount={} minimumProducers={} initialDateFilter={}",
            results.size(), qtdMinimaProdutores, initialDateFilter);
        return results.stream().map(AggregatedLaboratory::response).toList();
    }

    private AggregatedLaboratory toResponse(
        Laboratory laboratory,
        LocalDateTime dataInicialInicio,
        LocalDateTime dataInicialFim,
        LocalDateTime dataFinalInicio,
        LocalDateTime dataFinalFim,
        String buscaTexto,
        int minimum,
        java.util.Map<Long, List<Property>> propertiesByProducer
    ) {
        List<Producer> filtered = laboratory.getProdutores().stream()
            .filter(producer -> matchesInitialRange(producer, dataInicialInicio, dataInicialFim))
            .filter(producer -> matchesFinalRange(producer, dataFinalInicio, dataFinalFim))
            .filter(producer -> matchesObservacoes(producer, buscaTexto))
            .toList();

        if (filtered.size() < minimum) {
            return null;
        }

        BigDecimal producerCommissions = filtered.stream()
            .map(financialCalculationService::calculateProducerValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal propertyOperations = filtered.stream()
            .flatMap(producer -> propertiesByProducer.getOrDefault(producer.getId(), List.of()).stream())
            .distinct()
            .map(financialCalculationService::calculatePropertyValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal laboratoryTerritorialFee = financialCalculationService.calculateLaboratoryValue(filtered.size());
        BigDecimal total = producerCommissions.add(propertyOperations).add(laboratoryTerritorialFee);

        LocalDateTime earliestDate = filtered.stream().map(Producer::getDataInicial)
            .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        return new AggregatedLaboratory(new AnalyticsLaboratoryResponse(
            laboratory.getId(), String.valueOf(laboratory.getId()),
            laboratory.getNome().toUpperCase(Locale.ROOT), filtered.size(), total), earliestDate);
    }

    private boolean matchesInitialRange(Producer producer, LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null && fim == null) {
            return true;
        }
        LocalDateTime value = producer.getDataInicial();
        if (value == null) {
            return false;
        }
        if (inicio != null && value.isBefore(inicio)) {
            return false;
        }
        if (fim != null && value.isAfter(fim)) {
            return false;
        }
        return true;
    }

    private boolean matchesFinalRange(Producer producer, LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null && fim == null) {
            return true;
        }
        LocalDateTime value = producer.getDataFinal();
        if (value == null) {
            return false;
        }
        if (inicio != null && value.isBefore(inicio)) {
            return false;
        }
        if (fim != null && value.isAfter(fim)) {
            return false;
        }
        return true;
    }

    private boolean matchesObservacoes(Producer producer, String buscaTexto) {
        if (buscaTexto == null || buscaTexto.isBlank()) {
            return true;
        }
        return producer.getObservacoes() != null
            && producer.getObservacoes().toLowerCase(Locale.ROOT).contains(buscaTexto.toLowerCase(Locale.ROOT));
    }

    private void validateRange(LocalDateTime start, LocalDateTime end, String label) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                label + " início deve ser anterior ou igual ao fim.");
        }
    }

    private record AggregatedLaboratory(AnalyticsLaboratoryResponse response, LocalDateTime earliestInitialDate) {
    }
}
