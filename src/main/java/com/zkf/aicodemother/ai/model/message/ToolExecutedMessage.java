package com.zkf.aicodemother.ai.model.message;

import dev.langchain4j.service.tool.ToolExecution;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果消息
 * 当工具执行完成后发送此消息，包含执行结果
 *
 * @author zkf
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolExecutedMessage extends StreamMessage {

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
     * 工具执行结果
     */
    private String result;

    /**
     * 通过 ToolExecution 构造工具执行结果消息
     *
     * @param toolExecution LangChain4j 工具执行对象
     */
    public ToolExecutedMessage(ToolExecution toolExecution) {
        super(StreamMessageTypeEnum.TOOL_EXECUTED.getValue());
        this.id = toolExecution.request().id();
        this.name = toolExecution.request().name();
        this.arguments = toolExecution.request().arguments();
        this.result = toolExecution.result();
    }
}