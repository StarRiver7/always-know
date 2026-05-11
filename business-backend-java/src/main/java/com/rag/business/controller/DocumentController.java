package com.rag.business.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rag.business.common.Result;
import com.rag.business.entity.Document;
import com.rag.business.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public Result<Page<Document>> listDocuments(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = (Long) request.getAttribute("userId");
        Page<Document> page = documentService.listDocuments(userId, pageNum, pageSize);
        return Result.success(page);
    }

    @PostMapping("/upload")
    public Result<Document> uploadDocument(
            HttpServletRequest request,
            @RequestParam("title") String title,
            @RequestParam("file") MultipartFile file) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            Document document = documentService.uploadDocument(userId, title, file);
            return Result.success(document);
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(
            HttpServletRequest request,
            @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        documentService.deleteDocument(id, userId);
        return Result.success();
    }

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(
            HttpServletRequest request,
            @RequestBody ChatRequest chatRequest) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = documentService.chat(
                userId,
                chatRequest.getQuery(),
                chatRequest.getDocumentIds()
        );
        return Result.success(result);
    }

    @Data
    public static class ChatRequest {
        private String query;
        private List<Long> documentIds;
    }
}
