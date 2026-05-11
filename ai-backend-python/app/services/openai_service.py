import openai
from typing import List
from app.core.config import settings
import logging
import httpx

logger = logging.getLogger(__name__)

class OpenAIService:
    """OpenAI/智谱API服务封装类"""
    
    def __init__(self):
        """初始化OpenAI客户端"""
        logger.info(f"正在初始化OpenAI客户端，base_url: {settings.OPENAI_API_BASE}")
        logger.info(f"API Key已设置: {settings.OPENAI_API_KEY is not None and len(settings.OPENAI_API_KEY) > 0}")
        logger.info(f"API Key长度: {len(settings.OPENAI_API_KEY) if settings.OPENAI_API_KEY else 0}")
        self.client = openai.AsyncOpenAI(
            api_key=settings.OPENAI_API_KEY,
            base_url=settings.OPENAI_API_BASE,
            timeout=httpx.Timeout(120.0, connect=10.0)
        )
        logger.info("OpenAI客户端初始化成功")

    async def get_embeddings(self, texts: List[str]) -> List[List[float]]:
        """批量获取文本嵌入向量
        
        Args:
            texts: 文本列表
            
        Returns:
            嵌入向量列表
        """
        logger.info(f"正在获取 {len(texts)} 个文本的嵌入向量...")
        try:
            response = await self.client.embeddings.create(
                model=settings.OPENAI_EMBEDDING_MODEL,
                input=texts
            )
            logger.info(f"嵌入向量获取成功，数量: {len(response.data)}")
            return [embedding.embedding for embedding in response.data]
        except Exception as e:
            logger.error(f"获取嵌入向量失败: {e}", exc_info=True)
            raise

    async def get_embedding(self, text: str) -> List[float]:
        """获取单个文本的嵌入向量
        
        Args:
            text: 输入文本
            
        Returns:
            嵌入向量
        """
        logger.info(f"正在获取文本嵌入向量: {text[:50]}...")
        embeddings = await self.get_embeddings([text])
        logger.info(f"嵌入向量获取成功，维度: {len(embeddings[0])}")
        return embeddings[0]

    async def chat_completion(self, prompt: str, context: str) -> str:
        """生成对话响应
        
        Args:
            prompt: 用户提问
            context: 上下文信息
            
        Returns:
            AI生成的回答
        """
        logger.info(f"调用对话生成，提问长度: {len(prompt)}, 上下文长度: {len(context)}")

        system_message = f"""你是一个专业的知识库助手。请根据以下上下文信息回答用户的问题。
如果上下文不足以回答问题，请诚实地说明，不要编造信息。

上下文信息：
{context if context.strip() else '(无相关上下文，请基于你的知识回答)'}
"""

        try:
            logger.info("正在调用大模型API...")
            response = await self.client.chat.completions.create(
                model=settings.OPENAI_CHAT_MODEL,
                messages=[
                    {"role": "system", "content": system_message},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.7,
                max_tokens=1000
            )
            logger.info(f"大模型API响应成功，回答长度: {len(response.choices[0].message.content)}")
            return response.choices[0].message.content
        except Exception as e:
            logger.error(f"对话生成失败: {e}", exc_info=True)
            raise


openai_service = OpenAIService()
