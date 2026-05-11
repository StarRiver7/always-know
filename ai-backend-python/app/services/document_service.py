from typing import List, Dict, Any
import pdfplumber
from docx import Document
from io import BytesIO
from app.services.openai_service import openai_service
from app.services.milvus_service import milvus_service
import logging

logger = logging.getLogger(__name__)


class DocumentService:
    def __init__(self):
        self.chunk_size = 500
        self.chunk_overlap = 50

    def extract_text_from_pdf(self, file_bytes: bytes) -> str:
        text = ""
        with pdfplumber.open(BytesIO(file_bytes)) as pdf:
            for page in pdf.pages:
                text += page.extract_text() or ""
        return text

    def extract_text_from_docx(self, file_bytes: bytes) -> str:
        doc = Document(BytesIO(file_bytes))
        text = "\n".join([paragraph.text for paragraph in doc.paragraphs])
        return text

    def extract_text_from_txt(self, file_bytes: bytes) -> str:
        return file_bytes.decode("utf-8")

    def split_text_into_chunks(self, text: str) -> List[str]:
        chunks = []
        start = 0
        text_length = len(text)
        
        while start < text_length:
            end = min(start + self.chunk_size, text_length)
            
            if end < text_length:
                last_space = text.rfind(" ", start, end)
                if last_space != -1:
                    end = last_space
            
            chunks.append(text[start:end])
            
            # 如果已经到达末尾，退出循环
            if end >= text_length:
                break
            
            # 确保 start 向前移动，避免无限循环
            start = end - self.chunk_overlap
            if start <= 0 or start >= end:
                start = end
            
        return chunks

    async def process_document(self, document_id: int, file_bytes: bytes, filename: str, metadata: Dict = None) -> int:
        logger.info(f"process_document started: document_id={document_id}, filename={filename}, file_size={len(file_bytes)} bytes")
        
        try:
            if filename.endswith(".pdf"):
                logger.info("Step 1: Extracting text from PDF...")
                text = self.extract_text_from_pdf(file_bytes)
            elif filename.endswith(".docx"):
                logger.info("Step 1: Extracting text from DOCX...")
                text = self.extract_text_from_docx(file_bytes)
            elif filename.endswith(".txt"):
                logger.info("Step 1: Extracting text from TXT...")
                text = self.extract_text_from_txt(file_bytes)
            else:
                raise ValueError(f"Unsupported file type: {filename}")
            
            logger.info(f"Step 1 completed: text length={len(text)} characters")

            logger.info("Step 2: Splitting text into chunks...")
            chunks = self.split_text_into_chunks(text)
            logger.info(f"Step 2 completed: {len(chunks)} chunks created")

            logger.info("Step 3: Getting embeddings for chunks...")
            embeddings = await openai_service.get_embeddings(chunks)
            logger.info(f"Step 3 completed: {len(embeddings)} embeddings received")

            logger.info("Step 4: Preparing chunk data...")
            chunk_data = []
            for i, (chunk, embedding) in enumerate(zip(chunks, embeddings)):
                chunk_data.append({
                    "chunk_id": i,
                    "content": chunk,
                    "embedding": embedding,
                    "metadata": metadata or {}
                })
            logger.info(f"Step 4 completed: {len(chunk_data)} chunk data prepared")

            logger.info("Step 5: Inserting vectors into Milvus...")
            await milvus_service.insert_vectors(document_id, chunk_data)
            logger.info(f"Step 5 completed: vectors inserted")

            logger.info(f"process_document completed successfully: {len(chunks)} chunks processed")
            return len(chunks)
        except Exception as e:
            logger.error(f"process_document failed: {str(e)}", exc_info=True)
            raise

    async def query_knowledge(self, query: str, top_k: int = 5, document_ids: List[int] = None) -> Dict[str, Any]:
        logger.info(f"query_knowledge started: query={query[:50]}..., top_k={top_k}, document_ids={document_ids}")
        
        logger.info("Step 1: Getting query embedding...")
        query_embedding = await openai_service.get_embedding(query)
        logger.info(f"Step 1 completed: embedding length={len(query_embedding)}")
        
        logger.info("Step 2: Searching similar chunks in Milvus...")
        similar_chunks = await milvus_service.search_similar(query_embedding, top_k, document_ids)
        logger.info(f"Step 2 completed: found {len(similar_chunks)} similar chunks")
        
        context = "\n\n".join([f"[{i+1}] {chunk['content']}" for i, chunk in enumerate(similar_chunks)])
        logger.info(f"Step 3: Context length={len(context)}")
        
        logger.info("Step 4: Generating chat completion...")
        answer = await openai_service.chat_completion(query, context)
        logger.info(f"Step 4 completed: answer length={len(answer)}")
        
        logger.info("query_knowledge completed successfully")
        
        return {
            "answer": answer,
            "sources": similar_chunks
        }


document_service = DocumentService()
