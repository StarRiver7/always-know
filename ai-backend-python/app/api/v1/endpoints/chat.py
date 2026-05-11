"""
问答接口路由

提供基于知识库的智能问答功能，支持从已上传的文档中检索相关信息并生成回答。
"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional, List
from app.services.document_service import document_service
import logging

logger = logging.getLogger(__name__)

router = APIRouter(tags=["问答服务"])


class ChatRequest(BaseModel):
    """问答请求模型"""
    query: str                    # 用户提问内容
    top_k: Optional[int] = 5      # 返回相似文档数量
    document_ids: Optional[List[int]] = None  # 指定文档ID列表（可选）


class SourceChunk(BaseModel):
    """来源文档块模型"""
    document_id: int   # 文档ID
    chunk_id: int      # 块ID
    content: str       # 内容
    metadata: dict     # 元数据
    score: float       # 相似度分数


class ChatResponse(BaseModel):
    """问答响应模型"""
    answer: str              # AI生成的回答
    sources: List[SourceChunk]  # 来源文档列表


@router.post("/query", response_model=ChatResponse, summary="知识库问答")
async def chat_query(request: ChatRequest):
    """
    知识库问答接口
    
    根据用户提问从知识库中检索相关信息并生成回答。
    
    Args:
        request: 问答请求，包含提问内容、返回数量和可选的文档ID列表
        
    Returns:
        包含回答和来源文档的响应
        
    Raises:
        HTTPException: 处理过程中发生错误时返回500状态码
    """
    try:
        logger.info(f"收到问答请求: query={request.query[:50]}..., top_k={request.top_k}, document_ids={request.document_ids}")
        
        result = await document_service.query_knowledge(
            query=request.query,
            top_k=request.top_k,
            document_ids=request.document_ids
        )
        
        logger.info(f"问答响应生成成功，回答长度={len(result.get('answer', ''))}")
        
        return ChatResponse(
            answer=result["answer"],
            sources=[SourceChunk(**src) for src in result["sources"]]
        )
    except Exception as e:
        logger.error(f"问答处理失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"问答处理失败: {str(e)}")
