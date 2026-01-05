package com.tejas.bankpaymentservice.configurations;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Payment Service API",
                description = "API documentation for Payment Service",
                version = "1.0.0"
        ),
        servers = {
                @Server(
                		url = "/bankpaymentservice", 
                		description = "Payment Service")
        }
)
public class OpenApiConfig {

}
