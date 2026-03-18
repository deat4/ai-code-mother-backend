# AGENTS.md

Guidelines for AI coding agents working in this Spring Boot 3.5 + Java 21 backend project.

## Project Overview

- **Framework**: Spring Boot 3.5.11 with Java 21
- **Language**: Java 21 (Virtual Threads supported)
- **Build Tool**: Maven
- **ORM**: MyBatis-Flex
- **AI Framework**: LangChain4j 1.1.0
- **Reactive**: Project Reactor (Flux/Mono)

## Commands

```bash
mvn clean install          # 编译项目
mvn spring-boot:run        # 启动服务
mvn test                   # 运行所有测试
mvn test -Dtest=ClassName#methodName  # 运行单个测试
```

## Project Structure

```
com.zkf.aicodemother
├── ai/                    # AI 代码生成模块
│   ├── model/
│   │   ├── HtmlCodeResult.java
│   │   ├── MultiFileCodeResult.java
│   │   └── message/       # 流式消息类
│   └── AiCodeGeneratorService.java
├── config/                # 配置类
│   └── AiCodeGeneratorServiceFactory.java  # AI 服务工厂
├── core/                  # 核心业务模块
│   ├── parser/            # 代码解析器（策略模式）
│   ├── saver/             # 文件保存器（模板方法模式）
│   ├── handler/           # Flux 流处理器
│   │   ├── SimpleTextStreamHandler.java
│   │   ├── JsonMessageStreamHandler.java
│   │   └── StreamHandlerExecutor.java
│   ├── builder/           # 项目构建器
│   │   └── VueProjectBuilder.java
│   ├── CodeGenTypeEnum.java
│   └── AiCodeGeneratorFacade.java
├── controller/            # API 控制器
├── service/               # 业务逻辑层
└── model/                 # 实体、DTO、VO、枚举
```

## Code Generation Types

| 类型        | 枚举值        | 说明             | 模型                |
| ----------- | ------------- | ---------------- | ------------------- |
| HTML        | `HTML`        | 单文件 HTML 生成 | 默认模型            |
| MULTI_FILE  | `multi_file`  | 多文件代码生成   | 默认模型            |
| VUE_PROJECT | `vue_project` | Vue 工程项目     | 推理模型 + 工具调用 |

## Stream Message Types

用于统一流式响应消息格式：

| 类型            | 类名                  | 字段                                |
| --------------- | --------------------- | ----------------------------------- |
| `ai_response`   | `AiResponseMessage`   | `data`                              |
| `tool_request`  | `ToolRequestMessage`  | `id`, `name`, `arguments`           |
| `tool_executed` | `ToolExecutedMessage` | `id`, `name`, `arguments`, `result` |

## AI Service Factory

按生成类型选择不同模型配置：

```java
// 获取 AI 服务
AiCodeGeneratorService service = aiCodeGeneratorServiceFactory
    .getAiCodeGeneratorService(appId, CodeGenTypeEnum.VUE_PROJECT);
```

## Flux Stream Handlers

根据生成类型自动选择流处理器：

```java
// 执行器自动选择处理器
Flux<String> processedFlux = streamHandlerExecutor.doExecute(
    originFlux, chatHistoryService, appId, loginUser, codeGenType
);
```

## Vue Project Builder

异步构建 Vue 项目（虚拟线程）：

```java
// 异步构建（不阻塞主流程）
vueProjectBuilder.buildProjectAsyncByAppId(appId);

// 同步构建
boolean success = vueProjectBuilder.buildProject(projectPath);
```

## Design Patterns

| 模式         | 应用场景                             |
| ------------ | ------------------------------------ |
| 门面模式     | `AiCodeGeneratorFacade` 统一入口     |
| 策略模式     | `CodeParser` 不同类型解析策略        |
| 模板方法模式 | `CodeFileSaverTemplate` 统一保存流程 |
| 执行器模式   | `StreamHandlerExecutor` 流处理器选择 |

## Code Style

- 使用 Lombok 注解（`@Data`, `@Slf4j`, `@Builder`）
- 异常使用 `BusinessException` + `ErrorCode`
- 参数校验使用 `ThrowUtils.throwIf()`
- 日志使用 `log.info()`, `log.error()`
- 所有 public 方法添加 Javadoc 注释

## Error Handling

```java
ThrowUtils.throwIf(condition, ErrorCode.SYSTEM_ERROR, "错误信息");
```

## Virtual Threads (Java 21)

```java
// 使用虚拟线程执行耗时操作
Thread.ofVirtual().name("task-name").start(() -> {
    // 异步任务
});
```

## LangChain4j Notes

- 版本: 1.1.0
- `TokenStream` 已被覆盖，支持 `onPartialToolCall` 和 `onCompleteToolCall`
- `@MemoryId` 用于对话记忆隔离
- `@SystemMessage(fromResource = "...")` 从资源文件加载系统提示词

## Dependencies

- Spring Boot 3.5.11
- Java 21
- MyBatis-Flex
- LangChain4j 1.1.0
- Reactor Core
- Hutool (工具库)
- Lombok
- Knife4j (API 文档)

## Configuration

关键配置文件：

- `application.yml` - 主配置
- `application-location.yml` - AI 模型配置
- `AppConstant.java` - 常量定义

## Testing

```java
@SpringBootTest
class ExampleTest {
    @Resource
    private SomeService someService;

    @Test
    void testSomething() {
        // 测试代码
    }
}
```

## API Documentation

启动后访问：http://localhost:8123/api/doc.html
