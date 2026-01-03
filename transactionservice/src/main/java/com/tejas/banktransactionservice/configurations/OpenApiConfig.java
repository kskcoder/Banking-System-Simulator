package com.tejas.banktransactionservice.configurations;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(
        servers = {
                @Server(
                		url = "/bankpaymentservice", 
                		description = "Transaction Service")
        }
)
public class OpenApiConfig {

}
