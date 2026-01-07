package com.tejas.bankapigateway.configurations;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.core.Ordered;

import com.tejas.bankapigateway.services.JWTService;
import com.tejas.bankapigateway.configurations.GatewaySecretsConfig;
import com.tejas.bankingcommon.enums.InternalServiceType;
import com.tejas.bankingcommon.enums.UserType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@AllArgsConstructor
public class JWTFilter implements WebFilter, Ordered {

    private final JWTService jwtService;
    private final GatewaySecretsConfig secretsConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        ServerHttpRequest request = exchange.getRequest();

        if (path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars")
                || path.startsWith("/bankpaymentservice/payments/demo")) {
            return chain.filter(exchange);
        }

        String trimmedPath = path.startsWith("/") ? path.substring(1) : path;
        String serviceName = trimmedPath.split("/")[0];
        String secret = secretsConfig.getSecrets().get(serviceName);

        String external = request.getHeaders().getFirst("X-External");
        
        System.out.println("Gateway JWTFilter - X-External: " + external);
        System.out.println("Gateway JWTFilter - Path: " + path);

        if ("1".equals(external)) {
            System.out.println("Gateway JWTFilter - External request detected, setting up authentication");
            String userId = InternalServiceType.PAYMENT.toString();
            String role = UserType.INTERNAL_SERVICE.toString();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Internal-Auth", secret)
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .build();

            System.out.println("Gateway JWTFilter - Authentication set, proceeding with modified request");
            System.out.println("Gateway JWTFilter - Auth principal: " + auth.getPrincipal() + ", authorities: " + auth.getAuthorities());
            
            ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();
            
            return chain.filter(modifiedExchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                    .doOnSuccess(v -> System.out.println("Gateway JWTFilter - Filter chain completed successfully"))
                    .doOnError(e -> {
                        System.out.println("Gateway JWTFilter - Filter chain error: " + e.getMessage());
                        e.printStackTrace();
                    });
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.extractAllClaims(token);
            String userId = jwtService.extractUserId(token);
            String role = claims.get("role", String.class);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Internal-Auth", secret)
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build())
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        } catch (JwtException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
    
    @Override
    public int getOrder() {
        return 0;
    }
}
