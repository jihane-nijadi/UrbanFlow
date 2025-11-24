package com.uf.apigateway.security;

import java.util.List;

public class ServiceRule {
    private String name;
    private List<PathRule> paths;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PathRule> getPaths() {
        return paths;
    }

    public void setPaths(List<PathRule> paths) {
        this.paths = paths;
    }
}
