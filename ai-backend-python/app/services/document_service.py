from typing import List, Dict, Any
import pdfplumber
from docx import Document
from io import BytesIO
from app.services.openai_service import openai_service
from app.services.milvus_service import milvus_service
import logging

logger = logging.getLogger(__name__)


class DocumentService:
    """文档处理服务类，负责文档的提取、分块、嵌入和检索"""
    
    def __init__(self):
        """初始化文档服务，设置默认的分块参数"""
        self.chunk_size = 500    # 每个块的最大字符数
        self.chunk_overlap = 50  # 块之间的重叠字符数

    def extract_text_from_pdf(self, file_bytes: bytes) -> str:
        """从PDF文件中提取文本
        
        Args:
            file_bytes: PDF文件的二进制数据
            
        Returns:
            提取的文本内容
        """
        text = ""
        with pdfplumber.open(BytesIO(file_bytes)) as pdf:
            for page in pdf.pages:
                text += page.extract_text() or ""
        return text

    def extract_text_from_docx(self, file_bytes: bytes) -> str:
        """从DOCX文件中提取文本
        
        Args:
            file_bytes: DOCX文件的二进制数据
            
        Returns:
            提取的文本内容
        """
        doc = Document(BytesIO(file_bytes))
        text = "\n".join([paragraph.text for paragraph in doc.paragraphs])
        return text

    def extract_text_from_txt(self, file_bytes: bytes) -> str:
        """从TXT文件中提取文本
        
        Args:
            file_bytes: TXT文件的二进制数据
            
        Returns:
            提取的文本内容
        """
        return file_bytes.decode("utf-8")

    def split_text_into_chunks(self, text: str) -> List[str]:
        """将文本分割成多个块
        
        Args:
            text: 原始文本
            
        Returns:
            文本块列表
        """
        chunks = []
        start = 0
        text_length = len(text)
        
        while start < text_length:
            end = min(start + self.chunk_size, text_length)
            
            # 在单词边界处分割，避免断开单词
            if end < text_length:
                last_space = text.rfind(" ", start, end)
                if last_space != -1:
                    end = last_space
            
            chunks.append(text[start:end])
            
            # 如果已经到达末尾，退出循环
            if end >= text_length:
                break
            
            # 确保start向前移动，避免无限循环
            start = end - self.chunk_overlap
            if start <= 0 or start >= end:
                start = end
            
        return chunks

    async def process_document(self, document_id: int, file_bytes: bytes, filename: str, metadata: Dict = None) -> int:
        """处理文档并存储到知识库
        
        Args:
            document_id: 文档ID
            file_bytes: 文件二进制数据
            filename: 文件名
            metadata: 元数据（可选）
            
        Returns:
            处理的块数
        """
        logger.info(f"开始处理文档: document_id={document_id}, filename={filename}, 文件大小={len(file_bytes)} 字节")
        
        try:
            # 步骤1: 提取文本
            if filename.endswith(".pdf"):
                logger.info("步骤1: 从PDF提取文本...")
                text = self.extract_text_from_pdf(file_bytes)
            elif filename.endswith(".docx"):
                logger.info("步骤1: 从DOCX提取文本...")
                text = self.extract_text_from_docx(file_bytes)
            elif filename.endswith(".txt"):
                logger.info("步骤1: 从TXT提取文本...")
                text = self.extract_text_from_txt(file_bytes)
            else:
                raise ValueError(f"不支持的文件类型: {filename}")
            
            logger.info(f"步骤1完成: 文本长度={len(text)} 字符")

            # 步骤2: 分割文本为块
            logger.info("步骤2: 分割文本为块...")
            chunks = self.split_text_into_chunks(text)
            logger.info(f"步骤2完成: 创建了 {len(chunks)} 个块")

            # 步骤3: 获取嵌入向量
            logger.info("步骤3: 获取块的嵌入向量...")
            embeddings = await openai_service.get_embeddings(chunks)
            logger.info(f"步骤3完成: 获取了 {len(embeddings)} 个嵌入向量")

            # 步骤4: 准备块数据
            logger.info("步骤4: 准备块数据...")
            chunk_data = []
            for i, (chunk, embedding) in enumerate(zip(chunks, embeddings)):
                chunk_data.append({
                    "chunk_id": i,
                    "content": chunk,
                    "embedding": embedding,
                    "metadata": metadata or {}
                })
            logger.info(f"步骤4完成: 准备了 {len(chunk_data)} 个块数据")

            # 步骤5: 插入Milvus
            logger.info("步骤5: 插入向量到Milvus...")
            await milvus_service.insert_vectors(document_id, chunk_data)
            logger.info("步骤5完成: 向量插入成功")

            logger.info(f"文档处理完成: {len(chunks)} 个块已处理")
            return len(chunks)
        except Exception as e:
            logger.error(f"文档处理失败: {str(e)}", exc_info=True)
            raise

    async def query_knowledge(self, query: str, top_k: int = 5, document_ids: List[int] = None) -> Dict[str, Any]:
        """查询知识库
        
        Args:
            query: 用户提问
            top_k: 返回的相似块数量
            document_ids: 指定的文档ID列表（可选）
            
        Returns:
            包含回答和来源的字典
        """
        logger.info(f"开始知识库查询: query={query[:50]}..., top_k={top_k}, document_ids={document_ids}")
        
        # 步骤1: 获取查询嵌入向量
        logger.info("步骤1: 获取查询嵌入向量...")
        query_embedding = await openai_service.get_embedding(query)
        logger.info(f"步骤1完成: 嵌入向量维度={len(query_embedding)}")
        
        # 步骤2: 在Milvus中搜索相似块
        logger.info("步骤2: 在Milvus中搜索相似块...")
        similar_chunks = await milvus_service.search_similar(query_embedding, top_k, document_ids)
        logger.info(f"步骤2完成: 找到 {len(similar_chunks)} 个相似块")
        
        # 步骤3: 构建上下文
        context = "\n\n".join([f"[{i+1}] {chunk['content']}" for i, chunk in enumerate(similar_chunks)])
        logger.info(f"步骤3: 上下文长度={len(context)}")
        
        # 步骤4: 生成回答
        logger.info("步骤4: 生成对话响应...")
        answer = await openai_service.chat_completion(query, context)
        logger.info(f"步骤4完成: 回答长度={len(answer)}")
        
        logger.info("知识库查询完成")
        
        return {
            "answer": answer,
            "sources": similar_chunks
        }


document_service = DocumentService()
