package com.rag.business.dto.response.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档处理响应DTO
 *
 * @author RAG Business Team
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDocumentResponse {
    /**
     * 文档ID
     */
    private Long documentId;
    /**
     * 处理的块数
     */
    private Integer chunksProcessed;
    /**
     * 状态
     */
    private String status;
}
