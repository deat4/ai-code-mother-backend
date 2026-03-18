package com.zkf.aicodemother.ai.model.message;

import lombok.Getter;

/**
 * 流式消息类型枚举
 * 定义了流式响应中可能出现的各种消息类型
 *
 * @author zkf
 */
@Getter
public enum StreamMessageTypeEnum {

    /**
     * AI 响应消息 - AI 生成的文本内容
     */
    AI_RESPONSE("ai_response", "AI响应"),

    /**
     * 工具请求消息 - AI 请求执行工具
     */
    TOOL_REQUEST("tool_request", "工具请求"),

    /**
     * 工具执行结果消息 - 工具执行完成后的结果
     */
    TOOL_EXECUTED("tool_executed", "工具执行结果");

    private final String value;
    private final String text;

    StreamMessageTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据值获取枚举
     *
     * @param value 枚举值
     * @return 枚举实例，如果未找到返回 null
     */
    public static StreamMessageTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (StreamMessageTypeEnum typeEnum : values()) {
            if (typeEnum.getValue().equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}