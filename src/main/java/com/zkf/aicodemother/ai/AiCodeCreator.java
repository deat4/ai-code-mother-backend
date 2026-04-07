package com.zkf.aicodemother.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * AI 代码创建服务接口
 * 专门用于应用首次创建，仅提供文件写入能力
 */
public interface AiCodeCreator {

    /**
     * 生成代码（流式传输）
     *
     * @param userMessage 用户消息（需求描述）
     * @return Token 流
     */
    @SystemMessage(fromResource = "prompt/vue-project-create-system-prompt.txt")
    TokenStream generateCodeStream(@UserMessage String userMessage);
}