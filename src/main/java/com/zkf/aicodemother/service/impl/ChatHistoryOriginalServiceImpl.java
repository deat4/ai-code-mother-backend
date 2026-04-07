package com.zkf.aicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.exception.ThrowUtils;
import com.zkf.aicodemother.mapper.ChatHistoryOriginalMapper;
import com.zkf.aicodemother.model.entity.ChatHistoryOriginal;
import com.zkf.aicodemother.model.enums.MessageTypeEnum;
import com.zkf.aicodemother.service.ChatHistoryOriginalService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 原始对话历史服务层实现
 *
 * @author <a href="https://github.com/deat4/ai-code-mother-backend">zkf</a>
 */
@Slf4j
@Service
public class ChatHistoryOriginalServiceImpl extends ServiceImpl<ChatHistoryOriginalMapper, ChatHistoryOriginal> implements ChatHistoryOriginalService {

    @Resource
    private ChatHistoryOriginalMapper chatHistoryOriginalMapper;

    @Override
    public boolean batchAddMessages(List<ChatHistoryOriginal> messages) {
        if (CollUtil.isEmpty(messages)) {
            return true;
        }
        try {
            return this.saveBatch(messages);
        } catch (Exception e) {
            log.error("批量添加原始消息失败", e);
            return false;
        }
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");

        QueryWrapper queryWrapper = QueryWrapper.create().eq("appId", appId);
        return this.remove(queryWrapper);
    }

    @Override
    public int loadOriginalHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
            ThrowUtils.throwIf(chatMemory == null, ErrorCode.PARAMS_ERROR, "对话记忆不能为空");

            // 使用带边缘检查的查询方法
            List<ChatHistoryOriginal> historyList = queryHistoryWithEdgeCheck(appId, maxCount);

            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }

            // 先清理历史缓存，防止重复加载
            chatMemory.clear();

            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            for (ChatHistoryOriginal history : historyList) {
                String messageType = history.getMessageType();
                String message = history.getMessage();

                if (MessageTypeEnum.USER.getValue().equals(messageType)) {
                    chatMemory.add(UserMessage.from(message));
                    loadedCount++;
                } else if (MessageTypeEnum.AI.getValue().equals(messageType)) {
                    chatMemory.add(AiMessage.from(message));
                    loadedCount++;
                } else if (MessageTypeEnum.TOOL_REQUEST.getValue().equals(messageType)) {
                    // 解析工具调用请求 JSON
                    try {
                        JSONObject json = JSONUtil.parseObj(message);
                        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                                .id(json.getStr("id"))
                                .name(json.getStr("name"))
                                .arguments(json.getStr("arguments"))
                                .build();
                        // AiMessage 可以包含工具调用请求
                        chatMemory.add(AiMessage.from(List.of(toolRequest)));
                        loadedCount++;
                    } catch (Exception e) {
                        log.warn("解析工具调用请求失败: {}", message, e);
                    }
                } else if (MessageTypeEnum.TOOL_RESULT.getValue().equals(messageType)) {
                    // 解析工具执行结果 JSON
                    try {
                        JSONObject json = JSONUtil.parseObj(message);
                        String toolId = json.getStr("id");
                        String toolName = json.getStr("name");
                        String result = json.getStr("result");
                        ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.from(toolId, toolName, result);
                        chatMemory.add(resultMessage);
                        loadedCount++;
                    } catch (Exception e) {
                        log.warn("解析工具执行结果失败: {}", message, e);
                    }
                }
            }

            log.info("成功为 appId: {} 加载了 {} 条原始对话历史", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载原始对话历史失败，appId: {}, error: {}", appId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public List<ChatHistoryOriginal> queryHistoryWithEdgeCheck(Long appId, int maxCount) {
        // 1. 倒序查询 maxCount + 1 条数据（多查一条用于探路）
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .orderBy("createTime", false)  // 倒序，新数据在前
                .limit(maxCount + 1);

        List<ChatHistoryOriginal> list = this.list(queryWrapper);

        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        if (list.size() <= maxCount) {
            // 数据不够 maxCount，安全，反转后返回
            Collections.reverse(list);
            return list;
        }

        // 2. 检查第 maxCount 条（最边缘的那条数据）
        ChatHistoryOriginal edgeRecord = list.get(maxCount - 1);

        if (MessageTypeEnum.TOOL_RESULT.getValue().equals(edgeRecord.getMessageType())) {
            // 危险！边缘是一条 Result，它的 Request 可能被丢在 maxCount 之外了
            // 解决办法：主动丢弃这条 Result，让 maxCount - 1，保证 Request 和 Result 成对被排除或成对被包含
            list = list.subList(0, maxCount - 1);
            log.debug("边缘检查：检测到边缘为 tool_result，丢弃以保持 Request-Result 配对完整");
        } else {
            // 安全，截取前 maxCount 条
            list = list.subList(0, maxCount);
        }

        // 反转为正序（老数据在前，新数据在后）
        Collections.reverse(list);
        return list;
    }
}