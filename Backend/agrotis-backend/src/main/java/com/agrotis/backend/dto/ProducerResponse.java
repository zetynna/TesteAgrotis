package com.agrotis.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProducerResponse(
    Long id,
    String nome,
    String inscricaoFiscal,
    AddressDto endereco,
    List<PropertyDto> propriedades,
    LaboratoryDto laboratorio,
    LocalDateTime dataInicial,
    LocalDateTime dataFinal,
    String observacoes
) {
}
