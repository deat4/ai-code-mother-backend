package com.zkf.aicodemother.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * AI 代码修改服务接口
 * 专门用于应用修改，提供完整的读/写/改/删能力
 */
public interface AiCodeModifier {

    /**
     * 修改代码（流式传输）
     *
     * @param modificationRequest 修改请求（需求描述）
     * @return Token 流
     */
    @SystemMessage(fromResource = "prompt/vue-project-modify-system-prompt.txt")
    TokenStream updateCodeStream(@UserMessage String modificationRequest);
}