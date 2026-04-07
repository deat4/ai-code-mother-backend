package com.zkf.aicodemother.service;

import com.mybatisflex.core.service.IService;
import com.zkf.aicodemother.model.entity.ChatHistoryOriginal;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 * 原始对话历史服务层
 * 存储完整的工具调用信息，用于 AI 记忆恢复
 *
 * @author <a href="https://github.com/deat4/ai-code-mother-backend">zkf</a>
 */
public interface ChatHistoryOriginalService extends IService<ChatHistoryOriginal> {

    /**
     * 批量添加原始消息
     *
     * @param messages 消息列表
     * @return 是否成功
     */
    boolean batchAddMessages(List<ChatHistoryOriginal> messages);

    /**
     * 删除应用的所有原始对话历史
     *
     * @param appId 应用ID
     * @return 是否成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 加载原始对话历史到 AI 记忆中（含边缘检查）
     * 解决 LangChain4j 严格要求 Request-Result 成对的问题
     *
     * @param appId      应用ID
     * @param chatMemory AI 对话记忆
     * @param maxCount   最大加载条数
     * @return 实际加载的消息条数
     */
    int loadOriginalHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 带边缘检查的历史查询
     * 确保工具调用 Request-Result 配对完整
     *
     * @param appId    应用ID
     * @param maxCount 最大查询条数
     * @return 消息列表（正序，老数据在前）
     */
    List<ChatHistoryOriginal> queryHistoryWithEdgeCheck(Long appId, int maxCount);
}