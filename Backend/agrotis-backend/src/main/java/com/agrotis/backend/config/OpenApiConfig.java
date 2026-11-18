package com.agrotis.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI agrotisOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Agrotis Agroinformática API")
                .description("Backend para gestão de produtores, propriedades, laboratórios e integração de dados")
                .version("1.0.0"));
    }
}
