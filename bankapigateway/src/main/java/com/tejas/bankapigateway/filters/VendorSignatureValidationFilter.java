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
		
		System.out.println("Gateway VendorSignatureValidationFilter - X-External: " + external);

		if (!"1".equals(external)) {
			System.out.println("Gateway VendorSignatureValidationFilter - Not external request, skipping validation");
			return chain.filter(exchange);
		}

		String cachedBody = exchange.getAttribute(CachedBodyGlobalFilter.CACHED_BODY_STRING_ATTR);
		System.out.println("Gateway VendorSignatureValidationFilter - Cached body: " + (cachedBody != null ? cachedBody.substring(0, Math.min(100, cachedBody.length())) + "..." : "null"));
		
		if (cachedBody == null) {
			System.out.println("Gateway VendorSignatureValidationFilter - Cached body is null, returning 400");
			exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
			return exchange.getResponse().setComplete();
		}

		String vendorId = request.getHeaders().getFirst("X-Vendor-Id");
		String vendorSecret = request.getHeaders().getFirst("X-Vendor-Secret");
		String timestamp = request.getHeaders().getFirst("X-Timestamp");
		String signature = request.getHeaders().getFirst("X-Signature");
		
		System.out.println("Gateway VendorSignatureValidationFilter - vendorId: " + vendorId + ", timestamp: " + timestamp + ", signature: " + (signature != null ? signature.substring(0, Math.min(20, signature.length())) + "..." : "null"));

		if (vendorId == null || timestamp == null || signature == null) {
			System.out.println("Gateway VendorSignatureValidationFilter - Missing required headers, returning 400");
			exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
			return exchange.getResponse().setComplete();
		}

		String configVendorSecret = externalConfig.getVendorKey().get(vendorId);
		if (configVendorSecret == null || !vendorSecret.equals(configVendorSecret)) {
			System.out.println("Gateway VendorSignatureValidationFilter - Vendor secret mismatch, returning 401");
			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}

		System.out.println("Gateway VendorSignatureValidationFilter - Calling verifySignature");
		boolean isValid = vendorValidation.verifySignature(vendorId, vendorSecret, timestamp, cachedBody, signature);
		System.out.println("Gateway VendorSignatureValidationFilter - Signature validation result: " + isValid);
		
		if (!isValid) {
			System.out.println("Gateway VendorSignatureValidationFilter - Signature invalid, returning 401");
			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}

		System.out.println("Gateway VendorSignatureValidationFilter - Signature valid, proceeding");
		return chain.filter(exchange);
	}

    @Override
    public int getOrder() {
        return -150;
    }
}

