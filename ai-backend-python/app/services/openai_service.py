from typing import List
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain.schema import SystemMessage, HumanMessage
from app.core.config import settings
import logging

logger = logging.getLogger(__name__)


class OpenAIService:
    """基于LangChain的OpenAI服务封装"""
    
    def __init__(self):
        """初始化LangChain客户端"""
        logger.info(f"正在初始化LangChain OpenAI客户端...")
        
        self.embeddings = OpenAIEmbeddings(
            api_key=settings.OPENAI_API_KEY,
            base_url=settings.OPENAI_API_BASE,
            model=settings.OPENAI_EMBEDDING_MODEL
        )
        
        self.chat_model = ChatOpenAI(
            api_key=settings.OPENAI_API_KEY,
            base_url=settings.OPENAI_API_BASE,
            model=settings.OPENAI_CHAT_MODEL,
            temperature=0.7,
            max_tokens=1000
        )
        
        logger.info("LangChain OpenAI客户端初始化成功")

    async def get_embeddings(self, texts: List[str]) -> List[List[float]]:
        """批量获取文本嵌入向量"""
        logger.info(f"正在获取 {len(texts)} 个文本的嵌入向量...")
        try:
            embeddings = await self.embeddings.aembed_documents(texts)
            logger.info(f"嵌入向量获取成功，数量: {len(embeddings)}")
            return embeddings
        except Exception as e:
            logger.error(f"获取嵌入向量失败: {e}", exc_info=True)
            raise

    async def get_embedding(self, text: str) -> List[float]:
        """获取单个文本的嵌入向量"""
        logger.info(f"正在获取文本嵌入向量: {text[:50]}...")
        embedding = await self.embeddings.aembed_query(text)
        logger.info(f"嵌入向量获取成功，维度: {len(embedding)}")
        return embedding

    async def chat_completion(self, prompt: str, context: str) -> str:
        """生成对话响应"""
        logger.info(f"调用对话生成，提问长度: {len(prompt)}, 上下文长度: {len(context)}")

        system_message = SystemMessage(content=f"""你是一个专业的知识库助手。请根据以下上下文信息回答用户的问题。
如果上下文不足以回答问题，请诚实地说明，不要编造信息。

上下文信息：
{context if context.strip() else '(无相关上下文，请基于你的知识回答)'}
""")
        
        human_message = HumanMessage(content=prompt)

        try:
            logger.info("正在调用大模型API...")
            response = await self.chat_model.ainvoke([system_message, human_message])
            logger.info(f"大模型API响应成功，回答长度: {len(response.content)}")
            return response.content
        except Exception as e:
            logger.error(f"对话生成失败: {e}", exc_info=True)
            raise


openai_service = OpenAIService()