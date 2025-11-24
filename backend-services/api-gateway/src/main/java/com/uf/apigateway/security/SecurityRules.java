package com.uf.apigateway.security;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.InitializingBean;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SecurityRules implements InitializingBean {

    private final Map<String, ServiceRule> rulesByService = new HashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();

    public void afterPropertiesSet() {
        load();
    }

    public void load() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:security/*.yml");
            Yaml yaml = new Yaml();
            for(Resource r : resources) {
                try (InputStream is = r.getInputStream()) {
                    Object data = yaml.load(is);
                    if(!(data instanceof Map)) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String,Object>) data;
                    ServiceRule sr = new ServiceRule();
                    String filename = r.getFilename();
                    if(filename != null && filename.contains(".")) {
                        sr.setName(filename.substring(0, filename.lastIndexOf('.')));
                    } else {
                        sr.setName(filename);
                    }
                    Object paths = map.get("paths");
                    if(paths instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> p = (List<Object>) paths;
                        List<PathRule> pathRules = new ArrayList<>();
                        for(Object entry : p) {
                            if(entry instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> em = (Map<String,Object>) entry;
                                PathRule pr = new PathRule();
                                Object pattern = em.get("pattern");
                                if(pattern != null) pr.setPattern(String.valueOf(pattern));
                                Object rolesObj = em.get("roles");
                                if(rolesObj instanceof List) {
                                    @SuppressWarnings("unchecked")
                                    List<String> roles = (List<String>) rolesObj;
                                    pr.setRoles(roles);
                                }
                                Object authObj = em.get("authenticated");
                                if(authObj != null) {
                                    pr.setAuthenticated(Boolean.valueOf(String.valueOf(authObj)));
                                }
                                Object methodsObj = em.get("methods");
                                if(methodsObj instanceof List) {
                                    @SuppressWarnings("unchecked")
                                    List<String> methods = (List<String>) methodsObj;
                                    pr.setMethods(methods);
                                }
                                pathRules.add(pr);
                            }
                        }
                        sr.setPaths(pathRules);
                    }
                    rulesByService.put(sr.getName(), sr);
                }
            }
        } catch (Exception ex) {
            // non-fatal: log to stdout for now
            System.err.println("Failed to load security rules: " + ex.getMessage());
        }
    }

    public Optional<MatchedRule> findMatchingRule(String path, String method) {
        for(java.util.Map.Entry<String, ServiceRule> e : rulesByService.entrySet()) {
            String service = e.getKey();
            ServiceRule sr = e.getValue();
            List<PathRule> prs = sr.getPaths();
            if(prs == null || prs.isEmpty()) {
                // service-wide default rule (no pattern) => match
                return Optional.of(new MatchedRule(service, null));
            }
            for(PathRule pr : prs) {
                String pattern = pr.getPattern();
                if(pattern == null) continue;
                if(!matcher.match(pattern, path)) continue;
                // method check
                if(pr.getMethods() != null && !pr.getMethods().isEmpty()) {
                    boolean allowed = false;
                    for(String m : pr.getMethods()) {
                        if(m.equalsIgnoreCase(method)) { allowed = true; break; }
                    }
                    if(!allowed) continue;
                }
                return Optional.of(new MatchedRule(service, pr));
            }
        }
        return Optional.empty();
    }

    public Map<String, ServiceRule> getAll() {
        return rulesByService;
    }
}
