import openai
from typing import List
from app.core.config import settings
import logging
import httpx

logger = logging.getLogger(__name__)

class OpenAIService:
    def __init__(self):
        logger.info(f"Initializing OpenAI client with base_url: {settings.OPENAI_API_BASE}")
        logger.info(f"API Key set: {settings.OPENAI_API_KEY is not None and len(settings.OPENAI_API_KEY) > 0}")
        logger.info(f"API Key length: {len(settings.OPENAI_API_KEY) if settings.OPENAI_API_KEY else 0}")
        self.client = openai.AsyncOpenAI(
            api_key=settings.OPENAI_API_KEY,
            base_url=settings.OPENAI_API_BASE,
            timeout=httpx.Timeout(120.0, connect=10.0)
        )
        logger.info("OpenAI client initialized successfully")

    async def get_embeddings(self, texts: List[str]) -> List[List[float]]:
        logger.info(f"Getting embeddings for {len(texts)} texts...")
        try:
            response = await self.client.embeddings.create(
                model=settings.OPENAI_EMBEDDING_MODEL,
                input=texts
            )
            logger.info(f"Embeddings received, count: {len(response.data)}")
            return [embedding.embedding for embedding in response.data]
        except Exception as e:
            logger.error(f"Error getting embeddings: {e}", exc_info=True)
            raise

    async def get_embedding(self, text: str) -> List[float]:
        logger.info(f"Getting embedding for text: {text[:50]}...")
        embeddings = await self.get_embeddings([text])
        logger.info(f"Single embedding received, length: {len(embeddings[0])}")
        return embeddings[0]

    async def chat_completion(self, prompt: str, context: str) -> str:
        logger.info(f"chat_completion called with prompt: {prompt[:50]}..., context length: {len(context)}")

        system_message = f"""你是一个专业的知识库助手。请根据以下上下文信息回答用户的问题。
如果上下文不足以回答问题，请诚实地说明，不要编造信息。

上下文信息：
{context if context.strip() else '(无相关上下文，请基于你的知识回答)'}
"""

        try:
            logger.info("Calling OpenAI API...")
            response = await self.client.chat.completions.create(
                model=settings.OPENAI_CHAT_MODEL,
                messages=[
                    {"role": "system", "content": system_message},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.7,
                max_tokens=1000
            )
            logger.info(f"OpenAI API response received, content length: {len(response.choices[0].message.content)}")
            return response.choices[0].message.content
        except Exception as e:
            logger.error(f"Error in chat_completion: {e}", exc_info=True)
            raise


openai_service = OpenAIService()
