# AGENTS.md - AI Code Mother Project Guide

Essential context for agentic coding agents working in this repository.

## Project Overview

- **Type**: Spring Boot 3.5.11 REST API Application
- **Java Version**: 21
- **GroupId**: `com.zkf`
- **ArtifactId**: `ai-code-mother`
- **Base Package**: `com.zkf.aicodemother`
- **Server Port**: 8123
- **Context Path**: `/api`
- **ORM**: MyBatis-Flex
- **AI Integration**: LangChain4j + Reactor (SSE Streaming)

## Build & Run Commands

```bash
# Build
mvn clean install              # Full build (recommended)
mvn clean install -DskipTests  # Without tests
mvn compile                    # Compile only

# Run
mvn spring-boot:run            # Development mode
java -jar target/ai-code-mother-0.0.1-SNAPSHOT.jar
mvn spring-boot:run -Dspring-boot.run.profiles=location

# Test
mvn test                             # All tests
mvn test -Dtest=ClassName            # Single class
mvn test -Dtest=ClassName#methodName # Single method
```

## Key Dependencies

| Dependency | Purpose |
|------------|---------|
| spring-boot-starter-web | REST API framework |
| spring-boot-devtools | Hot reload in development |
| mysql-connector-j | MySQL driver |
| lombok | Boilerplate reduction |
| hutool-all 5.8.38 | Java utility library |
| knife4j-openapi3 4.4.0 | OpenAPI/Swagger UI |
| mybatis-flex 1.11.0 | ORM framework |
| langchain4j 1.1.0 | AI integration framework |
| reactor-core | Reactive programming (SSE) |

## Project Structure

```
src/main/java/com/zkf/aicodemother/
├── AiCodeMotherApplication.java  # Main entry point
├── ai/                 # AI code generation module
├── annotation/         # Custom annotations (@AuthCheck)
├── aop/                # AOP aspects (AuthInterceptor)
├── common/             # BaseResponse, ResultUtils, PageRequest
├── config/             # CorsConfig, JsonConfig
├── constant/           # UserConstant, AppConstant
├── controller/         # REST controllers
├── core/               # Code parsers (Strategy), file savers (Template Method)
├── exception/          # ErrorCode, BusinessException, GlobalExceptionHandler
├── mapper/             # MyBatis mappers
├── model/
│   ├── dto/            # Request DTOs
│   ├── entity/         # Entity classes
│   ├── enums/          # Enumerations
│   └── vo/             # View objects
└── service/            # Business logic (interface + impl)
```

## Code Style Guidelines

### Import Order
```java
// 1. Java standard library
import java.util.List;
import java.time.LocalDateTime;
// 2. Jakarta/Javax
import jakarta.servlet.http.HttpServletRequest;
// 3. Spring Framework
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Service;
// 4. Third-party (Hutool, Lombok, MyBatis-Flex, Reactor)
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.Data;
import reactor.core.publisher.Flux;
// 5. Project-specific
import com.zkf.aicodemother.model.entity.User;
```

### Class Naming Conventions
- **Controllers**: `{Entity}Controller` (e.g., `UserController`)
- **Services**: `{Entity}Service` / `{Entity}ServiceImpl`
- **Mappers**: `{Entity}Mapper`
- **Entities**: `{Entity}` (singular, e.g., `User`, `App`)
- **DTOs**: `{Entity}Request`, `{Entity}QueryRequest`, `{Entity}AddRequest`
- **VOs**: `{Entity}VO`, `LoginUserVO`

### Controller Pattern
```java
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    // region 用户认证相关接口
    @PostMapping("login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest request, HttpServletRequest httpRequest) {
        return ResultUtils.success(userService.userLogin(request.getUserAccount(), request.getUserPassword(), httpRequest));
    }
    // endregion
}
```

### Service Pattern
```java
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // Validation using ThrowUtils
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR);
        
        // Query using MyBatis-Flex
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.getOne(queryWrapper);
        
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return BeanUtil.copyProperties(user, LoginUserVO.class);
    }
}
```

### Entity Pattern
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user")
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;
    
    private String userAccount;
    
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
```

### Error Handling Pattern
```java
// Use ThrowUtils for assertion-style validation
ThrowUtils.throwIf(condition, ErrorCode.PARAMS_ERROR);
ThrowUtils.throwIf(condition, ErrorCode.NOT_FOUND_ERROR, "自定义错误消息");

// ErrorCode enum values
SUCCESS(0, "ok")
PARAMS_ERROR(40000, "请求参数错误")
NOT_LOGIN_ERROR(40100, "未登录")
NO_AUTH_ERROR(40101, "无权限")
NOT_FOUND_ERROR(40400, "请求数据不存在")
SYSTEM_ERROR(50000, "系统内部异常")
OPERATION_ERROR(50001, "操作失败")
```

### QueryWrapper Pattern (MyBatis-Flex)
```java
QueryWrapper queryWrapper = QueryWrapper.create();
queryWrapper.eq("id", id, id != null);
queryWrapper.like("name", name, StrUtil.isNotBlank(name));
queryWrapper.orderBy(sortField, isAsc, StrUtil.isNotBlank(sortField));
```

## Best Practices

- **Dependency Injection**: Use `@Resource` for field injection
- **Lombok**: Use `@Data`, `@Slf4j`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- **Error Handling**: Use `ThrowUtils.throwIf()` for assertion-style checks
- **Transactions**: `@Transactional` on service methods modifying data
- **Permissions**: Use `@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)` for admin endpoints
- **Data Masking**: Return `UserVO`/`AppVO` for public data, entities only for admin
- **Code Organization**: Use `// region ... // endregion` comments to group related methods
- **Design Patterns**: Strategy for parsers, Template Method for savers, Facade for unified entry

## 注意事项

### 代码生成相关

1. **目录清理**
   - `CodeFileSaverTemplate.buildUniqueDir()` 在生成代码前会先删除旧目录
   - 相同 `appId` 重复生成代码时，旧文件会被完全清理，不会污染新输出
   - 目录命名格式: `{codeGenType}_{appId}` (如 `HTML_1`, `MULTI_FILE_2`)

2. **SSE 流式响应**
   - 使用 `Flux<ServerSentEvent<String>>` 返回流式数据
   - 前端通过 `EventSource` 接收，`event: done` 表示生成完成
   - 数据格式: `data: {"d":"实际内容"}`

### 常见陷阱

1. **BeanUtil.copyProperties 顺序**
   - 复制属性后设置默认值，不要再调用 `copyProperties`，否则会覆盖默认值
   - 正确: 先 copyProperties，再设置默认值
   - 错误: copyProperties → 设置默认值 → copyProperties (覆盖默认值)

2. **MyBatis-Flex 逻辑删除**
   - Entity 中使用 `@Column(value = "isDelete", isLogicDelete = true)`
   - 查询时自动过滤 `isDelete=1` 的记录
   - 物理删除需要手动处理

3. **文件路径处理**
   - 使用 `File.separator` 而非硬编码 `/` 或 `\\`
   - 使用 `StrUtil.format()` 构建路径，避免字符串拼接
   - 目录操作前检查 `FileUtil.exist()`

### AI 生成相关

1. **SSE 流式响应**
   - 使用 `Flux<ServerSentEvent<String>>` 返回流式数据
   - 前端通过 `EventSource` 接收
   - 事件类型：`session` (返回 sessionId)、`done` (生成完成)
   - 数据格式：`data: {"d":"实际内容"}`

2. **生成中断机制**
   - 每次生成创建唯一 sessionId
   - `GenerationSessionManager` 管理活跃会话
   - 前端调用 `POST /api/app/chat/stop?sessionId=xxx` 停止生成
   - 后端通过 `Disposable.dispose()` 中断 Flux 流

3. **应用删除级联清理**
   - 删除应用时自动清理关联文件
   - 清理预览目录：`tmp/code_output/{codeGenType}_{appId}/`
   - 清理部署目录：`tmp/code_deploy/{deployKey}/`

4. **环境变量配置**
   - `app.deploy.host`: 已部署应用访问域名
   - `app.preview.host`: 预览应用访问域名
   - 通过 `AppConfig` 类读取配置


## API Endpoints Summary

| Module | Key Endpoints | Auth |
|--------|---------------|------|
| User | `/user/register`, `/user/login`, `/user/logout`, `/user/get/login` | Mixed |
| App | `/app/add`, `/app/update`, `/app/delete`, `/app/deploy`, `/app/chat/gen/code` (SSE) | Owner |
| Admin | `/user/add`, `/user/delete`, `/app/admin/*` | Admin |
| Static | `/static/{deployKey}/**`, `/preview/{codeGenType_appId}/**` | None |

## Directory Structure

```
tmp/
├── code_output/           # AI generated code (preview)
│   ├── HTML_1/            # Format: codeGenType_appId
│   └── MULTI_FILE_2/
└── code_deploy/           # Deployed applications
    └── aB3xYz/           # Format: deployKey
```

## Constants

```java
// UserConstant
ADMIN_ROLE = "admin"

// AppConstant
GOOD_APP_PRIORITY = 99
DEFAULT_APP_PRIORITY = 0
CODE_OUTPUT_ROOT_DIR = user.dir/tmp/code_output
CODE_DEPLOY_ROOT_DIR = user.dir/tmp/code_deploy

// CodeGenTypeEnum
HTML("HTML")                 // Single HTML file
MULTI_FILE("MULTI_FILE")     // Multiple files
```

## API Documentation

- **Knife4j UI**: http://localhost:8123/api/doc.html
- **OpenAPI JSON**: http://localhost:8123/api/v3/api-docs

---

## 最近更新 (2026-03-08)

### 新增模块

#### 1. ChatHistory 对话历史模块
- 实体: `ChatHistory` (chat_history 表)
- 服务: `ChatHistoryService` / `ChatHistoryServiceImpl`
- 控制器: `ChatHistoryController`
- 功能: 用户消息和 AI 消息持久化存储

#### 2. AppVersion 应用版本模块
- 实体: `AppVersion` (app_version 表)
- 服务: `AppVersionService` / `AppVersionServiceImpl`
- 控制器: `AppVersionController`
- 功能: 版本自动管理、版本对比、版本回退

### 新增功能

#### 1. 对话历史管理
- 用户消息和 AI 消息自动保存到数据库
- 支持游标分页加载历史对话
- 按应用隔离对话历史
- 删除应用时级联删除对话历史

**API 端点**:
- `GET /api/chatHistory/app/{appId}` - 获取应用对话历史（游标分页）
- `POST /api/chatHistory/user/save` - 保存用户消息
- `POST /api/chatHistory/ai/save` - 保存 AI 消息
- `POST /api/chatHistory/admin/list/page` - 管理员分页查询

#### 2. 应用版本管理
- AI 每次生成自动创建版本记录
- 版本差异计算（使用 java-diff-utils）
- 版本回退功能
- 删除应用时级联删除版本记录

**API 端点**:
- `POST /api/app/version/list/page` - 获取版本列表
- `GET /api/app/version/get/detail` - 获取版本详情
- `GET /api/app/version/diff` - 对比两个版本
- `POST /api/app/version/rollback` - 回退版本

#### 3. AI 生成中断
- 每次生成返回唯一 `sessionId`
- 前端可通过 `sessionId` 停止生成
- `GenerationSessionManager` 管理活跃会话

**SSE 事件流**:
```
event: session
data: {"sessionId":"abc123"}

data: {"d":"内容"}

event: done
data:
```

**API 端点**:
- `POST /api/app/chat/stop?sessionId=xxx` - 停止 AI 生成

#### 4. 对话记忆功能
- 每个应用独立的对话上下文
- AI 能记住之前的对话内容
- 使用 Redis 存储对话记忆
- Caffeine 缓存优化 AI 服务实例

**架构**:
```
AiCodeGeneratorServiceFactory
  ├── Caffeine 缓存 (最大 1000 实例)
  │   ├── 写入后 30 分钟过期
  │   └── 访问后 10 分钟过期
  ├── MessageWindowChatMemory (每个 appId 独立)
  │   ├── id: appId
  │   ├── chatMemoryStore: RedisChatMemoryStore
  │   └── maxMessages: 20
  └── 懒加载历史对话
```

#### 5. 环境变量配置
- 部署域名可配置: `app.deploy.host`
- 预览域名可配置: `app.preview.host`
- 通过 `AppConfig` 类读取

### 数据库变更

#### 新增表

**chat_history 表**:
```sql
CREATE TABLE `chat_history` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `appId` bigint NOT NULL,
  `userId` bigint NOT NULL,
  `message` text NOT NULL,
  `messageType` varchar(32) NOT NULL,  -- user/ai
  `fileList` json,
  `parentId` bigint,  -- AI 消息关联用户消息
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `isDelete` tinyint DEFAULT 0
);
```

**app_version 表**:
```sql
CREATE TABLE `app_version` (
  `id` bigint AUTO_INCREMENT PRIMARY KEY,
  `app_id` bigint NOT NULL,
  `version_number` int NOT NULL,
  `version_name` varchar(100),
  `content` longtext NOT NULL,
  `summary` varchar(500),
  `change_type` varchar(20) NOT NULL,  -- CREATE/UPDATE/ROLLBACK
  `diff_summary` varchar(500),
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `is_current` tinyint DEFAULT 0,
  `parent_version` int
);
```

#### App 表新增字段
```sql
ALTER TABLE `app` ADD COLUMN `current_version` int DEFAULT 1;
ALTER TABLE `app` ADD COLUMN `total_versions` int DEFAULT 1;
ALTER TABLE `app` ADD COLUMN `latest_version_time` datetime;
```

### 修复的 Bug

1. **AppServiceImpl.addApp() 重复代码错误**
   - 问题：第 72 行重复的 `BeanUtil.copyProperties()` 覆盖默认值
   - 修复：删除重复代码块

2. **CodeFileSaverTemplate 目录清理缺失**
   - 问题：相同 appId 第二次生成代码时旧文件未清理
   - 修复：添加 `FileUtil.del(dirPath)` 先删除旧目录

3. **循环依赖问题**
   - 问题：AppServiceImpl ↔ AppVersionServiceImpl 循环依赖
   - 修复：AppVersionServiceImpl 使用 AppMapper 代替 AppService

### 新增依赖

```xml
<!-- 版本差异计算 -->
<dependency>
    <groupId>io.github.java-diff-utils</groupId>
    <artifactId>java-diff-utils</artifactId>
    <version>4.12</version>
</dependency>
```

### 新增枚举

```java
// MessageTypeEnum - 消息类型
USER("用户消息", "user")
AI("AI消息", "ai")

// ChangeTypeEnum - 变更类型
CREATE("创建", "CREATE")
UPDATE("更新", "UPDATE")
ROLLBACK("回退", "ROLLBACK")
```
