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