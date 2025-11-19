package com.uf.apigateway.filters;

import com.uf.apigateway.JWT.JWTUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter implements GatewayFilter {

    @Autowired
    private JWTUtil jwtUtil;

    @Value("${authentication.enabled:true}")
    private boolean authEnabled;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if(!authEnabled) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();

        if(isSecured(request)) {
            if(isCredsMissing(request)) {
                return onError(exchange,"Credentials missing",HttpStatus.UNAUTHORIZED);
            }
            String token = extractBearerToken(request);
            if(token == null) {
                return onError(exchange,"Authorization header malformed",HttpStatus.UNAUTHORIZED);
            }
            if(jwtUtil.isInvalid(token)) {
                return onError(exchange,"Token invalid or expired",HttpStatus.UNAUTHORIZED);
            }
            populateRequestWithHeaders(exchange, token);
        }
        return chain.filter(exchange);
    }

    private boolean isSecured(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return !path.startsWith("/login");
    }

    private String extractBearerToken(ServerHttpRequest request) {
        if(!request.getHeaders().containsKey("Authorization")) {
            return null;
        }
        String authHeader = request.getHeaders().getFirst("Authorization");
        if(authHeader == null) return null;
        if(authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    private boolean isCredsMissing(ServerHttpRequest request) {
        return !request.getHeaders().containsKey("Authorization");
    }

    private void populateRequestWithHeaders(ServerWebExchange exchange, String token) {
        Claims claims = jwtUtil.getALlClaims(token);
        exchange.getRequest()
                .mutate()
                .header("id", String.valueOf(claims.get("id")))
                .header("role", String.valueOf(claims.get("role")))
                .build();
    }
}
