from fastapi import APIRouter, UploadFile, File, HTTPException, Depends
from pydantic import BaseModel
from typing import Optional, List
from app.services.document_service import document_service

router = APIRouter()


class ProcessDocumentRequest(BaseModel):
    document_id: int
    metadata: Optional[dict] = None


class ProcessDocumentResponse(BaseModel):
    document_id: int
    chunks_processed: int
    status: str


@router.post("/process", response_model=ProcessDocumentResponse)
async def process_document(
    document_id: int,
    file: UploadFile = File(...),
    metadata: Optional[dict] = None
):
    try:
        file_bytes = await file.read()
        chunks_processed = await document_service.process_document(
            document_id=document_id,
            file_bytes=file_bytes,
            filename=file.filename,
            metadata=metadata
        )
        return ProcessDocumentResponse(
            document_id=document_id,
            chunks_processed=chunks_processed,
            status="success"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/{document_id}")
async def delete_document(document_id: int):
    try:
        from app.services.milvus_service import milvus_service
        await milvus_service.delete_by_document_id(document_id)
        return {"status": "success", "document_id": document_id}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
