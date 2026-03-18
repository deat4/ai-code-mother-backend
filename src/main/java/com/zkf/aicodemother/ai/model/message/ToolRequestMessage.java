package com.zkf.aicodemother.ai.model.message;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具调用请求消息
 * 当 AI 请求执行工具时发送此消息
 *
 * @author zkf
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolRequestMessage extends StreamMessage {

    /**
     * 工具调用唯一标识
     */
    private String id;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具参数（JSON 格式）
     */
    private String arguments;

    /**
     * 通过 ToolExecutionRequest 构造工具请求消息
     *
     * @param toolExecutionRequest LangChain4j 工具执行请求对象
     */
    public ToolRequestMessage(ToolExecutionRequest toolExecutionRequest) {
        super(StreamMessageTypeEnum.TOOL_REQUEST.getValue());
        this.id = toolExecutionRequest.id();
        this.name = toolExecutionRequest.name();
        this.arguments = toolExecutionRequest.arguments();
    }
}