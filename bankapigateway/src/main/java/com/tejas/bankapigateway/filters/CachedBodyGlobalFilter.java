package com.tejas.bankapigateway.filters;

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
public class CachedBodyGlobalFilter implements WebFilter, Ordered {

    public static final String CACHED_BODY_STRING_ATTR = "cachedRequestBodyString";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (!path.startsWith("/bankpaymentservice") 
                || path.startsWith("/bankpaymentservice/v3/api-docs")
                || path.startsWith("/bankpaymentservice/swagger-ui")
                || path.startsWith("/bankpaymentservice/webjars")) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();

        return DataBufferUtils.join(request.getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    String bodyString = new String(bytes, StandardCharsets.UTF_8);
                    exchange.getAttributes().put(CACHED_BODY_STRING_ATTR, bodyString);

                    ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(request) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            if (bodyString.isEmpty()) {
                                return Flux.empty();
                            }
                            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bodyString.getBytes(StandardCharsets.UTF_8));
                            return Flux.just(buffer);
                        }
                    };

                    return chain.filter(exchange.mutate().request(decorator).build());
                });
    }

    @Override
    public int getOrder() {
        return -200;
    }
}

