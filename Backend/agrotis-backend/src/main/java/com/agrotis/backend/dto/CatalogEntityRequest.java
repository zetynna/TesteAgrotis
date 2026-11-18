package com.agrotis.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CatalogEntityRequest(
    @NotBlank @ValidInscricaoFiscal String inscricaoFiscal,
    @NotBlank String nome,
    @NotNull @Valid AddressDto endereco
) {
}
