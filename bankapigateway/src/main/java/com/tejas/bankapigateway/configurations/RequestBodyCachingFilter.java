package com.tejas.bankapigateway.configurations;

import java.nio.charset.StandardCharsets;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class RequestBodyCachingFilter implements WebFilter, Ordered{

	@Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (exchange.getRequest().getMethod().name().equals("GET")) {
			return chain.filter(exchange);
		}
		
		String path = exchange.getRequest().getURI().getPath();
		if (path.startsWith("/swagger-ui") || 
		    path.startsWith("/v3/api-docs") || 
		    path.startsWith("/webjars")) {
			return chain.filter(exchange);
		}
		
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .flatMap(dataBuffer -> {

                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    String body = new String(bytes, StandardCharsets.UTF_8);

                    exchange.getAttributes().put("cachedRequestBody", body);

                    Flux<DataBuffer> cachedFlux = Flux.defer(() -> {
                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                        return Mono.just(buffer);
                    });

                    ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return cachedFlux;
                        }
                    };

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

	
    @Override
    public int getOrder() {
        return -2; 
    }
}
