"""
文档处理接口路由
提供文档上传、处理和删除功能，支持PDF、DOCX和TXT格式的文档。
"""

from fastapi import APIRouter, UploadFile, File, HTTPException
from pydantic import BaseModel
from typing import Optional, List
from app.services.document_service import document_service
from app.services.milvus_service import milvus_service
import logging

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/documents",
    tags=["文档服务"]
)


class ProcessDocumentRequest(BaseModel):
    """文档处理请求模型"""
    document_id: int      # 文档ID
    metadata: Optional[dict] = None  # 元数据（可选）


class ProcessDocumentResponse(BaseModel):
    """文档处理响应模型"""
    document_id: int         # 文档ID
    chunks_processed: int    # 处理的块数
    status: str              # 处理状态


@router.post("/process", response_model=ProcessDocumentResponse, summary="处理文档")
async def process_document(
    document_id: int,
    file: UploadFile = File(...),
    metadata: Optional[dict] = None
):
    """
    上传并处理文档
    
    将上传的文档提取文本、分割成块、生成嵌入向量并存储到Milvus知识库。
    
    Args:
        document_id: 文档唯一标识
        file: 上传的文件（支持pdf/docx/txt格式）
        metadata: 文档元数据（可选）
        
    Returns:
        包含处理结果的响应
        
    Raises:
        HTTPException: 处理过程中发生错误时返回500状态码
    """
    try:
        logger.info(f"开始处理文档: document_id={document_id}, filename={file.filename}")
        
        file_bytes = await file.read()
        chunks_processed = await document_service.process_document(
            document_id=document_id,
            file_bytes=file_bytes,
            filename=file.filename,
            metadata=metadata
        )
        
        logger.info(f"文档处理成功: document_id={document_id}, chunks_processed={chunks_processed}")
        
        return ProcessDocumentResponse(
            document_id=document_id,
            chunks_processed=chunks_processed,
            status="success"
        )
    except ValueError as e:
        logger.error(f"文档处理失败（无效参数）: {str(e)}")
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error(f"文档处理失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"文档处理失败: {str(e)}")


@router.delete("/{document_id}", summary="删除文档")
async def delete_document(document_id: int):
    """
    删除文档
    
    从Milvus知识库中删除指定文档的所有向量数据。
    
    Args:
        document_id: 要删除的文档ID
        
    Returns:
        删除结果
        
    Raises:
        HTTPException: 删除过程中发生错误时返回500状态码
    """
    try:
        logger.info(f"删除文档: document_id={document_id}")
        
        await milvus_service.delete_by_document_id(document_id)
        
        logger.info(f"文档删除成功: document_id={document_id}")
        
        return {"status": "success", "document_id": document_id}
    except Exception as e:
        logger.error(f"文档删除失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"文档删除失败: {str(e)}")
