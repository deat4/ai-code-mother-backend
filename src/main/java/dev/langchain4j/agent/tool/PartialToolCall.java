package dev.langchain4j.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 部分工具调用
 * 用于流式输出工具调用的增量信息
 *
 * 注意：这是 LangChain4j 1.2.0+ 版本的类，在 1.1.0 版本中需要手动创建
 *
 * @author zkf
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartialToolCall {

    /**
     * 工具调用唯一标识
     */
    private String id;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具参数（JSON 格式，增量）
     */
    private String args;
}