package com.agrotis.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.agrotis.backend.domain.Address;
import com.agrotis.backend.domain.Laboratory;
import com.agrotis.backend.domain.Producer;
import com.agrotis.backend.domain.Property;
import com.agrotis.backend.repository.LaboratoryRepository;
import com.agrotis.backend.repository.PropertyRepository;
import com.agrotis.backend.service.FinancialCalculationService;
import com.agrotis.backend.service.LaboratoryAnalyticsService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class LaboratoryAnalyticsServiceTest {

    @Test
    void filtersCountsOrdersAndConsolidatesConfiguredFinancialRates() {
        var repository = org.mockito.Mockito.mock(LaboratoryRepository.class);
        var properties = org.mockito.Mockito.mock(PropertyRepository.class);
        var financial = new FinancialCalculationService(
            new BigDecimal("10"), new BigDecimal("2"), new BigDecimal("3"));
        var service = new LaboratoryAnalyticsService(repository, properties, financial);
        var laboratoryA = new Laboratory("Laboratório A", "a", new Address("Rua", "Cidade", "PR", "80000000"));
        var laboratoryB = new Laboratory("Laboratório B", "b", new Address("Rua", "Cidade", "PR", "80000000"));
        Producer a1 = producer("a1", "2026-05-02T00:00:00", "2026-05-03T00:00:00", "início");
        Producer a2 = producer("a2", "2026-05-03T00:00:00", "2026-05-05T00:00:00", "outro");
        Property property = new Property("Fazenda", "property", new Address("Rua", "Cidade", "PR", "80000000"));
        property.setProdutor(a1);
        a1.setPropriedades(List.of(property));
        Producer b1 = producer("b1", "2026-05-01T00:00:00", "2026-05-03T00:00:00", "início");
        Producer b2 = producer("b2", "2026-05-04T00:00:00", "2026-05-07T00:00:00", "outro");
        org.springframework.test.util.ReflectionTestUtils.setField(a1, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(a2, "id", 2L);
        org.springframework.test.util.ReflectionTestUtils.setField(b1, "id", 3L);
        org.springframework.test.util.ReflectionTestUtils.setField(b2, "id", 4L);
        laboratoryA.getProdutores().addAll(List.of(a1, a2));
        laboratoryB.getProdutores().addAll(List.of(b1, b2));
        when(repository.findAllByOrderByNomeAsc()).thenReturn(List.of(laboratoryA, laboratoryB));
        when(properties.findAllByProdutor_IdIn(List.of(1L, 2L, 3L, 4L))).thenReturn(List.of(property));

        var rows = service.analyze(LocalDateTime.parse("2026-05-01T00:00:00"),
            LocalDateTime.parse("2026-05-04T00:00:00"), null, null, null, 2);

        assertEquals(List.of("LABORATÓRIO B", "LABORATÓRIO A"),
            rows.stream().map(row -> row.nome()).toList());
        assertEquals(2, rows.get(0).totalProdutores());
        assertEquals(new BigDecimal("56.00"), rows.get(0).valorFinanceiroConsolidado());
        assertEquals(new BigDecimal("38.00"), rows.get(1).valorFinanceiroConsolidado());

        var textAndDateFiltered = service.analyze(LocalDateTime.parse("2026-05-02T00:00:00"),
            LocalDateTime.parse("2026-05-04T00:00:00"), null, null, "OUTRO", 1);
        assertEquals(List.of("LABORATÓRIO A", "LABORATÓRIO B"),
            textAndDateFiltered.stream().map(row -> row.nome()).toList());
        assertEquals(List.of(23, 33), textAndDateFiltered.stream()
            .map(row -> row.valorFinanceiroConsolidado().intValue()).toList());
    }

    private Producer producer(String fiscalId, String start, String end, String notes) {
        return new Producer(fiscalId, fiscalId, new Address("Rua", "Cidade", "PR", "80000000"), null,
            LocalDateTime.parse(start), LocalDateTime.parse(end), notes);
    }
}
