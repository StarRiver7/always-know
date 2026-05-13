"""
RAG AI后端服务主入口文件

该服务提供文档处理、向量检索和智能问答功能，基于FastAPI框架和LangChain构建。
"""

import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from app.api.v1 import api_router
from app.core.config import settings
from app.services.document_service import document_service

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    应用生命周期管理
    
    在应用启动时执行初始化操作，在应用关闭时执行清理操作。
    """
    logger.info("========== 启动RAG AI后端服务 ==========")
    
    logger.info("初始化LangChain向量存储...")
    await document_service.init_vector_store()
    
    logger.info("========== RAG AI后端服务启动完成 ==========")
    
    yield
    
    logger.info("========== 关闭RAG AI后端服务 ==========")
    logger.info("========== RAG AI后端服务已关闭 ==========")


app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    lifespan=lifespan,
    description="基于LangChain RAG架构的AI知识库问答服务"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(api_router, prefix="/api/v1")


@app.get("/health", summary="健康检查", description="检查服务是否正常运行")
async def health_check():
    return {"status": "healthy", "service": "ai-backend", "version": settings.APP_VERSION}


if __name__ == "__main__":
    import uvicorn
    logger.info(f"启动服务: http://{settings.HOST}:{settings.PORT}")
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        workers=4,
        reload=False
    )