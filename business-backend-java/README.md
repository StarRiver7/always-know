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
| Java | - | >= 17 |
| 安全框架 | Spring Security | >= 6.0 |

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
│  │ AuthController│   │DocumentController│ │TestController││
│  │  - 登录注册  │    │  - 文档管理  │    │  - 测试接口  │     │
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

### 安全架构

```
请求到达
  ↓
┌─────────────────────────────────────────┐
│  JwtAuthenticationFilter (JWT过滤器)    │
│  - 提取 Token                           │
│  - 验证 Token 有效性                    │
│  - 从 Redis 校验 Token                  │
│  - 设置 SecurityContext                 │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  Spring Security 授权检查               │
│  - 检查接口是否需要认证                 │
│  - 未认证 → RestAuthenticationEntryPoint│
│  - 权限不足 → RestAccessDeniedHandler   │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  RoleAuthorizationAspect (角色校验)     │
│  - 检查 @RequireRole 注解               │
│  - 查询用户角色                         │
│  - 校验角色权限                         │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  Controller 执行业务逻辑                │
│  - @CurrentUserId 自动注入用户ID        │
└─────────────────────────────────────────┘
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
│   ├── annotation/                    # 自定义注解
│   │   ├── CurrentUserId.java         # 当前用户ID注解
│   │   └── RequireRole.java           # 角色权限注解
│   ├── aspect/                        # 切面
│   │   └── RoleAuthorizationAspect.java # 角色权限校验切面
│   ├── client/                        # 外部服务客户端
│   │   └── AiBackendClient.java       # AI后端服务客户端
│   ├── common/                        # 通用组件
│   │   └── RoleConstants.java         # 角色常量类
│   ├── config/                        # 配置类
│   │   ├── JwtConfig.java             # JWT配置
│   │   ├── MybatisPlusConfig.java     # MyBatis Plus配置
│   │   ├── RedisConfig.java           # Redis配置
│   │   ├── SecurityConfig.java        # 安全配置
│   │   ├── ThreadPoolConfig.java      # 线程池配置
│   │   └── WebConfig.java             # Web配置（跨域、参数解析器）
│   ├── controller/                    # REST控制器
│   │   ├── AuthController.java        # 认证接口
│   │   ├── DocumentController.java    # 文档接口
│   │   ├── HealthController.java      # 健康检查接口
│   │   └── TestController.java        # 测试接口
│   ├── dto/                           # 数据传输对象
│   │   ├── request/                   # 请求DTO
│   │   │   ├── LoginRequest.java      # 登录请求
│   │   │   ├── RegisterRequest.java   # 注册请求
│   │   │   └── ChatRequest.java       # 聊天请求
│   │   └── response/                  # 响应DTO
│   │       ├── Result.java            # 统一响应
│   │       ├── ResultCode.java        # 状态码常量
│   │       └── UserInfoResponse.java  # 用户信息响应
│   ├── entity/                        # 数据库实体
│   │   ├── ChatHistory.java           # 对话历史
│   │   ├── Document.java              # 文档
│   │   ├── DocumentPermission.java    # 文档权限
│   │   ├── Role.java                  # 角色
│   │   ├── User.java                  # 用户
│   │   └── UserRole.java              # 用户角色关联
│   ├── exception/                     # 异常处理
│   │   └── GlobalExceptionHandler.java # 全局异常处理器
│   ├── filter/                        # 过滤器
│   │   └── JwtAuthenticationFilter.java # JWT认证过滤器
│   ├── mapper/                        # MyBatis Mapper接口
│   │   ├── ChatHistoryMapper.java
│   │   ├── DocumentMapper.java
│   │   ├── DocumentPermissionMapper.java
│   │   ├── RoleMapper.java
│   │   ├── UserMapper.java
│   │   └── UserRoleMapper.java
│   ├── resolver/                      # 参数解析器
│   │   └── CurrentUserIdArgumentResolver.java # 用户ID解析器
│   ├── security/                      # 安全组件
│   │   ├── RestAuthenticationEntryPoint.java  # 认证入口点
│   │   └── RestAccessDeniedHandler.java       # 授权异常处理
│   ├── service/                       # 业务服务
│   │   ├── DocumentService.java       # 文档服务
│   │   ├── TokenService.java          # Token服务
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

#### SecurityConfig.java

Spring Security 安全配置，管理认证和授权。

**核心配置：**
- JWT 过滤器集成
- 认证入口点（401 处理）
- 授权异常处理（403 处理）
- 无状态 Session 管理
- CORS 跨域配置

#### WebConfig.java

Web配置类，注册跨域配置和自定义参数解析器。

#### JwtConfig.java

JWT配置类，管理JWT密钥和过期时间。

| 配置项 | 说明 |
|--------|------|
| secret | JWT签名密钥（至少256位） |
| expiration | Token过期时间（毫秒） |

#### ThreadPoolConfig.java

线程池配置，用于异步文档处理任务。

### 3. 过滤器 (JwtAuthenticationFilter.java)

JWT认证过滤器，集成到 Spring Security 过滤链中。

**验证流程：**
1. 从请求头获取 `Authorization`
2. 检查Token格式是否为 `Bearer ` 开头
3. 验证Token是否过期
4. 从 Redis 校验Token有效性
5. 解析Token获取用户信息
6. 设置 Spring Security 上下文
7. 将用户ID存入请求属性（供 @CurrentUserId 使用）

### 4. 安全组件

#### RestAuthenticationEntryPoint.java

认证入口点，处理未登录或 Token 无效的情况。

**响应示例：**
```json
{
    "code": 401,
    "message": "未登录或 Token 无效，请先登录",
    "data": null,
    "timestamp": 1715529600000
}
```

#### RestAccessDeniedHandler.java

授权异常处理，处理权限不足的情况。

**响应示例：**
```json
{
    "code": 403,
    "message": "权限不足，无法访问该资源",
    "data": null,
    "timestamp": 1715529600000
}
```

### 5. 全局异常处理 (GlobalExceptionHandler.java)

统一处理系统中所有未捕获的异常。

**处理的异常类型：**

| 异常类型 | HTTP状态码 | 说明 |
|---------|-----------|------|
| MethodArgumentNotValidException | 400 | @Valid 参数校验失败 |
| ConstraintViolationException | 400 | 单个参数校验失败 |
| BadCredentialsException | 401 | 用户名或密码错误 |
| NoResourceFoundException | 404 | 资源不存在 |
| RuntimeException | 500 | 业务异常 |
| Exception | 500 | 未知异常 |

### 6. 自定义注解

#### @CurrentUserId

自动注入当前登录用户ID到Controller方法参数。

**使用示例：**
```java
@GetMapping("/documents")
public Result listDocuments(@CurrentUserId Long userId) {
    // userId 自动注入，无需手动从 request 获取
}
```

#### @RequireRole

角色权限校验注解，限制接口只能被特定角色访问。

**使用示例：**
```java
// 只有管理员能访问
@RequireRole(RoleConstants.ADMIN)
@PostMapping("/upload")
public Result upload() { ... }

// 管理员和普通用户都能访问
@RequireRole({RoleConstants.ADMIN, RoleConstants.USER})
@GetMapping("/documents")
public Result list() { ... }
```

**角色说明：**
- `1` - 管理员（ADMIN）
- `2` - 普通用户（USER）

### 7. 参数解析器 (CurrentUserIdArgumentResolver.java)

解析 @CurrentUserId 注解，自动从 request 中获取 userId 并注入到方法参数。

### 8. JWT工具类 (JwtUtil.java)

提供JWT的生成、解析和验证功能。

| 方法名 | 功能 | 参数 | 返回值 |
|--------|------|------|--------|
| generateToken | 生成Token | userId, username | String |
| parseToken | 解析Token | token | Claims |
| getUserIdFromToken | 从Token获取用户ID | token | Long |
| getUsernameFromToken | 从Token获取用户名 | token | String |
| isTokenExpired | 检查Token是否过期 | token | boolean |

### 9. 服务层

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

### 10. AI后端客户端 (AiBackendClient.java)

封装与Python AI后端服务的HTTP调用。

| 方法名 | 功能 | 调用路径 |
|--------|------|----------|
| processDocument | 处理文档 | POST /api/v1/documents/process |
| chat | 问答查询 | POST /api/v1/chat/query |
| deleteDocument | 删除文档向量 | DELETE /api/v1/documents/{id} |

### 11. 统一响应封装 (Result.java)

标准化API响应格式。

**响应结构：**

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码（使用 ResultCode 常量） |
| message | String | 响应消息 |
| data | T | 响应数据（泛型） |
| timestamp | long | 时间戳 |

**状态码常量 (ResultCode.java)：**

| 状态码 | 常量名 | 说明 |
|--------|--------|------|
| 200 | SUCCESS | 成功 |
| 400 | BAD_REQUEST | 请求参数错误 |
| 401 | UNAUTHORIZED | 未授权 |
| 403 | FORBIDDEN | 禁止访问 |
| 404 | NOT_FOUND | 资源不存在 |
| 500 | INTERNAL_SERVER_ERROR | 服务器内部错误 |
| 1000 | BUSINESS_ERROR | 业务逻辑错误 |
| 1001 | LOGIN_FAILED | 登录失败 |
| 1002 | USER_ALREADY_EXISTS | 用户已存在 |

### 8. 角色权限控制

#### 权限模型

系统采用基于角色的访问控制（RBAC）：

```
用户 (User)
  ↓ 多对多
用户角色关联 (UserRole)
  ↓ 多对一
角色 (Role)
```

#### 角色类型

| 角色ID | 角色名称 | 说明 |
|--------|---------|------|
| 1 | 管理员 (ADMIN) | 拥有所有权限 |
| 2 | 普通用户 (USER) | 拥有基础权限 |

#### 接口权限清单

| 接口 | 路径 | 需要认证 | 需要角色 | 说明 |
|------|------|---------|---------|------|
| 登录 | POST /api/auth/login | ❌ | - | 公开接口 |
| 注册 | POST /api/auth/register | ❌ | - | 公开接口 |
| 健康检查 | GET /api/health | ❌ | - | 公开接口 |
| 获取文档列表 | GET /api/documents | ✅ | 1, 2 | 登录用户 |
| 上传文档 | POST /api/documents/upload | ✅ | 1, 2 | 登录用户 |
| 删除文档 | DELETE /api/documents/{id} | ✅ | 1, 2 | 登录用户 |
| AI聊天 | POST /api/documents/chat | ✅ | 1, 2 | 登录用户 |
| 测试认证 | GET /api/test/auth-required | ✅ | - | 登录用户 |

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
    "code": 1001,
    "message": "用户名或密码错误",
    "data": null,
    "timestamp": 1715529600000
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

### 3. 用户退出登录

**请求：**
```
POST /api/auth/logout
Authorization: Bearer <token>
```

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": null,
    "timestamp": 1715529600000
}
```

### 4. 获取文档列表

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

### 5. 上传文档

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

### 6. 删除文档

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

### 7. 知识问答

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

### 8. 健康检查

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

### 9. 测试认证接口

**请求：**
```
GET /api/test/auth-required
Authorization: Bearer <token>
```

**成功响应 (200)：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "message": "认证成功！",
        "userId": 1,
        "timestamp": 1715529600000
    },
    "timestamp": 1715529600000
}
```

**失败响应 (401)：**
```json
{
    "code": 401,
    "message": "未登录或 Token 无效，请先登录",
    "data": null,
    "timestamp": 1715529600000
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

创建数据库和表：

```sql
CREATE DATABASE IF NOT EXISTS rag_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE rag_db;

-- 用户表
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 角色表
CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_user_role (user_id, role_id, deleted)
);

-- 插入默认角色
INSERT INTO sys_role (id, role_name, role_code) VALUES 
(1, '管理员', 'ADMIN'),
(2, '普通用户', 'USER');

-- 创建默认管理员用户（密码为BCrypt加密的123456）
INSERT INTO sys_user (username, password, real_name, status) 
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '管理员', 1);

-- 给管理员分配角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
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

# 测试认证
curl -X GET "http://localhost:8080/api/test/auth-required" \
  -H "Authorization: Bearer <token>"

# 退出登录
curl -X POST "http://localhost:8080/api/auth/logout" \
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
2. **JWT安全**：密钥要保密，不要硬编码在代码中，使用配置文件管理
3. **认证机制**：所有敏感接口都需要Token验证，集成 Spring Security
4. **角色权限**：使用 @RequireRole 注解实现细粒度权限控制
5. **文件上传**：限制文件类型和大小，防止恶意文件上传
6. **SQL注入**：使用MyBatis Plus的参数化查询，防止SQL注入
7. **日志安全**：不要在日志中记录敏感信息（如密码、Token）
8. **异常处理**：使用全局异常处理器，统一错误响应格式
9. **参数校验**：使用 @Valid 和校验注解，提前拦截无效请求
10. **Token管理**：Token存储在Redis中，支持踢人下线、单点登录等功能

## 核心特性

### 安全特性

- ✅ **JWT Token 认证**：基于 Token 的无状态认证
- ✅ **Redis Token 管理**：支持踢人下线、单点登录
- ✅ **Spring Security 集成**：完整的认证和授权机制
- ✅ **角色权限控制**：基于 RBAC 的细粒度权限管理
- ✅ **全局异常处理**：统一错误响应格式
- ✅ **参数校验**：使用 @Valid 提前拦截无效请求

### 架构特性

- ✅ **自定义注解**：@CurrentUserId 自动注入用户ID
- ✅ **参数解析器**：优雅的用户身份获取方式
- ✅ **AOP 切面**：声明式权限校验
- ✅ **DTO 分层**：请求和响应对象分离
- ✅ **状态码管理**：统一的 ResultCode 常量
- ✅ **异步处理**：文档异步处理，提高性能

### 代码质量

- ✅ **企业级规范**：完整的 JavaDoc 注释
- ✅ **分层架构**：Controller → Service → Mapper
- ✅ **依赖注入**：构造器注入，提高可测试性
- ✅ **统一响应**：标准化的 API 响应格式
- ✅ **日志记录**：详细的操作日志和错误日志

## 扩展功能建议

1. **Token刷新机制**：实现 refresh token 延长登录有效期
2. **文档版本管理**：支持文档的版本控制和历史记录
3. **消息通知**：文档处理完成后通知用户
4. **性能监控**：添加 Prometheus 和 Grafana 监控
5. **分布式部署**：支持多实例部署和负载均衡
6. **接口限流**：防止恶意刷接口
7. **操作日志**：记录用户操作历史
8. **数据权限**：实现更细粒度的数据访问控制

## 许可证

MIT License