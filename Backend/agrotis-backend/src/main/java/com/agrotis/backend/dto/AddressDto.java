package com.agrotis.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressDto(@NotBlank String logradouro, @NotBlank String cidade,
                         @NotBlank String estado, @NotBlank String cep) {
}
