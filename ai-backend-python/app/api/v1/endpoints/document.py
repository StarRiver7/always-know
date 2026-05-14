"""
文档处理接口路由

提供文档上传、处理和删除功能，支持PDF、DOCX和TXT格式的文档。

处理流程：
1. 文档上传 → 2. 文本提取 → 3. LangChain文本分块 → 4. 生成嵌入向量 → 5. 存储到Milvus向量数据库

技术栈：
- FastAPI: API框架
- LangChain: 文本分块和向量处理
- Milvus: 向量数据库
- pdfplumber/python-docx: 文档解析
"""

from fastapi import APIRouter, UploadFile, File, HTTPException
from pydantic import BaseModel
from typing import Optional, List
from app.services.document_service import document_service
import logging

logger = logging.getLogger(__name__)

router = APIRouter(
    tags=["文档服务"],
    responses={
        400: {"description": "请求参数错误"},
        500: {"description": "服务器内部错误"}
    }
)

# 文档处理请求模型
class ProcessDocumentRequest(BaseModel):
    """
    文档处理请求模型
    
    Attributes:
        document_id: 文档唯一标识，由业务系统生成
        metadata: 文档元数据，如文档类型、上传时间、上传者等（可选）
    """
    document_id: int
    metadata: Optional[dict] = None


class ProcessDocumentResponse(BaseModel):
    """
    文档处理响应模型
    
    Attributes:
        document_id: 文档唯一标识
        chunks_processed: 成功处理的文本块数量
        status: 处理状态，成功为"success"
    """
    document_id: int
    chunks_processed: int
    status: str


class DeleteDocumentResponse(BaseModel):
    """
    删除文档响应模型
    
    Attributes:
        status: 删除状态，成功为"success"
        document_id: 被删除的文档ID
    """
    status: str
    document_id: int

# 文档处理接口
@router.post(
    "/process",
    response_model=ProcessDocumentResponse,
    summary="上传并处理文档",
    description="""
    将上传的文档进行处理并存储到知识库。
    
    **处理步骤**:
    1. 提取文档文本内容（支持PDF/DOCX/TXT格式）
    2. 使用LangChain的RecursiveCharacterTextSplitter进行文本分块
    3. 调用OpenAI Embedding生成向量
    4. 将向量和元数据存储到Milvus向量数据库
    
    **支持的文件格式**:
    - PDF: .pdf
    - DOCX: .docx  
    - TXT: .txt
    
    **请求参数**:
    - document_id: 文档唯一标识，需保证全局唯一
    - file: 上传的文件对象
    - metadata: 可选的文档元数据，会随向量一起存储
    
    **返回值**:
    - document_id: 文档ID
    - chunks_processed: 生成的文本块数量
    - status: "success"表示成功
    
    **错误码**:
    - 400: 文件格式不支持或参数错误
    - 500: 服务器处理失败
    """
)
async def process_document(
    document_id: int,
    file: UploadFile = File(..., description="上传的文档文件（支持pdf/docx/txt格式）"),
    metadata: Optional[dict] = None
):
    try:
        logger.info(f"开始处理文档: document_id={document_id}, filename={file.filename}")
        
        file_bytes = await file.read()
        # 调用文档服务处理文档
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


@router.delete(
    "/{document_id}",
    response_model=DeleteDocumentResponse,
    summary="删除文档",
    description="""
    从Milvus向量数据库中删除指定文档的所有向量数据。
    
    **删除范围**:
    - 删除该document_id对应的所有文本块向量
    - 删除相关的元数据信息
    
    **请求参数**:
    - document_id: 要删除的文档ID
    
    **返回值**:
    - status: "success"表示成功
    - document_id: 被删除的文档ID
    
    **错误码**:
    - 500: 删除操作失败
    """
)
async def delete_document(document_id: int):
    try:
        logger.info(f"删除文档: document_id={document_id}")
        
        await document_service.delete_document(document_id)
        
        logger.info(f"文档删除成功: document_id={document_id}")
        
        return DeleteDocumentResponse(
            status="success",
            document_id=document_id
        )
    except Exception as e:
        logger.error(f"文档删除失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"文档删除失败: {str(e)}")
