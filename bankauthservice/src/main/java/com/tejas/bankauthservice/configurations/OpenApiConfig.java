package com.tejas.bankauthservice.configurations;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Auth Service API",
                description = "API documentation for Authorization Service",
                version = "1.0.0"
        ),
        servers = {
                @Server(
                		url = "/bankauthservice", 
                		description = "Authorization Service")
        }
)
public class OpenApiConfig {

}
