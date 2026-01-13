package com.tejas.bankaccountservice.configurations;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Account Service API",
                description = "API documentation for Account Service",
                version = "1.0.0"
        ),
        servers = {
                @Server(
                		url = "/bankaccountservice", 
                		description = "Account Service")
        }
)
public class OpenApiConfig {

}
