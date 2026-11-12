package com.flashcab.gatewayService.gatewayService.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticatedIdentityHeaderFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-Authenticated-User-Id";
    private static final String USER_ROLE_HEADER = "X-Authenticated-User-Role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                })
                .build();
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .map(authentication -> {
                    ServerHttpRequest authenticatedRequest = sanitizedRequest.mutate()
                            .headers(headers -> {
                                headers.set(USER_ID_HEADER, authentication.getToken().getSubject());
                                headers.set(USER_ROLE_HEADER,
                                        authentication.getToken().getClaimAsString("role"));
                            })
                            .build();
                    return sanitizedExchange.mutate().request(authenticatedRequest).build();
                })
                .defaultIfEmpty(sanitizedExchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
