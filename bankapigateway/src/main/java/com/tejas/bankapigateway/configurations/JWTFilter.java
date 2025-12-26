package com.tejas.bankapigateway.configurations;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
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

import com.tejas.bankapigateway.services.VendorValidation;
import com.tejas.bankapigateway.services.JWTService;
import com.tejas.bankingcommon.enums.UserType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class JWTFilter implements WebFilter {

    private final JWTService jwtService;
    private final GatewaySecretsConfig secretsConfig;
    private final VendorValidation serviceValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars")) {
            return chain.filter(exchange);
        }

        String trimmedPath = path.startsWith("/") ? path.substring(1) : path;
        String serviceName = trimmedPath.split("/")[0];
        String secret = secretsConfig.getSecrets().get(serviceName);

        String external = request.getHeaders().getFirst("X-External");

        if ("1".equals(external)) {

            Flux<DataBuffer> cachedBody =
                    exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);

            if (cachedBody == null) {
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                return exchange.getResponse().setComplete();
            }

            return DataBufferUtils.join(cachedBody)
                    .flatMap(buffer -> {

                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        DataBufferUtils.release(buffer);

                        String body = new String(bytes, StandardCharsets.UTF_8);

                        String vendorId = request.getHeaders().getFirst("X-Vendor-Id");
                        String vendorSecret = request.getHeaders().getFirst("X-Vendor-Secret");
                        String timestamp = request.getHeaders().getFirst("X-Timestamp");
                        String signature = request.getHeaders().getFirst("X-Signature");

                        if (vendorId == null || timestamp == null || signature == null) {
                            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                            return exchange.getResponse().setComplete();
                        }

                        boolean validated = serviceValidator.verifySignature(
                                vendorId, vendorSecret, timestamp, body, signature
                        );

                        if (!validated) {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }

                        String userId = "INTERNAL_PAYMENT_SERVICE";
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

                        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
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
}
