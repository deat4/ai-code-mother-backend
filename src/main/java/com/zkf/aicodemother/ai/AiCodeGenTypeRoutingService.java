package com.zkf.aicodemother.ai;

import com.zkf.aicodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI代码生成类型路由服务
 * 根据用户需求智能选择代码生成类型
 */
public interface AiCodeGenTypeRoutingService {

    /**
     * 根据用户需求路由代码生成类型
     *
     * @param userPrompt 用户需求描述
     * @return 推荐的代码生成类型
     */
    @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
    CodeGenTypeEnum routeCodeGenType(@UserMessage String userPrompt);
}