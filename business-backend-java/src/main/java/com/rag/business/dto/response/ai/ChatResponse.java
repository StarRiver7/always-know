package com.rag.business.dto.response.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI聊天响应DTO
 *
 * @author RAG Business Team
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    /**
     * AI生成的回答
     */
    private String answer;
    /**
     * 来源文档列表
     */
    private List<SourceChunk> sources;
}
