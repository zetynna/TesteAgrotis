package com.agrotis.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AgrotisBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void cepLookupRejectsInvalidCep() throws Exception {
        mockMvc.perform(get("/api/cep/123"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Informe um CEP válido com 8 dígitos."));
    }

    @Test
    void analyticsRequiresNonNegativeProducerMinimumAndValidDateRanges() throws Exception {
        mockMvc.perform(get("/api/analytics/laboratories"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/analytics/laboratories").param("qtdMinimaProdutores", "-1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/analytics/laboratories")
                .param("qtdMinimaProdutores", "0")
                .param("dataInicialInicio", "2026-06-02T00:00:00")
                .param("dataInicialFim", "2026-06-01T00:00:00"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void analyticsReturnsUppercaseNamesWithZeroDefaultRates() throws Exception {
        mockMvc.perform(get("/api/analytics/laboratories").param("qtdMinimaProdutores", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nome").value("LABORATÓRIO MODELO 1"))
            .andExpect(jsonPath("$[0].totalProdutores").value(2))
            .andExpect(jsonPath("$[0].valorFinanceiroConsolidado").value(0));
    }

    @Test
    void structuralLaboratoryAndPropertyCrudAcceptsValidatedJsonBodies() throws Exception {
        String laboratory = """
            {"nome":"Laboratório CRUD","inscricaoFiscal":"12.345.678/0001-95",
             "endereco":{"logradouro":"Rua A","cidade":"Curitiba","estado":"PR","cep":"80000000"}}
            """;
        String property = """
            {"nome":"Propriedade CRUD","inscricaoFiscal":"333.444.555-08",
             "endereco":{"logradouro":"Rua B","cidade":"Curitiba","estado":"PR","cep":"80000000"}}
            """;
        String laboratoryResponse = mockMvc.perform(post("/api/laboratories")
                .contentType("application/json").content(laboratory))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long laboratoryId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(laboratoryResponse).path("id").asLong();
        mockMvc.perform(put("/api/laboratories/{id}", laboratoryId)
                .contentType("application/json").content(laboratory.replace("Laboratório CRUD", "Laboratório Alterado")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Laboratório Alterado"));
        mockMvc.perform(delete("/api/laboratories/{id}", laboratoryId))
            .andExpect(status().isNoContent());

        String propertyResponse = mockMvc.perform(post("/api/properties")
                .contentType("application/json").content(property))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long propertyId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(propertyResponse).path("id").asLong();
        mockMvc.perform(put("/api/properties/{id}", propertyId)
                .contentType("application/json").content(property.replace("Propriedade CRUD", "Propriedade Alterada")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Propriedade Alterada"));

        String laboratories = mockMvc.perform(get("/api/laboratories"))
            .andReturn().getResponse().getContentAsString();
        long labIdForProducer = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(laboratories).get(0).path("id").asLong();
        String producer = """
            {"nome":"Produtor de vínculo","inscricaoFiscal":"333.444.555-08",
             "endereco":{"logradouro":"Rua C","cidade":"Curitiba","estado":"PR","cep":"80000000"},
             "propriedadeIds":[%d],"laboratorioId":%d,"observacoes":"Teste de vínculo"}
            """.formatted(propertyId, labIdForProducer);
        String producerResponse = mockMvc.perform(post("/api/producers")
                .contentType("application/json").content(producer))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.propriedades[0].id").value(propertyId))
            .andReturn().getResponse().getContentAsString();
        long producerId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(producerResponse).path("id").asLong();
        mockMvc.perform(put("/api/producers/{id}", producerId)
                .contentType("application/json").content(producer.replace("Produtor de vínculo", "Produtor atualizado")))
            .andExpect(status().isOk());

        String competingProducer = producer.replace("333.444.555-08", "444.555.666-19");
        mockMvc.perform(post("/api/producers").contentType("application/json").content(competingProducer))
            .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/properties/{id}", propertyId)).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/producers/{id}", producerId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/properties/{id}", propertyId)).andExpect(status().isNoContent());
    }
}
