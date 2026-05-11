"""
RAG AI后端服务主入口文件

该服务提供文档处理、向量检索和智能问答功能，基于FastAPI框架构建。
"""

import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from app.api.v1 import api_router
from app.core.config import settings
from app.services.milvus_service import milvus_service
from app.services.thread_pool_service import thread_pool_service

# 配置日志
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
    
    logger.info("初始化Milvus连接...")
    await milvus_service.init_connection()
    
    logger.info("创建/加载Milvus集合...")
    await milvus_service.create_collection_if_not_exists()
    
    logger.info("初始化线程池...")
    thread_pool_service.init_pool()
    
    logger.info("========== RAG AI后端服务启动完成 ==========")
    
    yield
    
    logger.info("========== 关闭RAG AI后端服务 ==========")
    logger.info("关闭线程池...")
    thread_pool_service.shutdown()
    logger.info("========== RAG AI后端服务已关闭 ==========")


# 创建FastAPI应用实例
app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    lifespan=lifespan,
    description="基于RAG架构的AI知识库问答服务"
)

# 配置跨域中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册API路由
app.include_router(api_router, prefix="/api/v1")


@app.get("/health", summary="健康检查", description="检查服务是否正常运行")
async def health_check():
    """
    健康检查接口
    
    返回服务状态信息，用于监控和负载均衡检测。
    
    Returns:
        包含状态和服务名称的字典
    """
    return {"status": "healthy", "service": "ai-backend", "version": settings.APP_VERSION}


if __name__ == "__main__":
    """
    应用入口
    
    直接运行此文件启动服务（生产环境建议使用uvicorn命令）
    """
    import uvicorn
    logger.info(f"启动服务: http://{settings.HOST}:{settings.PORT}")
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        workers=4,
        reload=False
    )
