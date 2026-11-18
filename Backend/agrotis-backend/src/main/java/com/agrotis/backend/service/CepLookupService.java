package com.agrotis.backend.service;

import com.agrotis.backend.dto.CepAddressResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CepLookupService {

    private final RestClient restClient;

    public CepLookupService(
        RestClient.Builder restClientBuilder,
        @Value("${agrotis.cep.base-url:https://viacep.com.br/ws}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public CepAddressResponse findByCep(String cep) {
        String normalizedCep = cep.replaceAll("\\D", "");
        if (!normalizedCep.matches("\\d{8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um CEP válido com 8 dígitos.");
        }

        try {
            JsonNode address = restClient.get()
                .uri("/{cep}/json/", normalizedCep)
                .retrieve()
                .body(JsonNode.class);

            if (address == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "A ViaCEP retornou uma resposta vazia."
                );
            }
            if (address.path("erro").asText().equalsIgnoreCase("true")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CEP não encontrado na ViaCEP.");
            }

            return new CepAddressResponse(
                text(address, "cep", normalizedCep),
                text(address, "logradouro", ""),
                text(address, "localidade", ""),
                text(address, "uf", "")
            );
        } catch (RestClientResponseException exception) {
            int upstreamStatus = exception.getStatusCode().value();
            if (upstreamStatus == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CEP não encontrado na ViaCEP.");
            }
            if (upstreamStatus == 429) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "A ViaCEP limitou as consultas (HTTP 429). Tente novamente mais tarde."
                );
            }
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "A ViaCEP respondeu com erro HTTP " + upstreamStatus + "."
            );
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Não foi possível conectar à ViaCEP."
            );
        }
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() && !value.asText().isBlank()
            ? value.asText()
            : fallback;
    }
}
