package com.tejas.bankapigateway.components;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class GatewayCacheFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        if (!path.startsWith("/bankpaymentservice")) {
            return chain.filter(exchange);
        }

        return ServerWebExchangeUtils.cacheRequestBody(exchange, serverHttpRequest -> {
            return chain.filter(
                exchange.mutate().request(serverHttpRequest).build()
            );
        });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}