package com.flashCab.riderService.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI riderServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("FlashCab Rider Service API")
                .description("API for registering and managing FlashCab riders.")
                .version("1.0.0"));
    }
}
