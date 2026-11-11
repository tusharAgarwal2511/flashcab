package com.flashCab.driverService.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI driverServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("FlashCab Driver Service API")
                .description("API for registering and managing FlashCab drivers.")
                .version("1.0.0"));
    }
}
