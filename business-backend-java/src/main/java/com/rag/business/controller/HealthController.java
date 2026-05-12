package com.rag.business.controller;

import com.rag.business.dto.response.Result;
import com.rag.business.dto.response.ResultCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "healthy");
        result.put("service", "business-backend");
        return Result.success(result);
    }
}
