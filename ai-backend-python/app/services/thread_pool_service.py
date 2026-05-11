import asyncio
from concurrent.futures import ThreadPoolExecutor
from typing import Callable, Any
from app.core.config import settings


class ThreadPoolService:
    def __init__(self):
        self.executor = None

    def init_pool(self):
        self.executor = ThreadPoolExecutor(
            max_workers=settings.MAX_WORKERS,
            thread_name_prefix="rag-worker"
        )

    def shutdown(self):
        if self.executor:
            self.executor.shutdown(wait=True)

    async def run_in_executor(self, func: Callable, *args, **kwargs) -> Any:
        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(self.executor, lambda: func(*args, **kwargs))


thread_pool_service = ThreadPoolService()
