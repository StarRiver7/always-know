from typing import List, Dict, Any
from langchain_community.vectorstores import Milvus
from langchain_openai import OpenAIEmbeddings
from app.core.config import settings
import logging

logger = logging.getLogger(__name__)


class MilvusService:
    """基于LangChain的Milvus服务封装"""
    
    def __init__(self):
        self.vector_store = None
        self.embeddings = OpenAIEmbeddings(
            api_key=settings.OPENAI_API_KEY,
            base_url=settings.OPENAI_API_BASE,
            model=settings.OPENAI_EMBEDDING_MODEL
        )

    async def init_connection(self):
        """初始化Milvus连接"""
        logger.info(f"正在连接Milvus服务器: {settings.MILVUS_HOST}:{settings.MILVUS_PORT}...")
        self.vector_store = Milvus(
            embedding_function=self.embeddings,
            collection_name=settings.MILVUS_COLLECTION_NAME,
            connection_args={
                "host": settings.MILVUS_HOST,
                "port": settings.MILVUS_PORT
            },
            drop_old=False
        )
        logger.info("Milvus连接建立成功")

    async def create_collection_if_not_exists(self):
        """创建集合（如果不存在）"""
        logger.info(f"检查/创建集合: {settings.MILVUS_COLLECTION_NAME}")
        # LangChain Milvus会自动处理集合创建
        if self.vector_store is None:
            await self.init_connection()
        logger.info("集合准备完成")

    async def insert_vectors(self, document_id: int, chunks: List[Dict[str, Any]]):
        """插入向量数据到Milvus"""
        logger.info(f"正在插入文档 {document_id} 的 {len(chunks)} 个向量块...")
        
        texts = [chunk["content"] for chunk in chunks]
        metadatas = [{
            "document_id": document_id,
            "chunk_id": chunk["chunk_id"],
            **chunk.get("metadata", {})
        } for chunk in chunks]
        
        await self.vector_store.aadd_texts(texts, metadatas)
        logger.info(f"文档 {document_id} 的向量块插入成功")

    async def search_similar(self, embedding: List[float], top_k: int = 5, document_ids: List[int] = None) -> List[Dict]:
        """搜索相似向量"""
        logger.info(f"正在搜索相似向量，top_k={top_k}")
        
        filter_expr = None
        if document_ids:
            filter_expr = {"document_id": {"$in": document_ids}}
        
        results = await self.vector_store.asimilarity_search_by_vector_with_score(
            embedding,
            k=top_k,
            filter=filter_expr
        )
        
        hits = []
        for doc, score in results:
            metadata = doc.metadata
            hits.append({
                "document_id": metadata.get("document_id"),
                "chunk_id": metadata.get("chunk_id"),
                "content": doc.page_content,
                "metadata": {k: v for k, v in metadata.items() if k not in ["document_id", "chunk_id"]},
                "score": score
            })
        
        logger.info(f"搜索完成，找到 {len(hits)} 个相似块")
        return hits

    async def delete_by_document_id(self, document_id: int):
        """根据文档ID删除向量"""
        logger.info(f"正在删除文档 {document_id} 的向量...")
        
        # LangChain Milvus的delete方法
        await self.vector_store.adelete({
            "filter": {"document_id": document_id}
        })
        
        logger.info(f"文档 {document_id} 的向量删除成功")


milvus_service = MilvusService()