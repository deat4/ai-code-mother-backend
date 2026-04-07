package com.zkf.aicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.exception.ThrowUtils;
import com.zkf.aicodemother.mapper.ChatHistoryMapper;
import com.zkf.aicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.zkf.aicodemother.model.entity.ChatHistory;
import com.zkf.aicodemother.model.enums.MessageTypeEnum;
import com.zkf.aicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


/**
 * 对话历史服务层实现
 *
 * @author <a href="https://github.com/deat4/ai-code-mother-backend">zkf</a>
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    private ChatHistoryMapper chatHistoryMapper;


    public ChatHistory saveUserMessage(Long appId, Long userId, String message) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .message(message)
                .messageType(MessageTypeEnum.USER.getValue())
                .build();

        boolean success = this.save(chatHistory);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "保存用户消息失败");

        log.info("保存用户消息成功: appId={}, userId={}, messageId={}", appId, userId, chatHistory.getId());
        return chatHistory;
    }


    public ChatHistory saveAiMessage(Long appId, Long userId, Long parentId, String message, String fileList) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message) && StrUtil.isBlank(fileList), ErrorCode.PARAMS_ERROR, "消息内容和文件列表不能同时为空");

        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .parentId(parentId)
                .message(message != null ? message : "")
                .messageType(MessageTypeEnum.AI.getValue())
                .fileList(fileList)
                .build();

        boolean success = this.save(chatHistory);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "保存AI消息失败");

        log.info("保存AI消息成功: appId={}, userId={}, parentId={}, messageId={}", appId, userId, parentId, chatHistory.getId());
        return chatHistory;
    }

    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");

        // 验证消息类型是否有效
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型: " + messageType);

        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();

        return this.save(chatHistory);
    }


    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest queryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (queryRequest == null) {
            return queryWrapper;
        }

        Long id = queryRequest.getId();
        String message = queryRequest.getMessage();
        String messageType = queryRequest.getMessageType();
        Long appId = queryRequest.getAppId();
        Long userId = queryRequest.getUserId();
        LocalDateTime lastCreateTime = queryRequest.getLastCreateTime();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq("id", id, id != null)
                .like("message", message, StrUtil.isNotBlank(message))
                .eq("messageType", messageType, StrUtil.isNotBlank(messageType))
                .eq("appId", appId, appId != null)
                .eq("userId", userId, userId != null)
                .eq("isDelete", 0);

        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }

        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }

        return queryWrapper;
    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");

        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);

        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);

        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public Page<ChatHistory> listAllChatHistoryByPageForAdmin(ChatHistoryQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR, "查询参数不能为空");

        int pageNum = queryRequest.getCurrent() > 0 ? queryRequest.getCurrent() : 1;
        int pageSize = queryRequest.getPageSize() > 0 ? queryRequest.getPageSize() : 10;

        // 查询数据
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        return this.page(Page.of(pageNum, pageSize), queryWrapper);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");

        QueryWrapper queryWrapper = QueryWrapper.create().eq("appId", appId);
        return this.remove(queryWrapper);
    }


    @Override
    public int loadChatHistoryToMemory(Long appId, dev.langchain4j.memory.chat.MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
            ThrowUtils.throwIf(chatMemory == null, ErrorCode.PARAMS_ERROR, "对话记忆不能为空");

            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            // 因为在对话流程中，用户消息被添加到数据库后，AI服务也会自动将用户消息添加到记忆中
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq("appId", appId)
                    .eq("isDelete", 0)
                    .orderBy("createTime", false)
                    .limit(maxCount, 1);  // offset=1 排除最新消息

            List<ChatHistory> historyList = this.list(queryWrapper);
            if (historyList == null || historyList.isEmpty()) {
                return 0;
            }

            // 反转列表，确保按时间正序（老的在前，新的在后）
            java.util.Collections.reverse(historyList);

            // 先清理历史缓存，防止重复加载
            chatMemory.clear();

            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            for (ChatHistory history : historyList) {
                String messageType = history.getMessageType();
                String message = history.getMessage();

                if (MessageTypeEnum.USER.getValue().equals(messageType)) {
                    chatMemory.add(dev.langchain4j.data.message.UserMessage.from(message));
                    loadedCount++;
                } else if (MessageTypeEnum.AI.getValue().equals(messageType)) {
                    chatMemory.add(dev.langchain4j.data.message.AiMessage.from(message));
                    loadedCount++;
                }
            }

            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }

}