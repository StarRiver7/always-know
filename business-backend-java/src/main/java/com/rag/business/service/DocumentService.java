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
        String originalFilename = file.getOriginalFilename();
        String fileExt = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + fileExt;
        
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
        this.save(document);

        DocumentPermission permission = new DocumentPermission();
        permission.setDocumentId(document.getId());
        permission.setUserId(userId);
        permission.setPermissionType(1);
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
        if (document == null || !document.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此文档");
        }
        
        this.removeById(documentId);
        aiBackendClient.deleteDocument(documentId);
        
        File file = new File(document.getFilePath());
        if (file.exists()) {
            file.delete();
        }
    }
}
