package com.zkf.aicodemother.config;

import com.zkf.aicodemother.ai.AiCodeGenTypeRoutingService;
import com.zkf.aicodemother.utils.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * AI代码生成类型路由服务工厂
 * 使用多例模式获取 ChatModel，解决并发阻塞问题
 *
 * @author <a href="https://github.com/deat4/ai-code-mother-backend">zkf</a>
 */
@Slf4j
@Configuration
public class AiCodeGenTypeRoutingServiceFactory {

    /**
     * 创建AI代码生成类型路由服务实例
     * 每次调用都获取新的 ChatModel 实例（多例模式）
     */
    public AiCodeGenTypeRoutingService createRoutingService() {
        log.debug("创建 AI 代码生成类型路由服务实例");

        // 使用多例模式获取路由 ChatModel，每次都是新实例
        ChatModel routingChatModel = SpringContextUtil.getBean(
                "routingChatModelPrototype", ChatModel.class);

        return AiServices.builder(AiCodeGenTypeRoutingService.class)
                .chatModel(routingChatModel)
                .build();
    }
}