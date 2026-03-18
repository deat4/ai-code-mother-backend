package com.zkf.aicodemother.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zkf.aicodemother.ai.tools.FileWriteTool;
import com.zkf.aicodemother.exception.BusinessException;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.model.enums.CodeGenTypeEnum;
import com.zkf.aicodemother.service.AiCodeGeneratorService;
import com.zkf.aicodemother.service.ChatHistoryService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Duration;

/**
 * AI 代码生成服务工厂
 * 支持按 appId 隔离对话记忆，并使用 Caffeine 缓存优化性能
 * 支持根据代码生成类型选择不同的模型配置
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


    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     * 缓存键格式：appId_codeGenType
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
     * 根据 appId 获取服务（带缓存）
     * 这个方法是为了兼容历史逻辑，默认使用 HTML 模式
     *
     * @param appId 应用 ID
     * @return AI 代码生成服务
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 和代码生成类型获取服务（带缓存）
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型
     * @return AI 代码生成服务
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 构建缓存键
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型
     * @return 缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

    /**
     * 创建新的 AI 服务实例
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型
     * @return AI 代码生成服务
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {}, codeGenType: {} 创建新的 AI 服务实例", appId, codeGenType.getValue());

        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(chatMemoryStore)
                .maxMessages(20)
                .build();

        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            // Vue 项目生成使用推理模型，支持工具调用
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                    .streamingChatModel(reasoningStreamChatModel)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(new FileWriteTool())
                    .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                            toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                    ))

                    .build();
            // HTML 和多文件生成使用默认模型
            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(streamingChatModel)
                    .chatMemory(chatMemory)
                    .build();
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    /**
     * 默认 AI 服务（向后兼容）
     * 使用 appId = 0 作为默认对话记忆
     *
     * @return AI 代码生成服务
     */
    @Lazy
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }

}