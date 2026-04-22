package com.zkf.aicodemother.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zkf.aicodemother.ai.model.message.*;
import com.zkf.aicodemother.ai.tools.BaseTool;
import com.zkf.aicodemother.ai.tools.ToolManager;
import com.zkf.aicodemother.config.AppConfig;
import com.zkf.aicodemother.core.CodeGenTypeEnum;
import com.zkf.aicodemother.core.builder.VueProjectBuilder;
import com.zkf.aicodemother.model.entity.ChatHistoryOriginal;
import com.zkf.aicodemother.model.entity.User;
import com.zkf.aicodemother.model.enums.MessageTypeEnum;
import com.zkf.aicodemother.service.AppService;
import com.zkf.aicodemother.service.ChatHistoryOriginalService;
import com.zkf.aicodemother.service.ChatHistoryService;
import com.zkf.aicodemother.service.impl.ScreenshotServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JSON 消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 * 支持双轨制存储：前端展示用格式化文本 + AI 记忆恢复用原始 JSON
 *
 * @author zkf
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    private static final String MESSAGE_TYPE_AI = "ai";

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ToolManager toolManager;

    @Resource
    private ChatHistoryOriginalService chatHistoryOriginalService;

    @Resource
    private ScreenshotServiceImpl screenshotService;

    @Resource
    @org.springframework.context.annotation.Lazy
    private AppService appService;

    @Resource
    private AppConfig appConfig;

    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // 收集数据用于生成后端记忆格式（前端展示用）
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 收集原始消息用于 AI 记忆恢复（包含完整工具调用信息）
        List<ChatHistoryOriginal> originalMessages = new ArrayList<>();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();

        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds, originalMessages, appId, loginUser.getId());
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字符串
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史（前端展示用）
                    String aiResponse = chatHistoryStringBuilder.toString();
                    if (StrUtil.isNotEmpty(aiResponse)) {
                        chatHistoryService.addChatMessage(appId, aiResponse, MESSAGE_TYPE_AI, loginUser.getId());
                        log.info("Vue 项目生成完成，已保存到对话历史，appId: {}, 内容长度: {}", appId, aiResponse.length());
                    }

                    // 批量保存原始消息到新表（AI 记忆恢复用）
                    if (!originalMessages.isEmpty()) {
                        try {
                            chatHistoryOriginalService.batchAddMessages(originalMessages);
                            log.info("已保存 {} 条原始对话历史，appId: {}", originalMessages.size(), appId);
                        } catch (Exception e) {
                            log.error("保存原始对话历史失败，appId: {}", appId, e);
                        }
                    }

                    // 异步构建 Vue 项目（不阻塞主流程）
                    vueProjectBuilder.buildProjectAsyncByAppId(appId);

                    // 异步生成截图封面
                    generateVueCoverAsync(appId);
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, MESSAGE_TYPE_AI, loginUser.getId());
                    log.error("Vue 项目生成失败，appId: {}, error: {}", appId, error.getMessage());
                });
    }

    /**
     * 解析并收集 TokenStream 数据
     *
     * @param chunk                     JSON 消息块
     * @param chatHistoryStringBuilder  对话历史构建器（前端展示用）
     * @param seenToolIds               已见过的工具ID集合
     * @param originalMessages          原始消息收集器（AI 记忆恢复用）
     * @param appId                     应用ID
     * @param userId                    用户ID
     * @return 处理后的输出字符串
     */
    private String handleJsonMessageChunk(String chunk,
                                          StringBuilder chatHistoryStringBuilder,
                                          Set<String> seenToolIds,
                                          List<ChatHistoryOriginal> originalMessages,
                                          long appId,
                                          long userId) {
        try {
            // 解析 JSON
            StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
            StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());

            if (typeEnum == null) {
                log.warn("未知的消息类型: {}", streamMessage.getType());
                return "";
            }

            switch (typeEnum) {
                case AI_RESPONSE -> {
                    AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                    String data = aiMessage.getData();
                    if (data != null) {
                        // 直接拼接响应
                        chatHistoryStringBuilder.append(data);
                        return data;
                    }
                    return "";
                }
                case TOOL_REQUEST -> {
                    ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                    String toolId = toolRequestMessage.getId();
                    String toolName = toolRequestMessage.getName();

                    // 保存原始工具调用请求到新表（AI 记忆恢复用）
                    saveOriginalMessage(originalMessages, appId, userId, chunk, MessageTypeEnum.TOOL_REQUEST.getValue());

                    // 检查是否是第一次看到这个工具 ID
                    if (toolId != null && !seenToolIds.contains(toolId)) {
                        // 第一次调用这个工具，记录 ID 并返回工具信息
                        seenToolIds.add(toolId);
                        // 根据工具名称获取工具实例
                        BaseTool tool = toolManager.getTool(toolName);
                        // 返回格式化的工具调用信息
                        if (tool != null) {
                            return tool.generateToolRequestResponse();
                        } else {
                            log.warn("未找到工具实例: {}", toolName);
                            return String.format("\n\n[选择工具] %s\n\n", toolName);
                        }
                    } else {
                        // 不是第一次调用这个工具，直接返回空字符串
                        return "";
                    }
                }
                case TOOL_EXECUTED -> {
                    ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                    String toolName = toolExecutedMessage.getName();
                    JSONObject arguments = JSONUtil.parseObj(toolExecutedMessage.getArguments());

                    // 保存原始工具执行结果到新表（AI 记忆恢复用）
                    saveOriginalMessage(originalMessages, appId, userId, chunk, MessageTypeEnum.TOOL_RESULT.getValue());

                    // 根据工具名称获取工具实例并生成相应的结果格式
                    BaseTool tool = toolManager.getTool(toolName);

                    if (tool != null) {
                        String result = tool.generateToolExecutedResult(arguments);
                        // 检查是否返回了结构化 JSON（包含 type 字段）
                        try {
                            JSONObject resultJson = JSONUtil.parseObj(result);
                            if (resultJson.containsKey("type") && "tool_call".equals(resultJson.getStr("type"))) {
                                // 结构化工具调用，返回带事件标记的 JSON
                                chatHistoryStringBuilder.append(String.format("[工具调用] %s %s\n",
                                        resultJson.getStr("toolName"), resultJson.getStr("fileName")));
                                return "EVENT:tool_call:" + result;
                            }
                        } catch (Exception ignored) {
                            // 不是 JSON 格式，按普通文本处理
                        }
                        // 普通文本格式
                        String output = String.format("\n\n%s\n\n", result);
                        chatHistoryStringBuilder.append(output);
                        return output;
                    } else {
                        String output = String.format("[工具执行] %s", toolName);
                        chatHistoryStringBuilder.append(output).append("\n");
                        return output;
                    }
                }
                default -> {
                    log.warn("不支持的消息类型: {}", typeEnum);
                    return "";
                }
            }
        } catch (Exception e) {
            log.error("解析 JSON 消息失败: {}, error: {}", chunk, e.getMessage());
            return "";
        }
    }

    /**
     * 保存原始消息到收集列表
     *
     * @param originalMessages 原始消息列表
     * @param appId            应用ID
     * @param userId           用户ID
     * @param messageJson      消息JSON
     * @param messageType      消息类型
     */
    private void saveOriginalMessage(List<ChatHistoryOriginal> originalMessages,
                                     long appId,
                                     long userId,
                                     String messageJson,
                                     String messageType) {
        ChatHistoryOriginal originalMessage = ChatHistoryOriginal.builder()
                .appId(appId)
                .userId(userId)
                .message(messageJson)
                .messageType(messageType)
                .createTime(LocalDateTime.now())
                .build();
        originalMessages.add(originalMessage);
    }

    /**
     * 异步生成 Vue 项目截图封面
     *
     * @param appId 应用ID
     */
    private void generateVueCoverAsync(long appId) {
        // Vue 项目构建完成后截图，构建需要一些时间，所以延迟一段时间后截图
        // 使用异步方式，等待构建完成后截图
        log.info("开始异步生成Vue项目截图封面: appId={}", appId);

        // Vue 项目预览 URL（构建后的 dist 目录）
        String previewUrl = String.format("%s/vue_project_%s/",
                appConfig.getPreview().getHost(), appId);

        // 延迟执行截图（等待 Vue 项目构建完成）
        new java.util.concurrent.CompletableFuture<Void>().completeOnTimeout(null, 10, java.util.concurrent.TimeUnit.SECONDS)
                .thenRun(() -> {
                    screenshotService.generateAndUploadScreenshotAsync(previewUrl)
                            .thenAccept(coverUrl -> {
                                if (StrUtil.isNotBlank(coverUrl)) {
                                    boolean updated = appService.updateCover(appId, coverUrl);
                                    if (updated) {
                                        log.info("Vue项目封面更新成功: appId={}, coverUrl={}", appId, coverUrl);
                                    } else {
                                        log.warn("Vue项目封面更新失败: appId={}", appId);
                                    }
                                } else {
                                    log.warn("Vue项目截图生成失败，无法更新封面: appId={}", appId);
                                }
                            });
                });
    }
}