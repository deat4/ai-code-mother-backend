package com.zkf.aicodemother.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zkf.aicodemother.ai.tools.BaseTool;
import com.zkf.aicodemother.ai.tools.FileDeleteTool;
import com.zkf.aicodemother.ai.tools.FileModifyTool;
import com.zkf.aicodemother.ai.tools.FileReadTool;
import com.zkf.aicodemother.ai.tools.FileWriteTool;
import com.zkf.aicodemother.ai.tools.DirectoryListTool;
import com.zkf.aicodemother.exception.BusinessException;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.model.enums.CodeGenTypeEnum;
import com.zkf.aicodemother.model.enums.GenerationSceneEnum;
import com.zkf.aicodemother.service.AiCodeGeneratorService;
import com.zkf.aicodemother.service.ChatHistoryOriginalService;
import com.zkf.aicodemother.service.ChatHistoryService;
import com.zkf.aicodemother.ai.AiCodeCreator;
import com.zkf.aicodemother.ai.AiCodeModifier;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;

/**
 * AI 代码生成服务工厂
 * 支持按 appId 隔离对话记忆，并使用 Caffeine 缓存优化性能
 * 支持根据代码生成类型选择不同的模型配置
 * 支持根据场景（创建/修改）提供不同的工具集合和系统提示词
 *
 * @author <a href="https://github.com/deat4/ai-code-mother-backend">zkf</a>
 */
@Slf4j
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    @Qualifier("openAiStreamingChatModel")
    private StreamingChatModel streamingChatModel;

    @Resource
    private ChatMemoryStore chatMemoryStore;

    @Resource
    @Qualifier("reasoningStreamingChatModel")
    private StreamingChatModel reasoningStreamChatModel;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ChatHistoryOriginalService chatHistoryOriginalService;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * Vue 项目创建服务缓存
     */
    private final Cache<String, AiCodeCreator> creatorServiceCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("Vue 创建服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * Vue 项目修改服务缓存
     */
    private final Cache<String, AiCodeModifier> modifierServiceCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("Vue 修改服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 用于创建场景的工具集合（仅文件写入工具）
     */
    @Bean
    @Qualifier("creationTools")
    public BaseTool[] creationTools() {
        return new BaseTool[]{new FileWriteTool()};
    }

    /**
     * 用于修改场景的工具集合（完整的读/写/改/删工具）
     */
    @Bean
    @Qualifier("modificationTools")
    public BaseTool[] modificationTools() {
        return new BaseTool[]{
                new FileWriteTool(),
                new FileReadTool(),
                new DirectoryListTool(),
                new FileDeleteTool(),
                new FileModifyTool()
        };
    }

    // ==================== HTML/MultiFile 服务 ====================

    /**
     * 根据 appId 和代码生成类型获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = appId + "_" + codeGenType.getValue();
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 根据 appId 获取服务（默认使用 HTML 类型）
     * 保持原有兼容性
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 内部创建 AI 服务实例的方法
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {}, codeGenType: {} 创建新的 AI 服务实例", appId, codeGenType.getValue());

        // 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = buildChatMemory(appId);

        // 根据代码生成类型选择模型
        return switch (codeGenType) {
            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(streamingChatModel)
                    .chatMemory(chatMemory)
                    .build();

            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    // ==================== Vue 项目服务 ====================

    /**
     * 根据 appId 获取 Vue 项目创建服务
     * 使用创建场景工具集（仅 writeFile）
     */
    public AiCodeCreator getVueProjectCreatorService(long appId) {
        String cacheKey = String.valueOf(appId);
        return creatorServiceCache.get(cacheKey, key -> createVueProjectCreatorService(appId));
    }

    /**
     * 根据 appId 获取 Vue 项目修改服务
     * 使用修改场景工具集（完整工具）
     */
    public AiCodeModifier getVueProjectModifierService(long appId) {
        String cacheKey = String.valueOf(appId);
        return modifierServiceCache.get(cacheKey, key -> createVueProjectModifierService(appId));
    }

    /**
     * 根据场景获取对应的 Vue 项目服务
     *
     * @param appId 应用 ID
     * @param scene 场景（创建/修改）
     * @return 对应的服务实例（返回 Object 类型，调用方需自行转型）
     */
    public Object getVueProjectService(long appId, GenerationSceneEnum scene) {
        return switch (scene) {
            case CREATION -> getVueProjectCreatorService(appId);
            case MODIFICATION -> getVueProjectModifierService(appId);
        };
    }

    /**
     * 创建 Vue 项目创建服务实例
     */
    private AiCodeCreator createVueProjectCreatorService(long appId) {
        log.info("为 appId: {} 创建 Vue 项目创建服务实例", appId);

        // Vue 项目使用原始历史表加载完整工具调用记忆
        MessageWindowChatMemory chatMemory = buildChatMemoryForVue(appId);

        return AiServices.builder(AiCodeCreator.class)
                .streamingChatModel(reasoningStreamChatModel)
                .chatMemoryProvider(memoryId -> chatMemory)
                .tools((Object[]) creationTools()) // 仅提供写入工具
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                ))
                .build();
    }

    /**
     * 创建 Vue 项目修改服务实例
     */
    private AiCodeModifier createVueProjectModifierService(long appId) {
        log.info("为 appId: {} 创建 Vue 项目修改服务实例", appId);

        // Vue 项目使用原始历史表加载完整工具调用记忆
        MessageWindowChatMemory chatMemory = buildChatMemoryForVue(appId);

        return AiServices.builder(AiCodeModifier.class)
                .streamingChatModel(reasoningStreamChatModel)
                .chatMemoryProvider(memoryId -> chatMemory)
                .tools((Object[]) modificationTools()) // 提供完整工具集
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                ))
                .build();
    }

    // ==================== 通用方法 ====================

    /**
     * 构建对话记忆（用于 HTML/MULTI_FILE 模式）
     * 从标准 chat_history 表加载文本记忆
     */
    private MessageWindowChatMemory buildChatMemory(long appId) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(chatMemoryStore)
                .maxMessages(20)
                .build();

        // 从数据库加载历史对话
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        return chatMemory;
    }

    /**
     * 构建 Vue 项目对话记忆
     * 从 chat_history_original 表加载完整工具调用记忆
     * 使用更大的消息窗口（60条），因为工具调用会消耗更多消息
     */
    private MessageWindowChatMemory buildChatMemoryForVue(long appId) {
        final int vueMaxMessages = 60;

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(chatMemoryStore)
                .maxMessages(vueMaxMessages)
                .build();

        // 从原始历史表加载完整工具调用记忆
        chatHistoryOriginalService.loadOriginalHistoryToMemory(appId, chatMemory, vueMaxMessages);

        return chatMemory;
    }

    /**
     * 默认 AI 服务（向后兼容）
     * 使用 appId = 0 和 HTML 类型作为默认实例
     */
    @Lazy
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }
}