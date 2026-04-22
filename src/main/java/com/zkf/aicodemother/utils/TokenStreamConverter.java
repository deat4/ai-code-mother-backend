package com.zkf.aicodemother.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * TokenStream 到 Flux 的转换工具
 * LangChain4j 的 TokenStream 不直接实现 Publisher 接口，
 * 需要通过回调方式手动转换
 *
 * 将 TokenStream 的多种事件（AI文本、工具调用请求、工具执行结果）转换为统一的 JSON 格式
 *
 * @author zkf
 */
public class TokenStreamConverter {

    // 使用小写值，与 StreamMessageTypeEnum 保持一致
    private static final String TYPE_AI_RESPONSE = "ai_response";
    private static final String TYPE_TOOL_REQUEST = "tool_request";
    private static final String TYPE_TOOL_EXECUTED = "tool_executed";

    /**
     * 将 LangChain4j TokenStream 转换为 Reactor Flux<String>
     * 输出的每条消息都是 JSON 格式，包含 type 字段
     *
     * @param tokenStream LangChain4j TokenStream
     * @return Reactor Flux<String>（每条都是JSON格式）
     */
    public static Flux<String> toFlux(TokenStream tokenStream) {
        return Flux.create(emitter -> {
            // 设置 onPartialResponse 回调，将每个 AI 文本片段包装为 JSON
            tokenStream.onPartialResponse(text -> {
                if (text != null && !text.isEmpty()) {
                    JSONObject message = new JSONObject();
                    message.set("type", TYPE_AI_RESPONSE);
                    message.set("data", text);
                    emitter.next(message.toString());
                }
            });

            // 设置 onToolExecuted 回调，将工具执行事件包装为 JSON
            tokenStream.onToolExecuted(toolExecution -> {
                ToolExecutionRequest request = toolExecution.request();
                String result = toolExecution.result();

                // 工具调用请求消息
                JSONObject requestMessage = new JSONObject();
                requestMessage.set("type", TYPE_TOOL_REQUEST);
                requestMessage.set("id", request.id());
                requestMessage.set("name", request.name());
                emitter.next(requestMessage.toString());

                // 工具执行结果消息
                JSONObject resultMessage = new JSONObject();
                resultMessage.set("type", TYPE_TOOL_EXECUTED);
                resultMessage.set("name", request.name());
                resultMessage.set("id", request.id());
                resultMessage.set("arguments", request.arguments());
                resultMessage.set("result", result);
                emitter.next(resultMessage.toString());
            });

            // 设置 onCompleteResponse 回调，完成 Flux
            tokenStream.onCompleteResponse((ChatResponse response) -> emitter.complete());

            // 设置 onError 回调，将错误发送到 Flux
            tokenStream.onError(emitter::error);

            // 启动流式传输
            tokenStream.start();

        }, FluxSink.OverflowStrategy.BUFFER);
    }
}