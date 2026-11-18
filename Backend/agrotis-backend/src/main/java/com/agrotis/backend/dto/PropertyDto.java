package com.agrotis.backend.dto;

public record PropertyDto(Long id, String nome, String inscricaoFiscal, AddressDto endereco,
                          Long produtorId, String produtorNome) {
}
