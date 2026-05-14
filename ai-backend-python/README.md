# RAG AI 后端服务 - Python

## 项目概述

本项目是一个基于 Retrieval-Augmented Generation (RAG) 架构的 AI 后端服务，提供文档处理、向量检索和智能问答功能。

## 技术栈

| 组件     | 技术           | 版本要求       |
| ------ | ------------ | ---------- |
| 框架     | FastAPI      | >= 0.104.1 |
| 向量数据库  | Milvus       | >= 2.4.0   |
| 大语言模型  | 智谱 GLM       | -          |
| 嵌入模型   | 智谱 Embedding | -          |
| AI框架   | LangChain    | >= 0.1.0   |
| Python | -            | >= 3.10    |

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
│  ┌─────────────────────────────────────────────────────────┐│
│  │ DocumentService (LangChain)                            ││
│  │  - RecursiveCharacterTextSplitter (文本分块)           ││
│  │  - OpenAIEmbeddings (嵌入生成)                        ││
│  │  - Milvus Vector Store (向量存储)                      ││
│  │  - ChatOpenAI (对话生成)                              ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
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
│  │  - id (主键, auto_id)                                 │   │
│  │  - document_id (文档ID)                              │   │
│  │  - chunk_id (块ID)                                   │   │
│  │  - page_content (文本内容)                            │   │
│  │  - embedding (向量嵌入 1024维)                        │   │
│  │  - metadata (元数据)                                  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 核心流程图

#### 文档上传流程

```
客户端上传文档 → DocumentAPI → DocumentService
    → 文本提取 (pdfplumber/python-docx)
    → 文本分块 (RecursiveCharacterTextSplitter)
    → 嵌入生成 (OpenAIEmbeddings)
    → Milvus存储 (LangChain Milvus)
```

#### 知识问答流程

```
用户提问 → ChatAPI → DocumentService
    → 嵌入生成 (OpenAIEmbeddings)
    → 向量检索 (Milvus similarity search)
    → 构建上下文 (PromptTemplate)
    → LLM生成 (ChatOpenAI)
    → 返回结果
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
│       └── document_service.py   # 文档处理服务 (LangChain)
├── test/                         # 测试文件
│   ├── test_chat.py              # 问答测试
│   ├── test_milvus.py            # Milvus测试
│   ├── check_milvus.py           # Milvus检查脚本
│   └── test_chat.ps1             # PowerShell测试脚本
├── .env                          # 环境变量配置
├── main.py                       # 应用入口
└── requirements.txt              # 依赖列表
```

## 核心模块详解

### 1. 配置模块 (core/config.py)

负责管理应用配置，支持从环境变量和 `.env` 文件读取配置。

**主要配置项：**

| 配置项                      | 类型  | 默认值                                    | 说明          |
| ------------------------ | --- | -------------------------------------- | ----------- |
| APP\_NAME                | str | RAG AI Backend                         | 应用名称        |
| APP\_VERSION             | str | 1.0.0                                  | 应用版本        |
| HOST                     | str | 0.0.0.0                                | 服务地址        |
| PORT                     | int | 8000                                   | 服务端口        |
| OPENAI\_API\_KEY         | str | -                                      | 智谱API密钥（必填） |
| OPENAI\_API\_BASE        | str | <https://open.bigmodel.cn/api/paas/v4> | API地址       |
| OPENAI\_EMBEDDING\_MODEL | str | embedding-2                            | 嵌入模型        |
| OPENAI\_CHAT\_MODEL      | str | glm-5.1                                | 对话模型        |
| MILVUS\_HOST             | str | localhost                              | Milvus地址    |
| MILVUS\_PORT             | int | 19530                                  | Milvus端口    |
| MILVUS\_COLLECTION\_NAME | str | rag\_knowledge\_base                   | 集合名称        |
| EMBEDDING\_DIMENSION     | int | 1024                                   | 向量维度        |

### 2. 文档服务 (services/document\_service.py)

基于 LangChain 框架的文档处理服务，提供文档处理和知识查询的核心业务逻辑。

**LangChain 组件：**

| 组件                             | 用途      |
| ------------------------------ | ------- |
| RecursiveCharacterTextSplitter | 语义文本分块  |
| OpenAIEmbeddings               | 文本嵌入生成  |
| ChatOpenAI                     | 大语言模型对话 |
| PromptTemplate                 | 提示词模板   |
| Milvus (langchain-community)   | 向量数据库存储 |

**核心方法：**

| 方法名                       | 功能       | 参数                                            | 返回值        |
| ------------------------- | -------- | --------------------------------------------- | ---------- |
| extract\_text\_from\_pdf  | PDF文本提取  | file\_bytes                                   | str        |
| extract\_text\_from\_docx | DOCX文本提取 | file\_bytes                                   | str        |
| extract\_text\_from\_txt  | TXT文本提取  | file\_bytes                                   | str        |
| split\_text\_into\_chunks | 文本分块     | text                                          | List\[str] |
| process\_document         | 处理并存储文档  | document\_id, file\_bytes, filename, metadata | int        |
| query\_knowledge          | 知识库查询    | query, top\_k, document\_ids                  | Dict       |

**语义分块策略：**

```python
RecursiveCharacterTextSplitter(
    chunk_size=500,           # 每个块的最大字符数
    chunk_overlap=50,         # 块之间的重叠字符数
    separators=[
        "\n\n",               # 段落分隔
        "\n",                 # 换行符
        "。", "！", "？",      # 中文句末标点
        ".", "!", "?",        # 英文句末标点
        "；", ";",            # 分号
        "，", ",",            # 逗号
        " ",                  # 空格
        ""                    # 兜底
    ]
)
```

### 3. API端点

#### 文档端点 (api/v1/endpoints/document.py)

| 接口                | 方法     | 路径                               | 功能      |
| ----------------- | ------ | -------------------------------- | ------- |
| process\_document | POST   | /api/v1/documents/process        | 上传并处理文档 |
| delete\_document  | DELETE | /api/v1/documents/{document\_id} | 删除文档    |

#### 问答端点 (api/v1/endpoints/chat.py)

| 接口          | 方法   | 路径                 | 功能   |
| ----------- | ---- | ------------------ | ---- |
| chat\_query | POST | /api/v1/chat/query | 知识问答 |

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

| 参数           | 类型   | 必填 | 说明                    |
| ------------ | ---- | -- | --------------------- |
| document\_id | int  | 是  | 文档唯一标识                |
| file         | File | 是  | 上传的文件（支持pdf/docx/txt） |
| metadata     | dict | 否  | 文档元数据                 |

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

| 参数           | 类型  | 必填 | 说明       |
| ------------ | --- | -- | -------- |
| document\_id | int | 是  | 要删除的文档ID |

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

| 参数            | 类型         | 必填 | 默认值  | 说明       |
| ------------- | ---------- | -- | ---- | -------- |
| query         | str        | 是  | -    | 用户提问     |
| top\_k        | int        | 否  | 5    | 返回相似文档数量 |
| document\_ids | List\[int] | 否  | None | 指定文档ID过滤 |

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

**主要依赖：**

| 依赖包                 | 用途                     |
| ------------------- | ---------------------- |
| fastapi             | Web框架                  |
| uvicorn             | ASGI服务器                |
| langchain           | LangChain核心库           |
| langchain-openai    | LangChain OpenAI集成     |
| langchain-community | LangChain社区集成（Milvus等） |
| pymilvus            | Milvus客户端              |
| pdfplumber          | PDF解析                  |
| python-docx         | DOCX解析                 |

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
2. 初始化 LangChain 组件
3. 连接 Milvus 向量数据库
4. 启动 FastAPI 服务
5. 注册 API 路由

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
   from app.services.document_service import document_service
   import asyncio
   result = await document_service.embeddings.aembed_query("测试")
   print(f"实际向量维度: {len(result)}")
   ```
2. 修改 `app/core/config.py` 中的 `EMBEDDING_DIMENSION` 为实际维度
3. 重启服务（已配置 `drop_old=True` 会自动重建集合）

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

### 4. 文档上传返回404

**错误信息：**

```
404 Not Found
```

**解决方案：**
检查请求路径是否为 `/api/v1/documents/process`（不是 `/api/v1/documents/documents/process`）

### 5. LangChain 版本兼容性问题

**解决方案：**
确保安装兼容的依赖版本：

```bash
pip install langchain>=0.1.0 langchain-openai>=0.0.2 langchain-community>=0.0.10
```

## 调试技巧

### 查看服务日志

服务启动后会输出详细日志，包含：

- 文档处理进度（步骤1-4）
- 检索到的文档数量和内容预览
- 发送给LLM的prompt长度
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
- 更高级的语义分块（SemanticChunker）

## 许可证

MIT License
