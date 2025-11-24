package com.uf.apigateway.security;

public class MatchedRule {
    private final String serviceName;
    private final PathRule pathRule;

    public MatchedRule(String serviceName, PathRule pathRule) {
        this.serviceName = serviceName;
        this.pathRule = pathRule;
    }

    public String getServiceName() {
        return serviceName;
    }

    public PathRule getPathRule() {
        return pathRule;
    }
}
