package com.rag.business.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.List;

/**
 * AI聊天请求参数
 *
 * @author RAG Business Team
 * @since 2026-05-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 用户问题
     */
    @NotBlank(message = "用户问题不能为空")
    private String query;
    /**
     * 文档ID列表
     */
    @NotBlank(message = "文档ID列表不能为空")
    @Size(min = 1, max = 100, message = "文档ID列表长度必须在1-100之间")
    private List<Long> documentIds;
}
