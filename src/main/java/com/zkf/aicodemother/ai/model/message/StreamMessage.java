package com.zkf.aicodemother.ai.model.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式消息响应基类
 * 用于统一不同类型的流式消息格式，方便前端区分处理
 *
 * @author zkf
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamMessage {
    
    /**
     * 消息类型
     * @see com.zkf.aicodemother.ai.model.message.StreamMessageTypeEnum
     */
    private String type;
}