package com.tejas.bankapigateway.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.tejas.bankapigateway.services.JWTService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import reactor.core.publisher.Mono;

@Component
public class JWTFilter implements WebFilter{
	@Autowired
	JWTService jwtService;
	
	@Autowired
	ApplicationContext context;
	
	@Autowired
	private GatewaySecretsConfig secretsConfig;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		String token = null;
		String userId = null;
		
		String path = request.getURI().getPath();
		String trimmedPath = path.startsWith("/") ? path.substring(1) : path;
		String serviceName = trimmedPath.split("/")[0];
		
		String secret = secretsConfig.getSecrets().get(serviceName);
		
		 if (authHeader == null || !authHeader.startsWith("Bearer ")) {
	            return chain.filter(exchange);
	        }

	        token = authHeader.substring(7);
	        try {
	            Claims claims = jwtService.extractAllClaims(token);

	            userId = jwtService.extractUserId(token);
	            String role = claims.get("role", String.class);

	            ServerHttpRequest modifiedRequest = request.mutate()
	            		.header("X-Internal-Auth", secret)
	                    .header("X-User-Id", userId)
	                    .header("X-User-Role", role)
	                    .build();

	            return chain.filter(exchange.mutate().request(modifiedRequest).build());

	        } catch (JwtException e) {
	            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
	            return exchange.getResponse().setComplete();
	        }
	}
}

