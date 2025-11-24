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
import com.uf.apigateway.security.SecurityRules;
import com.uf.apigateway.security.MatchedRule;
import com.uf.apigateway.security.PathRule;
import java.util.Optional;

@Component
public class AuthFilter implements GatewayFilter {

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private SecurityRules securityRules;

    @Value("${authentication.enabled:true}")
    private boolean authEnabled;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if(!authEnabled) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() != null ? request.getMethod().name() : "GET";

        Optional<MatchedRule> matched = securityRules.findMatchingRule(path, method);

        // If a rule exists and explicitly marks authenticated=false => allow anonymously
        if(matched.isPresent() && matched.get().getPathRule() != null && Boolean.FALSE.equals(matched.get().getPathRule().getAuthenticated())) {
            return chain.filter(exchange);
        }

        // Otherwise require auth (default: require authentication if no rule matches)
        if(isCredsMissing(request)) {
            return onError(exchange,"Credentials missing",HttpStatus.UNAUTHORIZED);
        }
        String token = extractBearerToken(request);
        if(token == null) {
            return onError(exchange,"Authorization header malformed",HttpStatus.UNAUTHORIZED);
        }
        try {
            if(jwtUtil.isInvalid(token)) {
                return onError(exchange,"Token invalid or expired",HttpStatus.UNAUTHORIZED);
            }
            populateRequestWithHeaders(exchange, token);

            // Enforce single-user-type policy: only PASSENGER users allowed when role is required
            if(matched.isPresent()) {
                PathRule pr = matched.get().getPathRule();
                if(pr != null && pr.getRoles() != null && !pr.getRoles().isEmpty()) {
                    Object claim = jwtUtil.getAllClaimsSafe(token).get("role");
                    String role = claim == null ? null : String.valueOf(claim);
                    if(role == null || !"PASSENGER".equalsIgnoreCase(role)) {
                        return onError(exchange, "Forbidden: passengers only", HttpStatus.FORBIDDEN);
                    }
                }
            }
        } catch (Exception ex) {
            return onError(exchange, "Token invalid or malformed", HttpStatus.UNAUTHORIZED);
        }
        return chain.filter(exchange);
    }

    // security is now driven by rule files; keep helper removed

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
        Claims claims = jwtUtil.getAllClaimsSafe(token);
        exchange.getRequest()
                .mutate()
                .header("id", String.valueOf(claims.get("id")))
                .header("role", String.valueOf(claims.get("role")))
                .build();
    }
}
