package com.uf.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public Map<String, Object> test() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "OK");
        resp.put("time", Instant.now().toString());
        return resp;
    }
}
