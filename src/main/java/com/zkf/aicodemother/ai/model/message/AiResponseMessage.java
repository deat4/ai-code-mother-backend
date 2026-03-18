package com.zkf.aicodemother.ai.model.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * AI 响应消息
 * 用于传输 AI 生成的文本内容
 *
 * @author zkf
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class AiResponseMessage extends StreamMessage {

    /**
     * AI 生成的文本内容
     */
    private String data;

    /**
     * 构造 AI 响应消息
     *
     * @param data AI 生成的文本内容
     */
    public AiResponseMessage(String data) {
        super(StreamMessageTypeEnum.AI_RESPONSE.getValue());
        this.data = data;
    }
}