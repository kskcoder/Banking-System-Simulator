package com.tejas.bankapigateway.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
            .cors(Customizer.withDefaults())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable) 
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(exchange -> exchange
                .pathMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/bankauthservice/v3/api-docs/**",
                        "/bankaccountservice/v3/api-docs/**",
                        "/bankcardservice/v3/api-docs/**",
                        "/banktransactionservice/v3/api-docs/**",
                        "/bankpaymentservice/v3/api-docs/**",
                        "/webjars/**",
                        "/swagger-config",
                        "/swagger-resources/**",
                        "/favicon.ico"
                    ).permitAll()
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .pathMatchers("/bankauthservice/auth/**").permitAll()
                .pathMatchers("/bankpaymentservice/payments/demo/**").permitAll()
                .pathMatchers("/bankpaymentservice/payments/**").hasAnyRole("INTERNAL_SERVICE", "USER", "ADMIN")
                .anyExchange().authenticated()
            )
            .addFilterBefore(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }
}
