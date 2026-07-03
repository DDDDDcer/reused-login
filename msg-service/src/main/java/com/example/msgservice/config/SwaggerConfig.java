package com.example.msgservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI msgServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Reusable Message Notification Service API")
                .description("Template, carrier, immediate/scheduled delivery and local message APIs")
                .version("1.0.0"));
    }
}
