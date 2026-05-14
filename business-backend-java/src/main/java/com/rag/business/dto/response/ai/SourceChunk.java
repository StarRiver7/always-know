package com.rag.business.dto.response.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 来源文档块DTO
 *
 * @author RAG Business Team
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceChunk {
    /**
     * 文档ID
     */
    private Integer documentId;
    /**
     * 块ID
     */
    private Integer chunkId;
    /**
     * 文本内容
     */
    private String content;
    /**
     * 元数据
     */
    private Map<String, Object> metadata;
    /**
     * 相似度分数
     */
    private Double score;
}
