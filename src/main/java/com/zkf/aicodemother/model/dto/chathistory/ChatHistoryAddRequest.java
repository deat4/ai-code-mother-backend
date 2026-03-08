package com.zkf.aicodemother.model.dto.chathistory;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建对话历史请求
 */
@Data
public class ChatHistoryAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 父消息id(用于关联AI回复与用户提示词)
     */
    private Long parentId;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型(user/ai)
     */
    private String messageType;

    /**
     * 文件列表(JSON数组)
     */
    private String fileList;

    /**
     * 应用id
     */
    private Long appId;
}