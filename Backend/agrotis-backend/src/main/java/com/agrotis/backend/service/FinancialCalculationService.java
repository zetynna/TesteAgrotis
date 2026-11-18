package com.agrotis.backend.service;

import com.agrotis.backend.domain.Laboratory;
import com.agrotis.backend.domain.Producer;
import com.agrotis.backend.domain.Property;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FinancialCalculationService {

    private final BigDecimal producerCommissionRate;
    private final BigDecimal propertyOperationalRate;
    private final BigDecimal laboratoryTerritorialRate;

    public FinancialCalculationService(
        @Value("${agrotis.finance.producer-commission-rate:0}") BigDecimal producerCommissionRate,
        @Value("${agrotis.finance.property-operational-rate:0}") BigDecimal propertyOperationalRate,
        @Value("${agrotis.finance.laboratory-territorial-rate:0}") BigDecimal laboratoryTerritorialRate
    ) {
        this.producerCommissionRate = producerCommissionRate;
        this.propertyOperationalRate = propertyOperationalRate;
        this.laboratoryTerritorialRate = laboratoryTerritorialRate;
    }

    public BigDecimal calculateProducerValue(Producer producer) {
        if (producer.getDataInicial() == null || producer.getDataFinal() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long days = Math.max(Duration.between(producer.getDataInicial(), producer.getDataFinal()).toDays(), 0L);
        return producerCommissionRate.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculatePropertyValue(Property property) {
        return propertyOperationalRate.multiply(property.getProdutor() == null
            ? BigDecimal.ZERO : BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateLaboratoryValue(Laboratory laboratory) {
        long producerCount = laboratory.getProdutores() == null ? 0L : laboratory.getProdutores().size();
        return calculateLaboratoryValue(producerCount);
    }

    public BigDecimal calculateLaboratoryValue(long producerCount) {
        return laboratoryTerritorialRate.multiply(BigDecimal.valueOf(producerCount))
            .setScale(2, RoundingMode.HALF_UP);
    }
}
