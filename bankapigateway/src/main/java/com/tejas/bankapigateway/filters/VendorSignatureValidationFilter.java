package com.tejas.bankapigateway.filters;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.tejas.bankapigateway.configurations.ExternalVendorSecretsConfig;
import com.tejas.bankapigateway.services.VendorValidation;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class VendorSignatureValidationFilter implements WebFilter, Ordered {

    private final ExternalVendorSecretsConfig externalConfig;
    private final VendorValidation vendorValidation;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String external = request.getHeaders().getFirst("X-External");

        if (!"1".equals(external)) {
            return chain.filter(exchange);
        }

        String cachedBody = exchange.getAttribute(CachedBodyGlobalFilter.CACHED_BODY_STRING_ATTR);
        if (cachedBody == null) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }

        String vendorId = request.getHeaders().getFirst("X-Vendor-Id");
        String vendorSecret = request.getHeaders().getFirst("X-Vendor-Secret");
        String timestamp = request.getHeaders().getFirst("X-Timestamp");
        String signature = request.getHeaders().getFirst("X-Signature");

        if (vendorId == null || timestamp == null || signature == null) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }

        String configVendorSecret = externalConfig.getVendorKey().get(vendorId);
        if (configVendorSecret == null || !vendorSecret.equals(configVendorSecret)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        boolean isValid = vendorValidation.verifySignature(vendorId, vendorSecret, timestamp, cachedBody, signature);
        if (!isValid) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -150;
    }
}

