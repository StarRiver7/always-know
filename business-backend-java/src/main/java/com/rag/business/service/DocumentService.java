package com.rag.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rag.business.entity.Document;
import com.rag.business.entity.DocumentPermission;
import com.rag.business.mapper.DocumentMapper;
import com.rag.business.mapper.DocumentPermissionMapper;
import com.rag.business.client.AiBackendClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService extends ServiceImpl<DocumentMapper, Document> {

    private final DocumentPermissionMapper documentPermissionMapper;
    private final AiBackendClient aiBackendClient;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    private Path getUploadDir() throws IOException {
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
            log.info("Created upload directory: {}", uploadDir);
        }
        return uploadDir;
    }

    public Page<Document> listDocuments(Long userId, Integer pageNum, Integer pageSize) {
        List<Long> accessibleDocIds = baseMapper.selectAccessibleDocumentIds(userId);

        Page<Document> page = new Page<>(pageNum, pageSize);
        if (accessibleDocIds.isEmpty()) {
            return page;
        }

        return this.page(page, new LambdaQueryWrapper<Document>()
                .in(Document::getId, accessibleDocIds)
                .orderByDesc(Document::getCreateTime));
    }

    public Document uploadDocument(Long userId, String title, MultipartFile file) throws IOException {
        // 1. 校验文件
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }
        
        // 2. 校验文件类型
        String fileExt = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        List<String> allowedExts = List.of(".txt", ".pdf", ".doc", ".docx", ".md", ".csv", ".xlsx");
        if (!allowedExts.contains(fileExt)) {
            throw new RuntimeException("不支持的文件类型: " + fileExt + "，仅支持: " + String.join(", ", allowedExts));
        }
        
        // 3. 校验文件大小（100MB）
        long maxSize = 100 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new RuntimeException("文件大小不能超过 100MB");
        }
        
        // 4. 生成新文件名
        String newFileName = UUID.randomUUID() + fileExt;
        
        Path uploadDir = getUploadDir();
        Path destPath = uploadDir.resolve(newFileName);
        
        log.info("Uploading file to: {}", destPath);
        
        File destFile = destPath.toFile();
        file.transferTo(destFile);
        
        String absolutePath = destPath.toString();

        Document document = new Document();
        document.setTitle(title);
        document.setFileName(originalFilename);
        document.setFilePath(absolutePath);
        document.setFileSize(file.getSize());
        document.setFileType(fileExt);
        document.setUserId(userId);
        document.setStatus(0);
        document.setCreateTime(LocalDateTime.now());
        this.save(document);

        DocumentPermission permission = new DocumentPermission();
        permission.setDocumentId(document.getId());
        permission.setUserId(userId);
        permission.setPermissionType(1);
        permission.setCreateTime(java.time.LocalDateTime.now());
        documentPermissionMapper.insert(permission);

        this.processDocumentAsync(document.getId(), absolutePath, originalFilename);

        return document;
    }

    @Async("ragTaskExecutor")
    public void processDocumentAsync(Long documentId, String filePath, String fileName) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("File not found: {}", filePath);
                Document document = this.getById(documentId);
                document.setStatus(-1);
                this.updateById(document);
                return;
            }
            
            Map<String, Object> result = aiBackendClient.processDocument(documentId, file, null);
            
            Document document = this.getById(documentId);
            document.setStatus(1);
            document.setChunksProcessed((Integer) result.get("chunks_processed"));
            this.updateById(document);

            log.info("Document {} processed successfully", documentId);
        } catch (Exception e) {
            log.error("Failed to process document {}", documentId, e);
            Document document = this.getById(documentId);
            document.setStatus(-1);
            this.updateById(document);
        }
    }

    public Map<String, Object> chat(Long userId, String query, List<Long> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            docIds = baseMapper.selectAccessibleDocumentIds(userId);
        }

        Map<String, Object> response = aiBackendClient.chat(query, 5, docIds);

        com.rag.business.entity.ChatHistory history = new com.rag.business.entity.ChatHistory();
        history.setUserId(userId);
        history.setQuery(query);
        history.setAnswer((String) response.get("answer"));
        Object sources = response.get("sources");
        history.setSourceDocuments(sources != null ? sources.toString() : null);
        
        Map<String, Object> result = new HashMap<>();
        result.put("answer", response.get("answer"));
        result.put("sources", response.get("sources"));
        return result;
    }

    public void deleteDocument(Long documentId, Long userId) {
        Document document = this.getById(documentId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }
        if (!document.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此文档");
        }
        
        // 删除数据库记录
        this.removeById(documentId);
        
        // 删除 AI 后端索引
        try {
            aiBackendClient.deleteDocument(documentId);
        } catch (Exception e) {
            log.error("删除 AI 后端文档索引失败: {}", documentId, e);
            // 不抛出异常，继续删除本地文件
        }
        
        // 删除本地文件
        File file = new File(document.getFilePath());
        if (file.exists() && !file.delete()) {
            log.warn("删除本地文件失败: {}", document.getFilePath());
        }
    }
}
