package com.rag.business.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rag.business.annotation.CurrentUserId;
import com.rag.business.annotation.RequireRole;
import com.rag.business.common.RoleConstants;
import com.rag.business.dto.response.Result;
import com.rag.business.dto.response.ResultCode;
import com.rag.business.dto.request.ChatRequest;
import com.rag.business.entity.Document;
import com.rag.business.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    //  分页查询用户的文档列表
    @GetMapping
    public Result<Page<Document>> listDocuments(
            @CurrentUserId Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Document> page = documentService.listDocuments(userId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 上传文档（仅管理员）
     */
    @RequireRole({RoleConstants.ADMIN, RoleConstants.USER})
    @PostMapping("/upload")
    public Result<Document> uploadDocument(
            @CurrentUserId Long userId,
            @RequestParam("title") String title,
            @RequestParam("file") MultipartFile file) {
        try {
            Document document = documentService.uploadDocument(userId, title, file);
            return Result.success(document);
        } catch (Exception e) {
            return Result.error(ResultCode.UPLOAD_FAILED, "上传失败: " + e.getMessage());
        }
    }

    //  删除文档
    @RequireRole({RoleConstants.ADMIN, RoleConstants.USER})
    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        documentService.deleteDocument(id, userId);
        return Result.success();

    }

    //  ai聊天
    @RequireRole({RoleConstants.ADMIN, RoleConstants.USER})
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(
            @CurrentUserId Long userId,
            @RequestBody ChatRequest chatRequest) {
        Map<String, Object> result = documentService.chat(
                userId,
                chatRequest.getQuery(),
                chatRequest.getDocumentIds()
        );
        return Result.success(result);
    }

}
