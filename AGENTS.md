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

## Build & Run Commands

```bash
# Build
mvn clean install              # Full build (recommended)
mvn clean install -DskipTests  # Without tests
mvn compile                    # Compile only

# Run
mvn spring-boot:run            # Development mode
java -jar target/ai-code-mother-0.0.1-SNAPSHOT.jar
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Test Commands

```bash
mvn test                             # All tests
mvn test -Dtest=ClassName            # Single class
mvn test -Dtest=ClassName#methodName # Single method
mvn test -Dtest=*IntegrationTest     # Pattern
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

## Project Structure

```
src/main/java/com/zkf/aicodemother/
├── AiCodeMotherApplication.java  # Main entry point
├── annotation/       # Custom annotations (AuthCheck)
├── aop/              # AOP aspects (AuthInterceptor)
├── common/           # Common classes (BaseResponse, ResultUtils, PageRequest, DeleteRequest)
├── config/           # Configuration (CorsConfig, JsonConfig)
├── constant/         # Constants (UserConstant)
├── controller/       # REST controllers
├── exception/        # Exception handling (BusinessException, ErrorCode, GlobalExceptionHandler, ThrowUtils)
├── mapper/           # MyBatis mappers
├── model/
│   ├── dto/          # Data transfer objects
│   ├── entity/       # Entity classes
│   ├── enums/        # Enumerations (UserRoleEnum)
│   └── vo/           # View objects (UserVO, LoginUserVO)
└── service/          # Business logic (interface + impl)
```

## Code Style Guidelines

### Package Naming
- Base package: `com.zkf.aicodemother`
- Subpackages: `controller`, `service`, `mapper`, `model`, `config`, `common`, `exception`

### Class Naming
- **Controllers**: `{Entity}Controller` (e.g., `UserController`)
- **Services**: `{Entity}Service` / `{Entity}ServiceImpl`
- **Mappers**: `{Entity}Mapper`
- **Entities**: `{Entity}` (singular, e.g., `User`)
- **DTOs**: `{Entity}Request`, `{Entity}QueryRequest`
- **VOs**: `{Entity}VO`, `LoginUserVO`

### Import Order
```java
// 1. Java standard library
import java.util.List;
import java.time.LocalDateTime;
// 2. Jakarta/Javax
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
// 3. Spring Framework
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Service;
// 4. Third-party (Hutool, Lombok, MyBatis-Flex)
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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

### Service Pattern
```java
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. Validate parameters
        // 2. Query user
        // 3. Set session
        // 4. Return masked user info
    }
}
```

### QueryWrapper Pattern (MyBatis-Flex)
```java
QueryWrapper queryWrapper = QueryWrapper.create();
queryWrapper.eq(id != null, "id", id);
queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
queryWrapper.orderBy(StrUtil.isNotBlank(sortField), isAsc, sortField);
```

## Best Practices

- **Dependency Injection**: Use `@Resource` for field injection
- **Lombok**: Use `@Data`, `@Slf4j`, `@Builder`
- **Error Handling**: Use `ThrowUtils.throwIf()` for assertion-style checks
- **Transactions**: `@Transactional` on service methods modifying data
- **REST**: Plural nouns for resources (e.g., `/users`, `/orders`)
- **Permissions**: Use `@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)` for admin endpoints
- **Data Masking**: Return `UserVO` for public data, `User` only for admin

## API Endpoints (User Module)

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
| GET | `/user/get` | Get user by id | Admin |
| POST | `/user/list/page/vo` | List users (paginated) | Admin |

## API Documentation

- **Knife4j UI**: http://localhost:8123/api/doc.html
- **OpenAPI JSON**: http://localhost:8123/api/v3/api-docs


Essential context for agentic coding agents working in this repository.

## Project Overview

- **Type**: Spring Boot 3.5.11 REST API Application
- **Java Version**: 21
- **GroupId**: `com.zkf`
- **ArtifactId**: `ai-code-mother`
- **Base Package**: `com.zkf.aicodemother`
- **Server Port**: 8123
- **Context Path**: `/api`

## Build & Run Commands

```bash
# Build
mvn clean install              # Full build (recommended)
mvn clean install -DskipTests  # Without tests
mvn compile                    # Compile only

# Run
mvn spring-boot:run            # Development mode
java -jar target/ai-code-mother-0.0.1-SNAPSHOT.jar
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Test Commands

```bash
mvn test                             # All tests
mvn test -Dtest=ClassName            # Single class
mvn test -Dtest=ClassName#methodName # Single method
mvn test -Dtest=*IntegrationTest     # Pattern
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
## Project Structure

```
src/
├── main/
│   ├── java/com/zkf/aicodemother/
│   │   ├── AiCodeMotherApplication.java  # Main entry point
│   │   ├── controller/                   # REST controllers
│   │   ├── service/                      # Business logic
│   │   ├── repository/                   # Data access layer
│   │   ├── entity/                       # JPA entities
│   │   ├── dto/                          # Data transfer objects
│   │   ├── config/                       # Configuration classes
│   │   └── common/                       # Shared utilities
│   └── resources/
│       └── application.yml               # Application configuration
└── test/
    └── java/com/zkf/aicodemother/        # Mirror main structure
```

## Code Style Guidelines

### Package Naming
- Base package: `com.zkf.aicodemother`
- Subpackages: `controller`, `service`, `repository`, `entity`, `dto`, `config`, `common`

### Class Naming
- **Controllers**: `{Entity}Controller` (e.g., `UserController`)
- **Services**: `{Entity}Service` / `{Entity}ServiceImpl`
- **Repositories**: `{Entity}Repository`
- **Entities**: `{Entity}` (singular, e.g., `User`)
- **DTOs**: `{Entity}DTO`, `{Entity}Request`, `{Entity}Response`

### Import Order
```java
// 1. Java standard library
import java.util.List;
import java.time.LocalDateTime;
// 2. Jakarta/Javax
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
// 3. Spring Framework
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Service;
// 4. Third-party (Hutool, Lombok)
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
// 5. Project-specific
import com.zkf.aicodemother.entity.User;
```

### Controller Pattern
```java
@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/{id}")
    public Result<UserDTO> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }
}
```

### Service Pattern
```java
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    @Transactional
    public UserDTO create(UserRequest request) { /* ... */ }
}
```

### Entity Pattern
```java
@Entity
@Table(name = "user")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String username;
    
    private LocalDateTime createTime;
}
```

## Best Practices

- **Dependency Injection**: Constructor injection over `@Autowired` fields
- **Lombok**: Use `@Data`, `@Slf4j`, `@Builder`; avoid `@AllArgsConstructor` on entities
- **Error Handling**: `@RestControllerAdvice` for global exceptions
- **Transactions**: `@Transactional` on service methods modifying data
- **REST**: Plural nouns for resources (e.g., `/users`, `/orders`)

## API Documentation

- **Knife4j UI**: http://localhost:8123/api/doc.html
- **OpenAPI JSON**: http://localhost:8123/api/v3/api-docs

