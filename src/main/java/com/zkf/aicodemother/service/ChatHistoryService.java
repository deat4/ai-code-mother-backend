package com.zkf.aicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.zkf.aicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.zkf.aicodemother.model.entity.ChatHistory;
import com.zkf.aicodemother.model.vo.ChatHistoryVO;

import java.util.List;

/**
 * 对话历史服务层
 *
 * @author <a href="https://github.com/deat4/ai-code-mother-backend">zkf</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话消息（简化方法）
     *
     * @param appId       应用ID
     * @param message     消息内容
     * @param messageType 消息类型 (user/ai)
     * @param userId      用户ID
     * @return 是否成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 获取查询包装类
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest queryRequest);

    /**
     * 获取应用的对话历史(游标分页)
     *
     * @param appId          应用ID
     * @param pageSize       每页大小
     * @param lastCreateTime 游标时间(最后一条记录的创建时间)
     * @return 分页结果
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, java.time.LocalDateTime lastCreateTime);

    /**
     * 管理员分页查询对话历史
     *
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    Page<ChatHistory> listAllChatHistoryByPageForAdmin(ChatHistoryQueryRequest queryRequest);

    /**
     * 删除应用的所有对话历史
     *
     * @param appId 应用ID
     * @return 是否成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 加载历史对话到 AI 记忆中
     *
     * @param appId      应用ID
     * @param chatMemory AI 对话记忆
     * @param maxCount   最大加载条数
     * @return 实际加载的消息条数
     */
    int loadChatHistoryToMemory(Long appId, dev.langchain4j.memory.chat.MessageWindowChatMemory chatMemory, int maxCount);
}

