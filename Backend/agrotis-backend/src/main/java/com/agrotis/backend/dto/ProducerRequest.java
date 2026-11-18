package com.agrotis.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

public record ProducerRequest(
    @NotBlank String nome,
    @NotBlank @ValidInscricaoFiscal String inscricaoFiscal,
    @NotNull @Valid AddressDto endereco,
    @NotEmpty List<Long> propriedadeIds,
    @NotNull Long laboratorioId,
    OffsetDateTime dataInicial,
    OffsetDateTime dataFinal,
    String observacoes
) {
}
