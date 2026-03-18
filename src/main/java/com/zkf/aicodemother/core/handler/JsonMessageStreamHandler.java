package com.zkf.aicodemother.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zkf.aicodemother.ai.model.message.AiResponseMessage;
import com.zkf.aicodemother.ai.model.message.StreamMessage;
import com.zkf.aicodemother.ai.model.message.StreamMessageTypeEnum;
import com.zkf.aicodemother.ai.model.message.ToolExecutedMessage;
import com.zkf.aicodemother.ai.model.message.ToolRequestMessage;
import com.zkf.aicodemother.core.builder.VueProjectBuilder;
import com.zkf.aicodemother.model.entity.User;

// 注意：这里可能需要引入你的 ChatHistoryService 所在包
// import com.zkf.aicodemother.service.ChatHistoryService;

import com.zkf.aicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * JSON 消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 *
 * @author zkf
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    private static final String MESSAGE_TYPE_AI = "ai";

    @Resource
    private VueProjectBuilder vueProjectBuilder;

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
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();

        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字串
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    if (StrUtil.isNotEmpty(aiResponse)) {
                        chatHistoryService.addChatMessage(appId, aiResponse, MESSAGE_TYPE_AI, loginUser.getId());
                        log.info("Vue 项目生成完成，已保存到对话历史，appId: {}, 内容长度: {}", appId, aiResponse.length());
                    }
                    // 异步构建 Vue 项目（不阻塞主流程）
                    vueProjectBuilder.buildProjectAsyncByAppId(appId);
                }) // 修复点：删除了这里之后意外重复的代码块和多余的 })
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
     * @param chatHistoryStringBuilder  对话历史构建器
     * @param seenToolIds               已见过的工具ID集合
     * @return 处理后的输出字符串
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
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
                    // 检查是否是第一次看到这个工具 ID
                    if (toolId != null && !seenToolIds.contains(toolId)) {
                        // 第一次调用这个工具，记录 ID 并返回"选择工具"提示
                        seenToolIds.add(toolId);
                        return "\n\n[选择工具] 写入文件\n\n";
                    } else {
                        // 不是第一次调用这个工具，直接返回空
                        return "";
                    }
                }
                case TOOL_EXECUTED -> {
                    ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                    String arguments = toolExecutedMessage.getArguments();
                    if (StrUtil.isEmpty(arguments)) {
                        return "";
                    }

                    JSONObject jsonObject = JSONUtil.parseObj(arguments);
                    String relativeFilePath = jsonObject.getStr("relativeFilePath");
                    String content = jsonObject.getStr("content");

                    if (StrUtil.isEmpty(relativeFilePath)) {
                        return "";
                    }

                    String suffix = FileUtil.getSuffix(relativeFilePath);
                    String result = String.format("""
                            [工具调用] 写入文件 %s
                            ```%s
                            %s
                            ```
                            """, relativeFilePath, suffix != null ? suffix : "", content != null ? content : "");

                    // 输出前端和要持久化的内容
                    String output = String.format("\n\n%s\n\n", result);
                    chatHistoryStringBuilder.append(output);
                    return output;
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
}