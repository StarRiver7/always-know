# RAG 知识库系统 - 完整文档

## 目录

- [系统架构设计](#系统架构设计)
- [快速开始](#快速开始)
- [服务部署指南](#服务部署指南)
- [API 接口文档](#api-接口文档)
- [Docker 部署](#docker-部署)
- [Kubernetes 部署](#kubernetes-部署)

---

## 系统架构设计

### 系统概述

RAG (Retrieval-Augmented Generation) 知识库系统是一个企业级的智能问答平台，支持文档上传、向量化存储、基于文档的智能问答。

### 架构图

```
┌──────────────┐
│   Vue 前端   │ (3000)
│  (用户界面)  │
└──────┬───────┘
       │ HTTP
       ▼
┌───────────────────────┐
│  Java Spring Boot    │ (8080)
│  业务服务            │
│  - 用户认证         │
│  - 文档管理         │
│  - 权限控制         │
└──────┬──────────────┘
       │ HTTP
       ▼
┌───────────────────────┐
│  Python FastAPI      │ (8000)
│  AI服务              │
│  - 文档向量化       │
│  - 向量检索         │
│  - LLM问答          │
└──────┬──────────────┘
       │
       ├───────────────┐
       ▼               ▼
┌──────────────┐  ┌──────────────┐
│   Milvus     │  │   OpenAI     │
│  向量数据库  │  │   API        │
└──────────────┘  └──────────────┘
       │
       ▼
┌──────────────┐
│    MySQL     │
│  元数据存储  │
└──────────────┘
```

### 技术栈

| 层级 | 技术选型 | 版本 |
|------|---------|------|
| 前端 | Vue 3 + Element Plus | 3.4+ |
| 后端（业务） | Java 17 + Spring Boot 3.2 + MyBatis-Plus + WebClient | 3.2.0 |
| 后端（AI） | Python 3.10 + FastAPI + LangChain | 0.104.1 |
| 向量数据库 | Milvus | 2.3+ |
| 关系型数据库 | MySQL | 8.0+ |
| 缓存（可选）| Redis | 7.0+ |

### 核心功能

1. **用户认证与授权**
   - JWT Token 认证
   - 角色权限管理
   - 文档级权限控制

2. **文档管理**
   - 支持 PDF、Word、TXT 格式
   - 文档上传与解析
   - 文本分块处理
   - 异步向量化

3. **智能问答**
   - 语义检索相关文档块
   - LLM 基于上下文回答
   - 支持选择指定文档问答
   - 显示答案来源

4. **高并发支持**
   - 线程池异步处理
   - 数据库连接池优化
   - Milvus 连接池

---

## 快速开始

### 前置条件

- JDK 17+
- Python 3.10+
- Node.js 18+
- MySQL 8.0+
- Milvus 2.3+
- OpenAI API Key

### 1. 数据库初始化

执行 SQL 脚本创建数据库和表：

```bash
mysql -u root -p < business-backend-java/src/main/resources/db/schema.sql
```

### 2. 启动 Milvus

使用 Docker 启动 Milvus：

```bash
docker run -d \
  --name milvus \
  -p 19530:19530 \
  -p 9091:9091 \
  milvusdb/milvus:latest
```

### 3. 启动 Python AI 服务

```bash
cd ai-backend-python

# 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 复制环境变量配置
cp .env.example .env
# 编辑 .env 文件，配置 OpenAI API Key

# 启动服务
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

### 4. 启动 Java 业务服务

```bash
cd business-backend-java

# 编辑 application.yml，配置数据库连接

# 使用 Maven 启动
mvn spring-boot:run

# 或打包后运行
mvn package
java -jar target/rag-backend-1.0.0.jar
```

### 5. 启动 Vue 前端

```bash
cd frontend-vue

# 安装依赖
npm install

# 启动开发服务
npm run dev

# 访问 http://localhost:3000
```

---

## 服务部署指南

### Python AI 服务

配置文件说明：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| OPENAI_API_KEY | OpenAI API 密钥 | - |
| OPENAI_API_BASE | OpenAI API 地址 | https://api.openai.com/v1 |
| MILVUS_HOST | Milvus 主机地址 | localhost |
| MILVUS_PORT | Milvus 端口 | 19530 |
| EMBEDDING_DIMENSION | 向量维度 | 1536 |
| MAX_WORKERS | 最大线程数 | 100 |

### Java 业务服务

配置文件说明：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| spring.datasource.url | 数据库连接 URL | - |
| jwt.secret | JWT 密钥 | - |
| ai.backend.url | AI 服务地址 | http://localhost:8000 |
| ai.backend.timeout.connect | 连接超时（毫秒） | 5000 |
| ai.backend.timeout.read | 读取超时（毫秒） | 60000 |
| ai.backend.timeout.write | 写入超时（毫秒） | 60000 |

---

## API 接口文档

### Java 业务服务 API (端口 8080)

#### 认证接口

##### 用户注册

```
POST /api/auth/register
Content-Type: application/json

{
  "username": "test",
  "password": "123456",
  "realName": "测试用户",
  "email": "test@example.com",
  "phone": "13800138000"
}
```

##### 用户登录

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "test",
  "password": "123456"
}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGc...",
    "user": { ... }
  }
}
```

#### 文档管理接口

##### 获取文档列表

```
GET /api/documents?pageNum=1&pageSize=10
Authorization: Bearer {token}
```

##### 上传文档

```
POST /api/documents/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

{
  "title": "文档标题",
  "file": (binary)
}
```

##### 删除文档

```
DELETE /api/documents/{id}
Authorization: Bearer {token}
```

#### 问答接口

##### 提问

```
POST /api/documents/chat
Content-Type: application/json
Authorization: Bearer {token}

{
  "query": "文档中提到了什么？",
  "documentIds": [1, 2, 3]  // 可选，指定文档ID
}

Response:
{
  "code": 200,
  "data": {
    "answer": "根据文档内容...",
    "sources": [
      {
        "documentId": 1,
        "chunkId": 0,
        "content": "相关的文档片段...",
        "score": 0.89
      }
    ]
  }
}
```

### Python AI 服务 API (端口 8000)

#### 文档处理接口

##### 处理文档

```
POST /api/v1/documents/process?documentId=1
Content-Type: multipart/form-data

{
  "file": (binary)
}
```

##### 删除文档向量

```
DELETE /api/v1/documents/{documentId}
```

#### 问答接口

##### 问答

```
POST /api/v1/chat/query
Content-Type: application/json

{
  "query": "问题",
  "topK": 5,
  "documentIds": [1, 2]
}
```

##### 健康检查

```
GET /health
```

---

## Docker 部署

### 1. 创建 Docker Compose

在项目根目录创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: rag-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: rag_db
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./business-backend-java/src/main/resources/db/schema.sql:/docker-entrypoint-initdb.d/schema.sql
    networks:
      - rag-network

  milvus:
    image: milvusdb/milvus:latest
    container_name: rag-milvus
    ports:
      - "19530:19530"
      - "9091:9091"
    volumes:
      - milvus-data:/var/lib/milvus
    networks:
      - rag-network

  redis:
    image: redis:7-alpine
    container_name: rag-redis
    ports:
      - "6379:6379"
    networks:
      - rag-network

  ai-backend:
    build:
      context: ./ai-backend-python
    container_name: rag-ai-backend
    ports:
      - "8000:8000"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - MILVUS_HOST=milvus
      - REDIS_HOST=redis
    depends_on:
      - milvus
      - redis
    networks:
      - rag-network

  business-backend:
    build:
      context: ./business-backend-java
    container_name: rag-business-backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/rag_db
      - AI_BACKEND_URL=http://ai-backend:8000
    depends_on:
      - mysql
      - ai-backend
    networks:
      - rag-network

  frontend:
    build:
      context: ./frontend-vue
    container_name: rag-frontend
    ports:
      - "3000:80"
    depends_on:
      - business-backend
    networks:
      - rag-network

volumes:
  mysql-data:
  milvus-data:

networks:
  rag-network:
    driver: bridge
```

### 2. 创建各服务的 Dockerfile

**Python AI 服务 Dockerfile (ai-backend-python/Dockerfile)**

```dockerfile
FROM python:3.10-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

COPY . .

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4"]
```

**Java 业务服务 Dockerfile (business-backend-java/Dockerfile)**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-Xms512m", "-Xmx2g", "-jar", "app.jar"]
```

**Vue 前端 Dockerfile (frontend-vue/Dockerfile)**

```dockerfile
FROM node:18-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm install --registry=https://registry.npmmirror.com

COPY . .
RUN npm run build

FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

**Nginx 配置 (frontend-vue/nginx.conf)**

```nginx
server {
    listen 80;
    server_name localhost;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://business-backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 3. 启动所有服务

```bash
# 创建 .env 文件
OPENAI_API_KEY=your-api-key-here

# 启动
docker-compose up -d

# 查看日志
docker-compose logs -f
```

---

## Kubernetes 部署

### 1. 创建 Namespace

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: rag-system
```

### 2. MySQL 部署

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pvc
  namespace: rag-system
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql
  namespace: rag-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
      - name: mysql
        image: mysql:8.0
        env:
        - name: MYSQL_ROOT_PASSWORD
          value: "root"
        - name: MYSQL_DATABASE
          value: "rag_db"
        ports:
        - containerPort: 3306
        volumeMounts:
        - name: mysql-storage
          mountPath: /var/lib/mysql
      volumes:
      - name: mysql-storage
        persistentVolumeClaim:
          claimName: mysql-pvc

---
apiVersion: v1
kind: Service
metadata:
  name: mysql
  namespace: rag-system
spec:
  selector:
    app: mysql
  ports:
  - port: 3306
    targetPort: 3306
```

### 3. Milvus 部署

使用 Milvus 官方 Helm Chart：

```bash
helm repo add milvus https://zilliztech.github.io/milvus-helm/
helm repo update
helm install milvus milvus/milvus --namespace rag-system --set cluster.enabled=false
```

### 4. Redis 部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: rag-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379

---
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: rag-system
spec:
  selector:
    app: redis
  ports:
  - port: 6379
    targetPort: 6379
```

### 5. AI 后端部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-backend
  namespace: rag-system
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ai-backend
  template:
    metadata:
      labels:
        app: ai-backend
    spec:
      containers:
      - name: ai-backend
        image: your-registry/rag-ai-backend:latest
        env:
        - name: MILVUS_HOST
          value: "milvus"
        - name: REDIS_HOST
          value: "redis"
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: rag-secrets
              key: openai-api-key
        ports:
        - containerPort: 8000
        resources:
          requests:
            cpu: "500m"
            memory: "512Mi"
          limits:
            cpu: "2000m"
            memory: "2Gi"
        livenessProbe:
          httpGet:
            path: /health
            port: 8000
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health
            port: 8000
          initialDelaySeconds: 5
          periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: ai-backend
  namespace: rag-system
spec:
  selector:
    app: ai-backend
  ports:
  - port: 8000
    targetPort: 8000

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-backend-hpa
  namespace: rag-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### 6. 业务后端部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: business-backend
  namespace: rag-system
spec:
  replicas: 3
  selector:
    matchLabels:
      app: business-backend
  template:
    metadata:
      labels:
        app: business-backend
    spec:
      containers:
      - name: business-backend
        image: your-registry/rag-business-backend:latest
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:mysql://mysql:3306/rag_db"
        - name: AI_BACKEND_URL
          value: "http://ai-backend:8000"
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: rag-secrets
              key: jwt-secret
        ports:
        - containerPort: 8080
        resources:
          requests:
            cpu: "500m"
            memory: "512Mi"
          limits:
            cpu: "2000m"
            memory: "2Gi"
        livenessProbe:
          httpGet:
            path: /api/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: business-backend
  namespace: rag-system
spec:
  selector:
    app: business-backend
  ports:
  - port: 8080
    targetPort: 8080

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: business-backend-hpa
  namespace: rag-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: business-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### 7. 前端部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: rag-system
spec:
  replicas: 2
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: frontend
        image: your-registry/rag-frontend:latest
        ports:
        - containerPort: 80
        resources:
          requests:
            cpu: "100m"
            memory: "128Mi"
          limits:
            cpu: "500m"
            memory: "256Mi"

---
apiVersion: v1
kind: Service
metadata:
  name: frontend
  namespace: rag-system
spec:
  selector:
    app: frontend
  ports:
  - port: 80
    targetPort: 80
  type: LoadBalancer
```

### 8. Ingress 配置

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: rag-ingress
  namespace: rag-system
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: rag.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend
            port:
              number: 80
```

### 9. Secret 配置

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: rag-secrets
  namespace: rag-system
type: Opaque
stringData:
  openai-api-key: "your-openai-api-key"
  jwt-secret: "your-jwt-secret-key"
```

### 应用部署

```bash
# 1. 创建命名空间
kubectl apply -f namespace.yaml

# 2. 创建 Secrets
kubectl apply -f secrets.yaml

# 3. 部署基础设施
kubectl apply -f mysql.yaml
kubectl apply -f redis.yaml
helm install milvus milvus/milvus --namespace rag-system

# 4. 部署应用服务
kubectl apply -f ai-backend.yaml
kubectl apply -f business-backend.yaml
kubectl apply -f frontend.yaml

# 5. 部署 Ingress
kubectl apply -f ingress.yaml

# 6. 查看状态
kubectl get pods -n rag-system
kubectl get svc -n rag-system
```

---

## 性能优化建议

### 数据库优化

- 为常用查询字段添加索引
- 使用连接池（HikariCP）
- 读写分离（可选）

### Milvus 优化

- 调整 `nlist` 和 `nprobe` 参数
- 使用 IVF_FLAT 或 HNSW 索引
- 根据数据量调整 Segments

### AI 服务优化

- 使用异步处理
- 调整线程池大小
- 缓存常见问题的答案

### 高并发配置

| 配置项 | 建议值 |
|--------|--------|
| Java 连接池大小 | 50-100 |
| Python 线程池 | 100-200 |
| Milvus 连接数 | 32-64 |
| HPA 最大副本数 | 10-20 |

---

## 监控与日志

### 日志配置

- Java: Logback/SLF4J
- Python: structlog
- 统一日志格式，包含 Trace ID

### 监控指标

- 接口响应时间
- API 调用次数
- LLM 调用耗时
- 系统资源使用

---

## 常见问题

### 1. Milvus 连接失败

检查 Milvus 服务状态，确保端口 19530 可访问。

### 2. 向量化速度慢

- 使用批量向量化
- 增加工作线程数
- 优化分块大小

### 3. 答案不准确

- 调整 `topK` 参数
- 优化分块策略
- 检查文档质量

---

## 附录

### 项目目录结构

```
always-know/
├── ai-backend-python/
│   ├── app/
│   │   ├── api/
│   │   ├── core/
│   │   └── services/
│   ├── main.py
│   ├── requirements.txt
│   └── .env.example
├── business-backend-java/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── pom.xml
│   └── Dockerfile
├── frontend-vue/
│   ├── src/
│   │   ├── api/
│   │   ├── views/
│   │   └── utils/
│   ├── package.json
│   └── Dockerfile
├── docs/
├── docker-compose.yml
└── README.md
```

### 参考资料

- [Milvus 官方文档](https://milvus.io/docs)
- [FastAPI 文档](https://fastapi.tiangolo.com)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [OpenAI API 文档](https://platform.openai.com/docs)
