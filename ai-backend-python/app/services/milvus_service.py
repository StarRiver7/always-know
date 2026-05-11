from typing import List, Dict, Any
from pymilvus import connections, Collection, CollectionSchema, FieldSchema, DataType, utility
from app.core.config import settings
import asyncio
import logging

logger = logging.getLogger(__name__)


class MilvusService:
    def __init__(self):
        self.collection = None
        self.connection_alias = "default"

    def _sync_init_connection(self):
        logger.info(f"Connecting to Milvus at {settings.MILVUS_HOST}:{settings.MILVUS_PORT}...")
        connections.connect(
            alias=self.connection_alias,
            host=settings.MILVUS_HOST,
            port=settings.MILVUS_PORT,
            timeout=30
        )
        logger.info("Milvus connection established")

    def _sync_create_collection_if_not_exists(self):
        logger.info(f"Checking collection: {settings.MILVUS_COLLECTION_NAME}")
        if utility.has_collection(settings.MILVUS_COLLECTION_NAME):
            self.collection = Collection(settings.MILVUS_COLLECTION_NAME)
            self.collection.load()
            logger.info("Collection loaded (already exists)")
            return

        logger.info("Creating new collection...")
        fields = [
            FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
            FieldSchema(name="document_id", dtype=DataType.INT64),
            FieldSchema(name="chunk_id", dtype=DataType.INT64),
            FieldSchema(name="content", dtype=DataType.VARCHAR, max_length=65535),
            FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=settings.EMBEDDING_DIMENSION),
            FieldSchema(name="metadata", dtype=DataType.JSON),
        ]

        schema = CollectionSchema(fields=fields, description="RAG knowledge base")
        self.collection = Collection(
            name=settings.MILVUS_COLLECTION_NAME,
            schema=schema
        )

        index_params = {
            "index_type": "IVF_FLAT",
            "metric_type": "IP",
            "params": {"nlist": 1024}
        }
        self.collection.create_index(
            field_name="embedding",
            index_params=index_params
        )
        self.collection.load()
        logger.info("Collection created and loaded")

    async def init_connection(self):
        loop = asyncio.get_event_loop()
        await loop.run_in_executor(None, self._sync_init_connection)

    async def create_collection_if_not_exists(self):
        loop = asyncio.get_event_loop()
        await loop.run_in_executor(None, self._sync_create_collection_if_not_exists)

    def _sync_insert_vectors(self, document_id: int, chunks: List[Dict[str, Any]]):
        data = [
            [document_id] * len(chunks),
            [chunk["chunk_id"] for chunk in chunks],
            [chunk["content"] for chunk in chunks],
            [chunk["embedding"] for chunk in chunks],
            [chunk.get("metadata", {}) for chunk in chunks]
        ]
        self.collection.insert(data)
        self.collection.flush()
        logger.info(f"Inserted {len(chunks)} chunks for document {document_id}")

    async def insert_vectors(self, document_id: int, chunks: List[Dict[str, Any]]):
        loop = asyncio.get_event_loop()
        await loop.run_in_executor(None, self._sync_insert_vectors, document_id, chunks)

    def _sync_search_similar(self, embedding: List[float], top_k: int = 5, document_ids: List[int] = None) -> List[Dict]:
        search_params = {"metric_type": "IP", "params": {"nprobe": 16}}

        expr = None
        if document_ids:
            expr = f"document_id in {document_ids}"

        results = self.collection.search(
            data=[embedding],
            anns_field="embedding",
            param=search_params,
            limit=top_k,
            expr=expr,
            output_fields=["document_id", "chunk_id", "content", "metadata"]
        )

        hits = []
        for hit in results[0]:
            hits.append({
                "document_id": hit.entity.get("document_id"),
                "chunk_id": hit.entity.get("chunk_id"),
                "content": hit.entity.get("content"),
                "metadata": hit.entity.get("metadata"),
                "score": hit.score
            })
        return hits

    async def search_similar(self, embedding: List[float], top_k: int = 5, document_ids: List[int] = None) -> List[Dict]:
        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(None, self._sync_search_similar, embedding, top_k, document_ids)

    def _sync_delete_by_document_id(self, document_id: int):
        expr = f"document_id == {document_id}"
        self.collection.delete(expr)

    async def delete_by_document_id(self, document_id: int):
        loop = asyncio.get_event_loop()
        await loop.run_in_executor(None, self._sync_delete_by_document_id, document_id)


milvus_service = MilvusService()
