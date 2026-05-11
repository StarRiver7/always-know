# RAG 业务后端服务 - Java

## 项目概述

本项目是一个基于 Spring Boot 的业务后端服务，提供用户认证、文档管理和知识问答功能。作为 RAG 系统的业务层，它负责处理用户请求、管理文档权限，并与 AI 后端服务进行交互。

## 技术栈

| 组件 | 技术 | 版本要求 |
|------|------|----------|
| 框架 | Spring Boot | >= 3.2.0 |
| 数据库 | MySQL | >= 8.0 |
| 缓存 | Redis | >= 7.0 |
| ORM | MyBatis Plus | >= 3.5.0 |
| JWT | jjwt | >= 0.12.0 |
| Java | - | >= 21 |

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 / 客户端                          │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP/REST
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot 业务层                        │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │ AuthController│   │DocumentController│ │HealthController││
│  │  - 登录注册  │    │  - 文档管理  │    │  - 健康检查  │     │
│  └──────┬──────┘    └──────┬──────┘    └─────────────┘     │
│         │                  │                                │
│         ▼                  ▼                                │
│  ┌─────────────┐    ┌─────────────┐                        │
│  │ UserService │    │DocumentService│                      │
│  │  - 用户认证  │    │  - 文档处理  │                        │
│  └──────┬──────┘    └──────┬──────┘                        │
│         │                  │                                │
│         ▼                  ▼                                │
│  ┌─────────────────────────────────────────┐                │
│  │              数据库层                    │                │
│  │  MySQL: 用户、文档、权限、历史记录        │                │
│  │  Redis: Token缓存                       │                │
│  └─────────────────────────────────────────┘                │
│                   │                                         │
│                   ▼                                         │
│  ┌─────────────────────────────────────────┐                │
│  │         AI Backend Client               │                │
│  │  - 调用Python AI服务进行文档处理和问答   │                │
│  └─────────────────────────────────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

### 核心流程图

#### 用户登录流程

```
客户端请求 → AuthController → UserService → 验证用户 → 生成JWT → 返回token
```

#### 文档上传流程

```
客户端上传 → DocumentController → DocumentService → 保存文件 → 保存数据库 → 异步处理 → AI后端
```

#### 知识问答流程

```
用户提问 → DocumentController → DocumentService → 获取权限文档 → AI后端查询 → 返回结果
```

## 目录结构

```
business-backend-java/
├── src/main/java/com/rag/business/
│   ├── RagBusinessApplication.java    # Spring Boot 启动类
│   ├── client/                        # 外部服务客户端
│   │   └── AiBackendClient.java       # AI后端服务客户端
│   ├── common/                        # 通用组件
│   │   └── Result.java                # 统一响应封装
│   ├── config/                        # 配置类
│   │   ├── JwtConfig.java             # JWT配置
│   │   ├── MybatisPlusConfig.java     # MyBatis Plus配置
│   │   ├── SecurityConfig.java        # 安全配置
│   │   ├── ThreadPoolConfig.java      # 线程池配置
│   │   └── WebConfig.java             # Web配置（跨域、拦截器）
│   ├── controller/                    # REST控制器
│   │   ├── AuthController.java        # 认证接口
│   │   ├── DocumentController.java    # 文档接口
│   │   └── HealthController.java      # 健康检查接口
│   ├── entity/                        # 数据库实体
│   │   ├── ChatHistory.java           # 对话历史
│   │   ├── Document.java              # 文档
│   │   ├── DocumentPermission.java    # 文档权限
│   │   ├── Role.java                  # 角色
│   │   ├── User.java                  # 用户
│   │   └── UserRole.java              # 用户角色关联
│   ├── interceptor/                   # 拦截器
│   │   └── AuthInterceptor.java       # JWT认证拦截器
│   ├── mapper/                        # MyBatis Mapper接口
│   │   ├── ChatHistoryMapper.java
│   │   ├── DocumentMapper.java
│   │   ├── DocumentPermissionMapper.java
│   │   ├── RoleMapper.java
│   │   ├── UserMapper.java
│   │   └── UserRoleMapper.java
│   ├── service/                       # 业务服务
│   │   ├── DocumentService.java       # 文档服务
│   │   └── UserService.java           # 用户服务
│   └── util/                          # 工具类
│       └── JwtUtil.java               # JWT工具类
├── src/main/resources/
│   ├── mapper/                        # MyBatis XML映射文件
│   │   └── DocumentMapper.xml
│   └── application.yml                # 应用配置
├── uploads/                           # 文件上传目录（运行时创建）
└── pom.xml                            # Maven配置
```

## 核心模块详解

### 1. 启动类 (RagBusinessApplication.java)

Spring Boot 应用的入口类，负责启动整个应用。

**关键注解：**
- `@SpringBootApplication`: 启用Spring Boot自动配置
- `@EnableAsync`: 启用异步处理
- `@MapperScan`: 指定Mapper接口扫描路径

### 2. 配置类

#### JwtConfig.java

JWT配置类，管理JWT密钥和过期时间。

| 配置项 | 说明 |
|--------|------|
| secret | JWT签名密钥（至少256位） |
| expiration | Token过期时间（毫秒） |

#### WebConfig.java

Web配置类，注册跨域配置和认证拦截器。

#### ThreadPoolConfig.java

线程池配置，用于异步文档处理任务。

### 3. 拦截器 (AuthInterceptor.java)

JWT认证拦截器，对需要认证的接口进行Token验证。

**验证流程：**
1. 从请求头获取 `Authorization`
2. 检查Token格式是否为 `Bearer ` 开头
3. 验证Token是否过期
4. 解析Token获取用户信息
5. 将用户ID存入请求属性

### 4. JWT工具类 (JwtUtil.java)

提供JWT的生成、解析和验证功能。

| 方法名 | 功能 | 参数 | 返回值 |
|--------|------|------|--------|
| generateToken | 生成Token | userId, username | String |
| parseToken | 解析Token | token | Claims |
| getUserIdFromToken | 从Token获取用户ID | token | Long |
| getUsernameFromToken | 从Token获取用户名 | token | String |
| isTokenExpired | 检查Token是否过期 | token | boolean |

### 5. 服务层

#### UserService.java

用户服务，提供登录和注册功能。

| 方法名 | 功能 | 参数 | 返回值 |
|--------|------|------|--------|
| login | 用户登录 | username, password | Map (token, user) |
| register | 用户注册 | username, password, realName, email, phone | User |

#### DocumentService.java

文档服务，提供文档管理和问答功能。

| 方法名 | 功能 | 参数 | 返回值 |
|--------|------|------|--------|
| listDocuments | 获取用户文档列表 | userId, pageNum, pageSize | Page\<Document\> |
| uploadDocument | 上传文档 | userId, title, file | Document |
| processDocumentAsync | 异步处理文档 | documentId, filePath, fileName | void |
| chat | 知识问答 | userId, query, docIds | Map |
| deleteDocument | 删除文档 | documentId, userId | void |

### 6. AI后端客户端 (AiBackendClient.java)

封装与Python AI后端服务的HTTP调用。

| 方法名 | 功能 | 调用路径 |
|--------|------|----------|
| processDocument | 处理文档 | POST /api/v1/documents/process |
| chat | 问答查询 | POST /api/v1/chat/query |
| deleteDocument | 删除文档向量 | DELETE /api/v1/documents/{id} |

### 7. 统一响应封装 (Result.java)

标准化API响应格式。

**响应结构：**

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码（200成功，其他为错误） |
| message | String | 响应消息 |
| data | T | 响应数据（泛型） |

## API接口详细说明

### 1. 用户登录

**请求：**
```
POST /api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "123456"
}
```

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "user": {
            "id": 1,
            "username": "admin",
            "realName": "管理员",
            "email": "admin@example.com",
            "phone": "13800138000",
            "status": 1
        }
    }
}
```

**失败响应：**
```json
{
    "code": 500,
    "message": "用户名或密码错误",
    "data": null
}
```

### 2. 用户注册

**请求：**
```
POST /api/auth/register
Content-Type: application/json

{
    "username": "user",
    "password": "123456",
    "realName": "用户",
    "email": "user@example.com",
    "phone": "13900139000"
}
```

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |
| realName | String | 是 | 真实姓名 |
| email | String | 否 | 邮箱 |
| phone | String | 否 | 手机号 |

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 2,
        "username": "user",
        "realName": "用户",
        "email": "user@example.com",
        "phone": "13900139000",
        "status": 1
    }
}
```

### 3. 获取文档列表

**请求：**
```
GET /api/documents?pageNum=1&pageSize=10
Authorization: Bearer <token>
```

**请求参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页数量 |

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "records": [
            {
                "id": 1,
                "title": "企业员工手册",
                "fileName": "员工手册.pdf",
                "fileSize": 102400,
                "fileType": ".pdf",
                "status": 1,
                "chunksProcessed": 15,
                "createTime": "2024-01-01 10:00:00"
            }
        ],
        "total": 10,
        "size": 10,
        "current": 1
    }
}
```

### 4. 上传文档

**请求：**
```
POST /api/documents/upload?title=文档标题
Content-Type: multipart/form-data
Authorization: Bearer <token>

file: <文件二进制>
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 文档标题 |
| file | File | 是 | 上传的文件 |

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 1,
        "title": "文档标题",
        "fileName": "example.pdf",
        "fileSize": 102400,
        "fileType": ".pdf",
        "status": 0,
        "chunksProcessed": 0,
        "createTime": "2024-01-01 10:00:00"
    }
}
```

**状态说明：**
- 0: 处理中
- 1: 处理成功
- -1: 处理失败

### 5. 删除文档

**请求：**
```
DELETE /api/documents/{id}
Authorization: Bearer <token>
```

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文档ID |

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": null
}
```

### 6. 知识问答

**请求：**
```
POST /api/documents/chat
Content-Type: application/json
Authorization: Bearer <token>

{
    "query": "什么是RAG？",
    "documentIds": [1, 2, 3]
}
```

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| query | String | 是 | 用户提问 |
| documentIds | List\<Long\> | 否 | 指定文档ID，为空则使用用户有权限的所有文档 |

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "answer": "RAG（Retrieval-Augmented Generation）是一种结合检索和生成的AI技术...",
        "sources": [
            {
                "document_id": 1,
                "chunk_id": 3,
                "content": "RAG技术通过检索知识库中的相关信息...",
                "score": 0.85
            }
        ]
    }
}
```

### 7. 健康检查

**请求：**
```
GET /api/health
```

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "status": "UP",
        "timestamp": "2024-01-01T10:00:00"
    }
}
```

## 数据库设计

### 用户表 (user)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(255) | NOT NULL | 加密后的密码 |
| real_name | VARCHAR(50) | - | 真实姓名 |
| email | VARCHAR(100) | - | 邮箱 |
| phone | VARCHAR(20) | - | 手机号 |
| status | TINYINT | DEFAULT 1 | 状态（0禁用，1启用） |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 文档表 (document)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 文档ID |
| title | VARCHAR(200) | NOT NULL | 文档标题 |
| file_name | VARCHAR(200) | NOT NULL | 原始文件名 |
| file_path | VARCHAR(500) | NOT NULL | 文件存储路径 |
| file_size | BIGINT | NOT NULL | 文件大小（字节） |
| file_type | VARCHAR(20) | - | 文件类型 |
| user_id | BIGINT | FOREIGN KEY | 上传用户ID |
| status | TINYINT | DEFAULT 0 | 状态（0处理中，1成功，-1失败） |
| chunks_processed | INT | DEFAULT 0 | 处理的块数 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 文档权限表 (document_permission)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 权限ID |
| document_id | BIGINT | FOREIGN KEY | 文档ID |
| user_id | BIGINT | FOREIGN KEY | 用户ID |
| permission_type | TINYINT | DEFAULT 1 | 权限类型（1只读，2编辑） |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

### 对话历史表 (chat_history)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | FOREIGN KEY | 用户ID |
| query | TEXT | NOT NULL | 用户提问 |
| answer | TEXT | - | AI回答 |
| source_documents | TEXT | - | 来源文档 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

## 配置与部署

### Maven依赖

```xml
<dependencies>
    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- MyBatis Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.5</version>
    </dependency>
    
    <!-- MySQL Driver -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### application.yml 配置说明

```yaml
server:
  port: 8080                    # 服务端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rag_db # 数据库连接
    username: root               # 数据库用户名
    password: 123456            # 数据库密码
  
  data:
    redis:
      host: localhost            # Redis地址
      port: 6379                 # Redis端口
      database: 1                # Redis数据库索引

jwt:
  secret: your-secret-key-at-least-256-bits-long # JWT密钥
  expiration: 86400000          # Token过期时间（毫秒）

ai:
  backend:
    url: http://localhost:8000   # AI后端服务地址

file:
  upload:
    path: ./uploads              # 文件上传目录
```

### 数据库初始化

创建数据库和用户表：

```sql
CREATE DATABASE IF NOT EXISTS rag_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE rag_db;

CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP
);

-- 创建默认管理员用户（密码为BCrypt加密的123456）
INSERT INTO user (username, password, real_name, status) 
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '管理员', 1);
```

### 启动服务

**开发环境：**
```bash
cd business-backend-java
mvn spring-boot:run
```

**生产环境：**
```bash
mvn clean package
java -jar target/rag-business-backend-1.0.0.jar
```

## 使用示例

### cURL示例

```bash
# 登录
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "123456"}'

# 获取文档列表（替换<token>）
curl -X GET "http://localhost:8080/api/documents?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"

# 上传文档
curl -X POST "http://localhost:8080/api/documents/upload?title=测试文档" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@test.pdf"

# 知识问答
curl -X POST "http://localhost:8080/api/documents/chat" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"query": "什么是RAG？"}'

# 删除文档
curl -X DELETE "http://localhost:8080/api/documents/1" \
  -H "Authorization: Bearer <token>"
```

### Java客户端示例

```java
import org.springframework.web.client.RestTemplate;

public class RagClient {
    private static final String BASE_URL = "http://localhost:8080";
    private final RestTemplate restTemplate = new RestTemplate();
    private String token;

    public void login(String username, String password) {
        var request = new LoginRequest(username, password);
        var response = restTemplate.postForObject(
            BASE_URL + "/api/auth/login", 
            request, 
            Result.class
        );
        this.token = (String) response.getData().get("token");
    }

    public Result chat(String query) {
        var headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        var request = new ChatRequest(query, null);
        var entity = new HttpEntity<>(request, headers);
        
        return restTemplate.exchange(
            BASE_URL + "/api/documents/chat",
            HttpMethod.POST,
            entity,
            Result.class
        ).getBody();
    }
}
```

## 常见问题

### 1. JWT密钥长度不足

**错误信息：**
```
Invalid key length: 32 bytes
```

**解决方案：**
- JWT密钥至少需要256位（32字节）
- 在 `application.yml` 中设置更长的密钥：
```yaml
jwt:
  secret: this-is-a-very-long-secret-key-at-least-256-bits-long-for-hs256-algorithm
```

### 2. 数据库连接失败

**错误信息：**
```
Cannot get connection from pool
```

**解决方案：**
1. 确认MySQL服务正在运行
2. 检查数据库连接配置是否正确
3. 确认数据库用户权限

### 3. 文件上传失败

**错误信息：**
```
Max upload size exceeded
```

**解决方案：**
- 在 `application.yml` 中增加文件大小限制：
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

### 4. Token过期

**错误信息：**
```
token已过期
```

**解决方案：**
- 用户需要重新登录获取新的Token
- 可以考虑实现Token刷新机制

## 安全注意事项

1. **密码安全**：使用BCrypt加密存储密码，禁止明文存储
2. **JWT安全**：密钥要保密，不要硬编码在代码中
3. **权限控制**：所有敏感接口都需要Token验证
4. **文件上传**：限制文件类型和大小，防止恶意文件上传
5. **SQL注入**：使用MyBatis Plus的参数化查询，防止SQL注入
6. **日志安全**：不要在日志中记录敏感信息（如密码、Token）

## 扩展功能建议

1. **Token刷新机制**：实现refresh token延长登录有效期
2. **角色权限系统**：支持多种角色和细粒度权限控制
3. **文档版本管理**：支持文档的版本控制和历史记录
4. **消息通知**：文档处理完成后通知用户
5. **性能监控**：添加Prometheus和Grafana监控
6. **分布式部署**：支持多实例部署和负载均衡

## 许可证

MIT License