# AI 并发调用问题分析与解决方案

## 问题现象

当多个用户同时使用平台时，只有第一个用户的 AI 请求能够正常处理，后续的请求都会被阻塞，需要等待前面的请求完全处理完毕后才能开始执行。

## 问题分析

### 测试验证结果

```
[请求-2] 第一个响应到达，耗时: 1198ms
[请求-1] 第一个响应到达，耗时: 16822ms  ← 等请求2完成后才开始
[请求-3] 第一个响应到达，耗时: 30727ms  ← 等请求1完成后才开始
```

**关键证据：所有请求都在同一线程 `[hain4j-OpenAI-1]` 上执行**

### 根本原因

`StreamingChatModel` 虽然返回 `Flux<String>` 响应式流，但底层 HTTP 客户端是同步阻塞的：

```
Flux.generate() → 内部 HTTP 请求建立连接阶段是同步阻塞的
                        ↓
           必须等待 HTTP 连接建立 + 收到第一个响应字节
                        ↓
           后续的流式数据推送才是真正的异步
```

## 解决方案：Spring 多例模式

### 原理

为每次 AI 服务调用创建独立的 `StreamingChatModel` 实例，避免多个请求共用同一个实例导致的阻塞。

### 实现步骤

#### 1. 配置文件（application-location.yml）

```yaml
langchain4j:
  open-ai:
    # 流式聊天模型配置（多例模式）
    streaming-chat-model:
      base-url: https://api.deepseek.com
      api-key: <Your API Key>
      model-name: deepseek-chat
      max-tokens: 8192
      temperature: 0.7
      log-requests: true
      log-responses: true

    # 推理流式模型配置（多例模式）
    reasoning-streaming-chat-model:
      base-url: https://api.deepseek.com
      api-key: <Your API Key>
      model-name: deepseek-chat
      max-tokens: 8192
      temperature: 0.1
      log-requests: true
      log-responses: true

    # 智能路由模型配置（多例模式）
    routing-chat-model:
      base-url: https://api.deepseek.com
      api-key: <Your API Key>
      model-name: deepseek-chat
      max-tokens: 1024
      temperature: 0.3
      log-requests: false
      log-responses: false
```

#### 2. 配置类（使用 @Scope("prototype")）

```java
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
@Data
public class StreamingChatModelConfig {
    private String baseUrl;
    private String apiKey;
    // ... 其他属性

    @Bean
    @Scope("prototype")
    public StreamingChatModel streamingChatModelPrototype() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }
}
```

#### 3. 工具类获取多例 Bean

```java
@Component
public class SpringContextUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    public static <T> T getBean(String name, Class<T> clazz) {
        return applicationContext.getBean(name, clazz);
    }
}
```

#### 4. 工厂类使用多例模式

```java
private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
    // 使用多例模式获取 StreamingChatModel，每次都是新实例
    StreamingChatModel streamingChatModel = SpringContextUtil.getBean(
            "streamingChatModelPrototype", StreamingChatModel.class);

    return AiServices.builder(AiCodeGeneratorService.class)
            .streamingChatModel(streamingChatModel)
            .build();
}
```

### 预期效果

**修改前（串行）：**
```
[请求-2] 第一个响应到达，耗时: 1198ms
[请求-1] 第一个响应到达，耗时: 16822ms  ← 累加
[请求-3] 第一个响应到达，耗时: 30727ms  ← 累加
```

**修改后（并行）：**
```
[请求-1] 第一个响应到达，耗时: ~2000ms
[请求-2] 第一个响应到达，耗时: ~2100ms  ← 接近
[请求-3] 第一个响应到达，耗时: ~2200ms  ← 接近
```

## 文件修改清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `utils/SpringContextUtil.java` | 新建 | 获取多例 Bean 的工具类 |
| `config/StreamingChatModelConfig.java` | 新建 | 多例流式模型配置 |
| `config/ReasoningStreamingChatModelConfig.java` | 修改 | 改为多例模式 |
| `config/RoutingChatModelConfig.java` | 新建 | 多例路由模型配置 |
| `config/AiCodeGeneratorServiceFactory.java` | 修改 | 使用多例获取 StreamingChatModel |
| `config/AiCodeGenTypeRoutingServiceFactory.java` | 修改 | 使用多例获取 ChatModel |

## 运行测试验证

```bash
mvn test -Dtest=AiConcurrencyTest#testConcurrencyWithPrototypePattern
```