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
```

## Test Commands

```bash
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
| mybatis-flex | ORM framework |
| langchain4j | AI integration framework |
| reactor-core | Reactive programming (SSE) |

## Project Structure

```
src/main/java/com/zkf/aicodemother/
├── AiCodeMotherApplication.java  # Main entry point
├── ai/                 # AI code generation module
│   ├── model/          # AI result models (HtmlCodeResult, MultiFileCodeResult)
│   └── AiCodeGeneratorService.java
├── annotation/         # Custom annotations (AuthCheck)
├── aop/                # AOP aspects (AuthInterceptor)
├── common/             # Common classes (BaseResponse, ResultUtils, PageRequest)
├── config/             # Configuration (CorsConfig, JsonConfig, AiCodeGeneratorServiceFactory)
├── constant/           # Constants (UserConstant, AppConstant)
├── controller/         # REST controllers (UserController, AppController, StaticResourceController)
├── core/               # Core business module
│   ├── parser/         # Code parsers (Strategy Pattern)
│   ├── saver/          # File savers (Template Method Pattern)
│   ├── CodeGenTypeEnum.java
│   └── AiCodeGeneratorFacade.java
├── exception/          # Exception handling
├── mapper/             # MyBatis mappers
├── model/
│   ├── dto/            # Data transfer objects (AppAddRequest, AppDeployRequest, etc.)
│   ├── entity/         # Entity classes (User, App)
│   ├── enums/          # Enumerations (UserRoleEnum)
│   └── vo/             # View objects (UserVO, AppVO)
└── service/            # Business logic (interface + impl)
```

## Code Style Guidelines

### Package Naming
- Base package: `com.zkf.aicodemother`
- Subpackages: `controller`, `service`, `mapper`, `model`, `config`, `common`, `exception`, `core`, `ai`

### Class Naming
- **Controllers**: `{Entity}Controller` (e.g., `UserController`)
- **Services**: `{Entity}Service` / `{Entity}ServiceImpl`
- **Mappers**: `{Entity}Mapper`
- **Entities**: `{Entity}` (singular, e.g., `User`, `App`)
- **DTOs**: `{Entity}Request`, `{Entity}QueryRequest`, `{Entity}DeployRequest`
- **VOs**: `{Entity}VO`, `LoginUserVO`
- **Parsers**: `{Type}CodeParser` (e.g., `HtmlCodeParser`)
- **Savers**: `{Type}CodeFileSaverTemplate`

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

### Controller Pattern
```java
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping("login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest request, HttpServletRequest httpRequest) {
        return ResultUtils.success(userService.userLogin(request.getUserAccount(), request.getUserPassword(), httpRequest));
    }
}
```

### QueryWrapper Pattern (MyBatis-Flex)
```java
QueryWrapper queryWrapper = QueryWrapper.create();
queryWrapper.eq("id", id, id != null);
queryWrapper.like("name", name, StrUtil.isNotBlank(name));
queryWrapper.orderBy(sortField, isAsc, StrUtil.isNotBlank(sortField));
```

### AI Service Pattern (LangChain4j)
```java
public interface AiCodeGeneratorService {
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);
    
    Flux<String> generateHtmlCodeStream(String userMessage);
}
```

## Best Practices

- **Dependency Injection**: Use `@Resource` for field injection
- **Lombok**: Use `@Data`, `@Slf4j`, `@Builder`
- **Error Handling**: Use `ThrowUtils.throwIf()` for assertion-style checks
- **Transactions**: `@Transactional` on service methods modifying data
- **REST**: Plural nouns for resources (e.g., `/users`, `/orders`)
- **Permissions**: Use `@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)` for admin endpoints
- **Data Masking**: Return `UserVO`/`AppVO` for public data, `User`/`App` only for admin
- **Design Patterns**: Use Strategy for parsers, Template Method for savers, Facade for unified entry

## API Endpoints

### User Module
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/user/register` | User registration | None |
| POST | `/user/login` | User login | None |
| POST | `/user/logout` | User logout | Login required |
| GET | `/user/get/login` | Get current user | Login required |
| GET | `/user/get/vo/{id}` | Get user by id (masked) | None |
| POST | `/user/add` | Create user | Admin |
| POST | `/user/delete` | Delete user | Admin |
| POST | `/user/update` | Update user | Admin |
| POST | `/user/list/page/vo` | List users (paginated) | Admin |

### App Module
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/app/add` | Create application | Login required |
| POST | `/app/update` | Update application (owner) | Owner |
| POST | `/app/delete` | Delete application (owner or admin) | Owner/Admin |
| GET | `/app/get/vo` | Get application with user info | None |
| POST | `/app/my/list/page/vo` | List my applications (paginated) | Login required |
| POST | `/app/good/list/page/vo` | List featured applications | None |
| GET | `/app/chat/gen/code` | AI code generation (SSE stream) | Owner |
| POST | `/app/deploy` | Deploy application | Owner |

### Admin Endpoints
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/app/admin/delete` | Delete any application | Admin |
| POST | `/app/admin/update` | Update application (limited fields) | Admin |
| POST | `/app/admin/list/page/vo` | List all applications (paginated) | Admin |
| GET | `/app/admin/get/vo` | Get application details | Admin |

### Static Resources
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/static/{deployKey}/**` | Serve deployed application files | None |

## Directory Structure

```
tmp/
├── code_output/           # AI generated code (preview)
│   ├── HTML_1/            # Format: codeGenType_appId
│   └── MULTI_FILE_2/
└── code_deploy/           # Deployed applications
    ├── aB3xYz/           # Format: deployKey
    └── xY7zAb/
```

## Constants

```java
// UserConstant
ADMIN_ROLE = "admin"

// AppConstant
GOOD_APP_PRIORITY = 99       // Featured application priority
DEFAULT_APP_PRIORITY = 0     // Default priority
CODE_OUTPUT_ROOT_DIR = user.dir/tmp/code_output
CODE_DEPLOY_ROOT_DIR = user.dir/tmp/code_deploy
CODE_DEPLOY_HOST = http://localhost

// CodeGenTypeEnum
HTML("HTML")                 // Single HTML file
MULTI_FILE("MULTI_FILE")     // Multiple files
```

## API Documentation

- **Knife4j UI**: http://localhost:8123/api/doc.html
- **OpenAPI JSON**: http://localhost:8123/api/v3/api-docs