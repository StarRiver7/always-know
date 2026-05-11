package com.rag.business.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiBackendClient {

    @Value("${ai.backend.url}")
    private String aiBackendUrl;

    public Map<String, Object> processDocument(Long documentId, File file, Map<String, Object> metadata) {
        String url = aiBackendUrl + "/api/v1/documents/process?document_id=" + documentId;
        
        try {
            HttpResponse response = HttpRequest.post(url)
                    .form("file", file)
                    .timeout(600000)
                    .execute();
            
            String body = response.body();
            log.info("AI Backend process response: {}", body);
            return JSONUtil.toBean(body, Map.class);
        } catch (Exception e) {
            log.error("Failed to call AI backend", e);
            throw new RuntimeException("调用AI后端失败", e);
        }
    }

    public Map<String, Object> chat(String query, Integer topK, List<Long> documentIds) {
        String url = aiBackendUrl + "/api/v1/chat/query";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);
        requestBody.put("top_k", topK);
        requestBody.put("document_ids", documentIds);
        
        try {
            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(requestBody))
                    .timeout(60000)
                    .execute();
            
            String body = response.body();
            log.info("AI Backend chat response: {}", body);
            return JSONUtil.toBean(body, Map.class);
        } catch (Exception e) {
            log.error("Failed to call AI backend for chat", e);
            throw new RuntimeException("调用AI后端失败", e);
        }
    }

    public void deleteDocument(Long documentId) {
        String url = aiBackendUrl + "/api/v1/documents/" + documentId;
        
        try {
            HttpResponse response = HttpRequest.delete(url)
                    .timeout(30000)
                    .execute();
            log.info("AI Backend delete response: {}", response.body());
        } catch (Exception e) {
            log.error("Failed to call AI backend for delete", e);
        }
    }
}
