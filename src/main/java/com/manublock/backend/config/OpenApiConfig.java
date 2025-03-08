package com.manublock.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI manuBlockOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ManuBlock API")
                        .description("Supply Chain Management API with Blockchain Integration")
                        .version("1.0.0"));
    }
}