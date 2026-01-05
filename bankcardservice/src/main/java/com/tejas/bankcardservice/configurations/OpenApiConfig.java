package com.tejas.bankcardservice.configurations;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Card Service API",
                description = "API documentation for Card Service",
                version = "1.0.0"
        ),
        servers = {
                @Server(
                		url = "/bankcardservice", 
                		description = "Card Service")
        }
)
public class OpenApiConfig {

}
