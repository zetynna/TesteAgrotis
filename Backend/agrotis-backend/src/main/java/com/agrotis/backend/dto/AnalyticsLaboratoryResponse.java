package com.agrotis.backend.dto;

import java.math.BigDecimal;

public record AnalyticsLaboratoryResponse(Long id, String codigo, String nome,
                                          Integer totalProdutores,
                                          BigDecimal valorFinanceiroConsolidado) {
}
