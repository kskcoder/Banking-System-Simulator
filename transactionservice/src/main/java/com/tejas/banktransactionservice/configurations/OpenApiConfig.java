package com.tejas.banktransactionservice.configurations;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Transaction Service API",
                description = "API documentation for Transaction Service",
                version = "1.0.0"
        ),
        servers = {
                @Server(
                		url = "/banktransactionservice", 
                		description = "Transaction Service")
        }
)
public class OpenApiConfig {

}
