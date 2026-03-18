package com.zkf.aicodemother.core.handler;

import com.zkf.aicodemother.model.entity.User;
import com.zkf.aicodemother.model.enums.CodeGenTypeEnum;
import com.zkf.aicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 流处理器执行器
 * 根据代码生成类型创建合适的流处理器：
 * 1. 传统的 Flux&lt;String&gt; 流（HTML、MULTI_FILE） -&gt; SimpleTextStreamHandler
 * 2. TokenStream 格式的复杂流（VUE_PROJECT） -&gt; JsonMessageStreamHandler
 *
 * @author zkf
 */
@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    /**
     * 创建流处理器并处理聊天历史记录
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @param codeGenType        代码生成类型
     * @return 处理后的流
     */
    public Flux<String> doExecute(Flux<String> originFlux,
                                  ChatHistoryService chatHistoryService,
                                  long appId, User loginUser, CodeGenTypeEnum codeGenType) {
        log.debug("执行流处理器，appId: {}, codeGenType: {}", appId, codeGenType);
        
        return switch (codeGenType) {
            case VUE_PROJECT -> 
                    // 使用注入的组件实例处理复杂的 JSON 消息流
                    jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
            case HTML, MULTI_FILE -> 
                    // 简单文本处理器不需要依赖注入，直接创建实例
                    new SimpleTextStreamHandler().handle(originFlux, chatHistoryService, appId, loginUser);
        };
    }
}