# RAG AI 后端服务 - Python

## 项目概述

本项目是一个基于 Retrieval-Augmented Generation (RAG) 架构的 AI 后端服务，提供文档处理、向量检索和智能问答功能。

## 技术栈

| 组件 | 技术 | 版本要求 |
|------|------|----------|
| 框架 | FastAPI | >= 0.104.1 |
| 向量数据库 | Milvus | >= 2.4.0 |
| 大语言模型 | 智谱 GLM | - |
| 嵌入模型 | 智谱 Embedding | - |
| Python | - | >= 3.10 |

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 / 客户端                          │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP/REST
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                     FastAPI 服务层                          │
│  ┌─────────────┐    ┌─────────────┐                        │
│  │  Chat API   │    │ Document API│                        │
│  └──────┬──────┘    └──────┬──────┘                        │
└─────────┼──────────────────┼────────────────────────────────┘
          │                  │
          ▼                  ▼
┌─────────────────────────────────────────────────────────────┐
│                     业务服务层                              │
│  ┌───────────────────┐    ┌───────────────────┐            │
│  │ DocumentService   │    │ OpenAIService     │            │
│  │  - 文档处理       │    │  - 嵌入生成       │            │
│  │  - 知识查询       │    │  - 对话生成       │            │
│  └───────┬───────────┘    └───────────┬───────┘            │
└──────────┼─────────────────────────────┼────────────────────┘
           │                             │
           │                             ▼
           │              ┌─────────────────────────┐
           │              │   OpenAI/智谱 API       │
           │              └─────────────────────────┘
           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Milvus 向量数据库                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Collection: rag_knowledge_base                       │   │
│  │  - id (主键)                                         │   │
│  │  - document_id (文档ID)                              │   │
│  │  - chunk_id (块ID)                                   │   │
│  │  - content (文本内容)                                │   │
│  │  - embedding (向量嵌入 1024维)                       │   │
│  │  - metadata (元数据)                                 │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 核心流程图

#### 文档上传流程

```
客户端上传文档 → DocumentAPI → DocumentService → 文本提取 → 文本分块 → 嵌入生成 → Milvus存储
```

#### 知识问答流程

```
用户提问 → ChatAPI → DocumentService → 嵌入生成 → Milvus检索 → 构建上下文 → LLM生成 → 返回结果
```

## 目录结构

```
ai-backend-python/
├── app/                          # 应用主目录
│   ├── __init__.py
│   ├── api/                      # API路由层
│   │   ├── __init__.py
│   │   └── v1/                   # API版本v1
│   │       ├── __init__.py
│   │       ├── api.py            # 路由注册
│   │       └── endpoints/        # 具体端点
│   │           ├── __init__.py
│   │           ├── chat.py       # 问答接口
│   │           └── document.py   # 文档处理接口
│   ├── core/                     # 核心配置
│   │   ├── __init__.py
│   │   └── config.py             # 配置管理
│   └── services/                 # 业务服务层
│       ├── __init__.py
│       ├── document_service.py   # 文档处理服务
│       ├── milvus_service.py     # Milvus向量服务
│       ├── openai_service.py     # 大模型服务
│       └── thread_pool_service.py # 线程池服务
├── test/                         # 测试文件
│   ├── test_chat.py              # 问答测试
│   ├── test_milvus.py            # Milvus测试
│   └── test_chat.ps1             # PowerShell测试脚本
├── .env                          # 环境变量配置
├── check_milvus.py               # Milvus检查脚本
├── main.py                       # 应用入口
└── requirements.txt              # 依赖列表
```

## 核心模块详解

### 1. 配置模块 (core/config.py)

负责管理应用配置，支持从环境变量和 `.env` 文件读取配置。

**主要配置项：**

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| APP_NAME | str | RAG AI Backend | 应用名称 |
| APP_VERSION | str | 1.0.0 | 应用版本 |
| HOST | str | 0.0.0.0 | 服务地址 |
| PORT | int | 8000 | 服务端口 |
| OPENAI_API_KEY | str | - | 智谱API密钥（必填） |
| OPENAI_API_BASE | str | https://open.bigmodel.cn/api/paas/v4 | API地址 |
| OPENAI_EMBEDDING_MODEL | str | embedding-2 | 嵌入模型 |
| OPENAI_CHAT_MODEL | str | glm-5.1 | 对话模型 |
| MILVUS_HOST | str | localhost | Milvus地址 |
| MILVUS_PORT | int | 19530 | Milvus端口 |
| MILVUS_COLLECTION_NAME | str | rag_knowledge_base | 集合名称 |
| EMBEDDING_DIMENSION | int | 1024 | 向量维度 |

### 2. Milvus服务 (services/milvus_service.py)

提供向量数据库的连接、数据插入、检索和删除功能。

**核心方法：**

| 方法名 | 功能 | 参数 | 返回值 |
|--------|------|------|--------|
| init_connection | 初始化Milvus连接 | 无 | None |
| create_collection_if_not_exists | 创建/加载集合 | 无 | None |
| insert_vectors | 插入向量数据 | document_id, chunks | None |
| search_similar | 向量相似度检索 | embedding, top_k, document_ids | List[Dict] |
| delete_by_document_id | 删除文档向量 | document_id | None |

### 3. OpenAI服务 (services/openai_service.py)

封装大语言模型和嵌入模型的调用。

**核心方法：**

| 方法名 | 功能 | 参数 | 返回值 |
|--------|------|------|--------|
| get_embeddings | 批量获取文本嵌入 | texts: List[str] | List[List[float]] |
| get_embedding | 获取单个文本嵌入 | text: str | List[float] |
| chat_completion | 生成对话响应 | prompt, context | str |

### 4. 文档服务 (services/document_service.py)

提供文档处理和知识查询的核心业务逻辑。

**核心方法：**

| 方法名 | 功能 | 参数 | 返回值 |
|--------|------|------|--------|
| extract_text_from_pdf | PDF文本提取 | file_bytes | str |
| extract_text_from_docx | DOCX文本提取 | file_bytes | str |
| extract_text_from_txt | TXT文本提取 | file_bytes | str |
| split_text_into_chunks | 文本分块 | text | List[str] |
| process_document | 处理并存储文档 | document_id, file_bytes, filename, metadata | int |
| query_knowledge | 知识库查询 | query, top_k, document_ids | Dict |

### 5. API端点

#### 文档端点 (api/v1/endpoints/document.py)

| 接口 | 方法 | 路径 | 功能 |
|------|------|------|------|
| process_document | POST | /api/v1/documents/process | 上传并处理文档 |
| delete_document | DELETE | /api/v1/documents/{document_id} | 删除文档 |

#### 问答端点 (api/v1/endpoints/chat.py)

| 接口 | 方法 | 路径 | 功能 |
|------|------|------|------|
| chat_query | POST | /api/v1/chat/query | 知识问答 |

## API接口详细说明

### 1. 文档上传接口

**请求：**
```
POST /api/v1/documents/process?document_id=1
Content-Type: multipart/form-data

file: <文件二进制>
metadata: {"key": "value"} (可选)
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| document_id | int | 是 | 文档唯一标识 |
| file | File | 是 | 上传的文件（支持pdf/docx/txt） |
| metadata | dict | 否 | 文档元数据 |

**成功响应 (200 OK)：**
```json
{
    "document_id": 1,
    "chunks_processed": 11,
    "status": "success"
}
```

**失败响应 (500 Internal Server Error)：**
```json
{
    "detail": "Unsupported file type: example.png"
}
```

### 2. 文档删除接口

**请求：**
```
DELETE /api/v1/documents/{document_id}
```

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| document_id | int | 是 | 要删除的文档ID |

**成功响应 (200 OK)：**
```json
{
    "status": "success",
    "document_id": 1
}
```

### 3. 问答接口

**请求：**
```
POST /api/v1/chat/query
Content-Type: application/json

{
    "query": "什么是RAG？",
    "top_k": 5,
    "document_ids": [1, 2, 3]
}
```

**请求体：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| query | str | 是 | - | 用户提问 |
| top_k | int | 否 | 5 | 返回相似文档数量 |
| document_ids | List[int] | 否 | None | 指定文档ID过滤 |

**成功响应 (200 OK)：**
```json
{
    "answer": "RAG（Retrieval-Augmented Generation）是一种结合检索和生成的AI技术...",
    "sources": [
        {
            "document_id": 1,
            "chunk_id": 3,
            "content": "RAG技术通过检索知识库中的相关信息...",
            "metadata": {},
            "score": 0.85
        }
    ]
}
```

### 4. 健康检查接口

**请求：**
```
GET /health
```

**成功响应 (200 OK)：**
```json
{
    "status": "healthy",
    "service": "ai-backend"
}
```

## 配置与部署

### 环境变量配置

创建 `.env` 文件：

```env
# 智谱API配置
OPENAI_API_KEY=your-api-key-here

# Milvus配置（如使用远程服务器需修改）
# MILVUS_HOST=remote-milvus-server
# MILVUS_PORT=19530

# 服务端口（可选）
# PORT=8000
```

### 依赖安装

```bash
# 创建虚拟环境（推荐）
conda create -n rag-env python=3.10
conda activate rag-env

# 安装依赖
pip install -r requirements.txt
```

### 启动服务

**开发模式：**
```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

**生产模式：**
```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

或使用内置启动方式：
```bash
python main.py
```

### 服务启动流程

1. 加载配置（从环境变量/.env）
2. 初始化Milvus连接
3. 创建/加载向量集合
4. 初始化线程池
5. 启动FastAPI服务
6. 注册API路由

## 使用示例

### Python客户端示例

```python
import httpx

# 上传文档
async def upload_document():
    async with httpx.AsyncClient() as client:
        with open("document.pdf", "rb") as f:
            response = await client.post(
                "http://localhost:8000/api/v1/documents/process?document_id=1",
                files={"file": f}
            )
        print(response.json())

# 知识问答
async def ask_question():
    async with httpx.AsyncClient() as client:
        response = await client.post(
            "http://localhost:8000/api/v1/chat/query",
            json={"query": "什么是人工智能？", "top_k": 5}
        )
        result = response.json()
        print("答案:", result["answer"])
        print("来源:", result["sources"])
```

### cURL示例

```bash
# 上传文档
curl -X POST "http://localhost:8000/api/v1/documents/process?document_id=1" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@example.pdf"

# 知识问答
curl -X POST "http://localhost:8000/api/v1/chat/query" \
  -H "Content-Type: application/json" \
  -d '{"query": "什么是RAG？", "top_k": 3}'

# 健康检查
curl http://localhost:8000/health
```

## 常见问题

### 1. 向量维度不匹配错误

**错误信息：**
```
MilvusException: (code=65535, message=the length(X) of float data should divide the dim(Y))
```

**原因：** 嵌入模型输出的向量维度与Milvus集合配置的维度不一致。

**解决方案：**
1. 使用以下代码确认实际向量维度：
   ```python
   from app.services.openai_service import openai_service
   import asyncio
   result = asyncio.run(openai_service.get_embedding("测试"))
   print(f"实际向量维度: {len(result)}")
   ```
2. 修改 `app/core/config.py` 中的 `EMBEDDING_DIMENSION` 为实际维度
3. 删除旧的Milvus集合并重启服务：
   ```bash
   python -c "from pymilvus import connections, utility; connections.connect('default', host='localhost', port='19530'); utility.drop_collection('rag_knowledge_base')"
   ```

### 2. Milvus连接失败

**错误信息：**
```
Failed to connect to Milvus
```

**解决方案：**
1. 确认Milvus服务正在运行：
   ```bash
   docker ps | grep milvus
   ```
2. 检查配置文件中的 `MILVUS_HOST` 和 `MILVUS_PORT` 是否正确
3. 测试连接：
   ```bash
   python test_milvus.py
   ```

### 3. API密钥无效

**错误信息：**
```
Invalid API key
```

**解决方案：**
1. 确认 `.env` 文件中的 `OPENAI_API_KEY` 正确设置
2. 检查API密钥是否过期或额度是否充足
3. 验证网络是否可以访问智谱API

## 调试技巧

### 查看服务日志

服务启动后会输出详细日志，包含：
- Milvus连接状态
- 文档处理进度
- API调用日志
- 错误堆栈信息

### 检查Milvus数据

使用 `check_milvus.py` 脚本检查数据：

```bash
python check_milvus.py
```

输出示例：
```
Collection exists: True
Collection name: rag_knowledge_base
Num entities: 100
Sample data:
  ID: 1, DocID: 1, Content: 这是文档内容...
```

## 性能优化建议

1. **批量处理**：上传多个文档时，考虑批量处理以减少API调用次数
2. **索引优化**：根据数据量调整Milvus索引参数（nlist, nprobe）
3. **缓存策略**：对高频查询结果进行缓存
4. **异步处理**：大文件处理使用异步方式避免阻塞
5. **资源限制**：根据服务器配置调整线程池大小

## 扩展功能

可考虑添加的功能：
- 文档版本管理
- 查询历史记录
- 权限控制
- 多租户支持
- 模型切换
- 性能监控
- 日志审计

## 许可证

MIT License