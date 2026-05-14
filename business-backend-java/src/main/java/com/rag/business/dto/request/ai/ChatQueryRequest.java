package com.rag.business.dto.request.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI聊天查询请求DTO
 *
 * @author RAG Business Team
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatQueryRequest {
    /**
     * 用户查询
     */
    private String query;
    /**
     * 返回Top K
     */
    private Integer topK;
    /**
     * 文档ID列表
     */
    private List<Long> documentIds;
}
