package com.tejas.bankapigateway.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class ApiGatewaySecurityConfig {
	@Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, JWTFilter jwtFilter) {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchange -> exchange
                .pathMatchers("/auth-service/auth/**").permitAll() 
                .anyExchange().authenticated()
            )
            .addFilterBefore(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }
}
