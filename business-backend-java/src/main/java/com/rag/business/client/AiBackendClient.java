package com.rag.business.client;

import com.rag.business.dto.request.ai.ChatQueryRequest;
import com.rag.business.dto.response.ai.ChatResponse;
import com.rag.business.dto.response.ai.ProcessDocumentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Map;

/**
 * AI后端客户端 - 使用WebClient异步方式
 *
 * @author RAG Business Team
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiBackendClient {

    private final WebClient aiBackendWebClient;

    /**
     * 异步聊天查询
     *
     * @param query       查询内容
     * @param topK        返回Top K
     * @param documentIds 文档ID列表
     * @return ChatResponse
     */
    public Mono<ChatResponse> chatAsync(String query, Integer topK, java.util.List<Long> documentIds) {
        ChatQueryRequest request = ChatQueryRequest.builder()
                .query(query)
                .topK(topK)
                .documentIds(documentIds)
                .build();

        return aiBackendWebClient.post()
                .uri("/api/v1/chat/query")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .doOnError(WebClientResponseException.class, e -> {
                    log.error("Chat request failed: Status {}, Body {}",
                            e.getStatusCode(), e.getResponseBodyAsString());
                })
                .onErrorResume(e -> {
                    log.error("Chat request failed", e);
                    throw new RuntimeException("调用AI后端聊天接口失败: " + e.getMessage(), e);
                });
    }

    /**
     * 同步聊天查询（兼容现有代码）
     *
     * @param query       查询内容
     * @param topK        返回Top K
     * @param documentIds 文档ID列表
     * @return Map<String, Object>
     */
    public Map<String, Object> chat(String query, Integer topK, java.util.List<Long> documentIds) {
        return chatAsync(query, topK, documentIds)
                .map(response -> {
                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("answer", response.getAnswer());
                    result.put("sources", response.getSources());
                    return result;
                })
                .block();
    }

    /**
     * 异步处理文档
     *
     * @param documentId 文档ID
     * @param file       文件
     * @param metadata   元数据
     * @return ProcessDocumentResponse
     */
    public Mono<ProcessDocumentResponse> processDocumentAsync(Long documentId, File file, Map<String, Object> metadata) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));

        StringBuilder uriBuilder = new StringBuilder("/api/v1/documents/process?document_id=" + documentId);
        if (metadata != null && !metadata.isEmpty()) {
            // 简单处理元数据，如果需要复杂元数据，可扩展为JSON
        }

        return aiBackendWebClient.post()
                .uri(uriBuilder.toString())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ProcessDocumentResponse.class)
                .doOnError(WebClientResponseException.class, e -> {
                    log.error("Process document request failed: Status {}, Body {}",
                            e.getStatusCode(), e.getResponseBodyAsString());
                })
                .onErrorResume(e -> {
                    log.error("Process document request failed", e);
                    throw new RuntimeException("调用AI后端文档处理接口失败: " + e.getMessage(), e);
                });
    }

    /**
     * 同步处理文档（兼容现有代码）
     *
     * @param documentId 文档ID
     * @param file       文件
     * @param metadata   元数据
     * @return Map<String, Object>
     */
    public Map<String, Object> processDocument(Long documentId, File file, Map<String, Object> metadata) {
        log.info("发送文档处理请求。等待响应");
        return processDocumentAsync(documentId, file, metadata)
                .map(response -> {
                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("document_id", response.getDocumentId());
                    result.put("chunks_processed", response.getChunksProcessed());
                    result.put("status", response.getStatus());
                    return result;
                })
                .block();
    }

    /**
     * 异步删除文档
     *
     * @param documentId 文档ID
     * @return Mono<Void>
     */
    public Mono<Void> deleteDocumentAsync(Long documentId) {
        return aiBackendWebClient.delete()
                .uri("/api/v1/documents/{documentId}", documentId)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(WebClientResponseException.class, e -> {
                    log.error("Delete document request failed: Status {}, Body {}",
                            e.getStatusCode(), e.getResponseBodyAsString());
                })
                .onErrorResume(e -> {
                    log.error("Delete document request failed", e);
                    throw new RuntimeException("调用AI后端删除接口失败: " + e.getMessage(), e);
                });
    }

    /**
     * 同步删除文档（兼容现有代码）
     *
     * @param documentId 文档ID
     */
    public void deleteDocument(Long documentId) {
        deleteDocumentAsync(documentId).block();
    }
}
