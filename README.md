# RAG 知识库系统

一个企业级的检索增强生成（Retrieval-Augmented Generation）知识库系统，支持高并发、多用户访问。

## 技术栈

- **Python**: FastAPI + OpenAI（向量化和问答）
- **Java**: Spring Boot + MyBatis-Plus（业务逻辑、权限管理）
- **Vue**: 前端展示（问答界面）
- **Milvus**: 向量数据库（存储向量化文档）
- **MySQL**: 关系型数据库（用户、文档元信息）

## 项目结构

```
always know/
├── ai-backend-python/      # Python FastAPI 服务
├── business-backend-java/  # Java Spring Boot 服务
├── frontend-vue/           # Vue 前端
├── docs/                   # 项目文档
├── docker/                 # Docker 配置
└── k8s/                    # K8s 部署配置
```

## 快速开始

详细部署和使用说明请参考 [docs/README.md](./docs/README.md)。
