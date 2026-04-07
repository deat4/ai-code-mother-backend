# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

```bash
# Build
mvn clean install

# Run application
mvn spring-boot:run

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Run specific test method
mvn test -Dtest=ClassName#methodName
```

## Architecture Overview

**AI Code Mother Backend** - An intelligent code generation platform using Spring Boot 3.5.11 + Java 21 + MyBatis-Flex + LangChain4j.

### Core Design Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| Facade | `AiCodeGeneratorFacade` | Unified entry point for code generation |
| Strategy | `CodeParser<T>` interface | Different parsing strategies for HTML/MultiFile |
| Template Method | `CodeFileSaverTemplate<T>` | Standardized file saving workflow |
| Factory | `AiCodeGeneratorServiceFactory` | AI service creation with Caffeine cache |

### Key Package Structure

```
com.zkf.aicodemother
├── ai/                    # LangChain4j AI services
│   ├── AiCodeGeneratorService   # HTML/MultiFile generation interface
│   ├── AiCodeCreator            # Vue project creation (reasoning model + tools)
│   ├── AiCodeModifier           # Vue project modification (full tool set)
│   ├── model/message/           # Stream message types for VUE_PROJECT
│   └── tools/                   # AI tool implementations
├── core/
│   ├── parser/            # Code parsers (HtmlCodeParser, MultiFileCodeParser)
│   ├── saver/             # File savers with template method pattern
│   ├── handler/           # Stream handlers (JsonMessageStreamHandler for VUE_PROJECT)
│   ├── builder/           # VueProjectBuilder for npm builds
│   └── GenerationSessionManager  # SSE session management
├── config/                # Spring configurations
│   ├── AiCodeGeneratorServiceFactory  # AI service factory with caching
│   ├── AppConfig                       # Deploy/preview host config
│   └── ReasoningStreamingChatModelConfig # DeepSeek reasoning model
├── service/               # Business logic layer
├── mapper/                # MyBatis-Flex mappers
└── model/
    ├── entity/            # Database entities (User, App, AppVersion, ChatHistory)
    ├── enums/             # CodeGenTypeEnum, GenerationSceneEnum, etc.
    └── vo/                # View Objects
```

### Code Generation Types (`CodeGenTypeEnum`)

| Type | Value | Description | Model | Tools |
|------|-------|-------------|-------|-------|
| HTML | `HTML` | Single HTML file with embedded CSS/JS | Standard streaming | None |
| MULTI_FILE | `multi_file` | Separate HTML, CSS, JS files | Standard streaming | None |
| VUE_PROJECT | `vue_project` | Full Vue project with npm build | Reasoning streaming | FileWriteTool + others |

### Generation Scenes (`GenerationSceneEnum`)

| Scene | Tools | Use Case |
|-------|-------|----------|
| CREATION | `FileWriteTool` only | Creating new Vue project |
| MODIFICATION | Full set: Write/Read/Delete/Modify/List | Editing existing Vue project |

### AI Service Factory Architecture

`AiCodeGeneratorServiceFactory` manages cached AI service instances:

```
Factory
├── serviceCache (HTML/MULTI_FILE)
│   ├── max: 1000 instances
│   ├── expireAfterWrite: 30min
│   └── expireAfterAccess: 10min
│
├── creatorServiceCache (Vue creation)
│   └── Tools: [FileWriteTool]
│
├── modifierServiceCache (Vue modification)
│   └── Tools: [FileWriteTool, FileReadTool, DirectoryListTool, FileDeleteTool, FileModifyTool]
│
└── Each appId has isolated chat memory
    ├── Redis-backed MessageWindowChatMemory
    ├── maxMessages: 20
    └── Lazy loads from ChatHistory table
```

### Stream Processing Flow

**HTML/MULTI_FILE**:
```
UserMessage → AiCodeGeneratorService.generateXxxCodeStream()
→ Flux<String> → AiCodeGeneratorFacade.processCodeStream()
→ CodeParserExecutor → CodeFileSaverExecutor → AppVersionService.createVersion()
```

**VUE_PROJECT**:
```
UserMessage → AiCodeCreator/AiCodeModifier.chat()
→ TokenStream → JsonMessageStreamHandler.handle()
→ Parses JSON messages (AI_RESPONSE, TOOL_REQUEST, TOOL_EXECUTED)
→ FileWriteTool executes file operations
→ VueProjectBuilder.buildProjectAsyncByAppId() → npm run build
```

### Stream Message Types (`StreamMessageTypeEnum`)

| Type | Class | Purpose |
|------|-------|---------|
| `AI_RESPONSE` | `AiResponseMessage` | AI text response chunks |
| `TOOL_REQUEST` | `ToolRequestMessage` | AI requesting tool execution |
| `TOOL_EXECUTED` | `ToolExecutedMessage` | Tool execution result with arguments |

### AI Tools

| Tool | Method | Scene |
|------|--------|-------|
| `FileWriteTool` | `writeFile(relativeFilePath, content, appId)` | Both |
| `FileReadTool` | `readFile(relativeFilePath, appId)` | MODIFICATION |
| `FileModifyTool` | `modifyFile(relativeFilePath, oldContent, newContent, appId)` | MODIFICATION |
| `FileDeleteTool` | `deleteFile(relativeFilePath, appId)` | MODIFICATION |
| `DirectoryListTool` | `listDirectory(relativePath, appId)` | MODIFICATION |

### Key Files

| File | Purpose |
|------|---------|
| `AiCodeGeneratorFacade.java` | Main orchestration for code generation |
| `AiCodeGeneratorServiceFactory.java` | AI service creation and caching |
| `JsonMessageStreamHandler.java` | VUE_PROJECT stream processing |
| `VueProjectBuilder.java` | Async npm build execution |
| `GenerationSessionManager.java` | SSE session tracking and interrupt |
| `application.yml` | Main configuration (DB, Redis, port 8123) |
| `application-location.yml` | External LangChain4j model config (not in repo) |

### Database Entities

| Entity | Key Fields |
|--------|------------|
| `App` | codeGenType, deployKey, generatedCode, currentVersion |
| `AppVersion` | content, changeType, summary, versionNumber, isCurrent |
| `ChatHistory` | appId, userId, messageType, message, parentId |

### API Endpoints

| Endpoint | Type | Purpose |
|----------|------|---------|
| `/api/app/chat/gen/code` | SSE | Streaming code generation |
| `/api/app/chat/stop` | POST | Stop generation by sessionId |
| `/api/static/{deployKey}/**` | GET | Deployed app static resources |
| `/api/preview/{type_appId}/**` | GET | Preview generated code |
| `/api/chatHistory/app/{appId}` | GET | Chat history (cursor pagination) |
| `/api/app/version/list/page` | POST | Version list |
| `/api/app/version/diff` | GET | Version comparison |
| `/api/app/version/rollback` | POST | Version rollback |
| `/api/doc.html` | GET | Knife4j API documentation |

### SSE Event Sequence

```
event: session
data: {"sessionId":"abc123"}

data: {"d":"content chunk"}
data: {"d":"more content"}

event: done
data:
```

## Environment Requirements

- Java 21
- MySQL (database: `ai-code-mother`)
- Redis (session + chat memory)
- Node.js (for Vue project builds)

## External Configuration

Required file `application-location.yml`:
```yaml
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.deepseek.com
      model-name: deepseek-chat
      max-tokens: 8192
    streaming-chat-model:
      base-url: https://api.deepseek.com
      model-name: deepseek-chat
    reasoning-streaming-chat-model:
      base-url: https://api.deepseek.com
      model-name: deepseek-reasoner
```

## Code Style Guidelines

### Import Order
```java
// 1. Java standard library
import java.util.List;
// 2. Jakarta
import jakarta.servlet.http.HttpServletRequest;
// 3. Spring Framework
import org.springframework.web.bind.annotation.*;
// 4. Third-party (Hutool, Lombok, MyBatis-Flex, Reactor)
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.Data;
import reactor.core.publisher.Flux;
// 5. Project-specific
import com.zkf.aicodemother.model.entity.User;
```

### Error Handling
```java
// Use ThrowUtils for assertion-style validation
ThrowUtils.throwIf(condition, ErrorCode.PARAMS_ERROR);
ThrowUtils.throwIf(condition, ErrorCode.NOT_FOUND_ERROR, "自定义消息");

// ErrorCode values: SUCCESS(0), PARAMS_ERROR(40000), NOT_LOGIN_ERROR(40100),
// NO_AUTH_ERROR(40101), NOT_FOUND_ERROR(40400), SYSTEM_ERROR(50000)
```

### Common Patterns

**Controller**: `@RestController`, `@Resource` injection, `// region ... // endregion` grouping

**Service**: `ServiceImpl<Mapper, Entity>`, `@Transactional` for mutations

**Entity**: `@Data`, `@Table("table_name")`, `@Id(keyType = KeyType.Generator)`

**QueryWrapper**:
```java
QueryWrapper queryWrapper = QueryWrapper.create();
queryWrapper.eq("id", id, id != null);  // conditional
queryWrapper.like("name", name, StrUtil.isNotBlank(name));
```

## Key Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| spring-boot | 3.5.11 | Core framework |
| mybatis-flex | 1.11.0 | ORM |
| langchain4j | 1.1.0 | AI integration |
| langchain4j-reactor | 1.1.0-beta7 | SSE streaming |
| langchain4j-community-redis | 1.1.0-beta7 | Chat memory storage |
| hutool-all | 5.8.38 | Utilities |
| knife4j-openapi3 | 4.4.0 | API docs |
| java-diff-utils | 4.12 | Version diff |
| caffeine | - | AI service cache |