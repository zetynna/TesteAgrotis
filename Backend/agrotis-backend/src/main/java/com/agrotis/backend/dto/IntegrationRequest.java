package com.agrotis.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IntegrationRequest(
    @NotNull @Min(1) Integer schemaVersion,
    @NotBlank @Size(max = 255) String source,
    @NotBlank @Size(max = 255) String externalReference,
    @NotNull EntityType entityType,
    @NotNull JsonNode payload
) {
    public enum EntityType {
        PRODUCER,
        PROPERTY,
        LABORATORY
    }
}
