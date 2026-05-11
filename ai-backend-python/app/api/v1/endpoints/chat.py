from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional, List
from app.services.document_service import document_service
import logging

logger = logging.getLogger(__name__)

router = APIRouter()


class ChatRequest(BaseModel):
    query: str
    top_k: Optional[int] = 5
    document_ids: Optional[List[int]] = None


class SourceChunk(BaseModel):
    document_id: int
    chunk_id: int
    content: str
    metadata: dict
    score: float


class ChatResponse(BaseModel):
    answer: str
    sources: List[SourceChunk]


@router.post("/query", response_model=ChatResponse)
async def chat_query(request: ChatRequest):
    try:
        logger.info(f"Received chat request: query={request.query[:50]}..., top_k={request.top_k}, document_ids={request.document_ids}")
        
        result = await document_service.query_knowledge(
            query=request.query,
            top_k=request.top_k,
            document_ids=request.document_ids
        )
        
        logger.info(f"Chat response generated successfully, answer length={len(result.get('answer', ''))}")
        
        return ChatResponse(
            answer=result["answer"],
            sources=[SourceChunk(**src) for src in result["sources"]]
        )
    except Exception as e:
        logger.error(f"Chat error: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
