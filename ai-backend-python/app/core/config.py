from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    APP_NAME: str = "RAG AI Backend"
    APP_VERSION: str = "1.0.0"
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    OPENAI_API_KEY: str
    OPENAI_API_BASE: str = "https://open.bigmodel.cn/api/paas/v4"
    OPENAI_EMBEDDING_MODEL: str = "embedding-2"
    OPENAI_CHAT_MODEL: str = "glm-5.1"

    MILVUS_HOST: str = "localhost"
    MILVUS_PORT: int = 19530
    MILVUS_COLLECTION_NAME: str = "rag_knowledge_base"
    EMBEDDING_DIMENSION: int = 1024

    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_DB: int = 0

    THREAD_POOL_SIZE: int = 50
    MAX_WORKERS: int = 100

    JAVA_BACKEND_URL: str = "http://localhost:8080"

    class Config:
        env_file = ".env"


@lru_cache()
def get_settings():
    return Settings()


settings = get_settings()
