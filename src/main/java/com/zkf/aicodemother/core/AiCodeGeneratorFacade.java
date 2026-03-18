package com.zkf.aicodemother.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zkf.aicodemother.ai.model.HtmlCodeResult;
import com.zkf.aicodemother.ai.model.MultiFileCodeResult;
import com.zkf.aicodemother.ai.model.message.AiResponseMessage;
import com.zkf.aicodemother.ai.model.message.ToolExecutedMessage;
import com.zkf.aicodemother.ai.model.message.ToolRequestMessage;
import com.zkf.aicodemother.core.parser.CodeParserExecutor;
import com.zkf.aicodemother.core.saver.CodeFileSaverExecutor;
import com.zkf.aicodemother.exception.BusinessException;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.model.dto.appversion.AppVersionAddRequest;
import com.zkf.aicodemother.model.enums.ChangeTypeEnum;
import com.zkf.aicodemother.config.AiCodeGeneratorServiceFactory;
import com.zkf.aicodemother.model.enums.CodeGenTypeEnum;
import com.zkf.aicodemother.service.AppVersionService;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiServiceFactory;

    @Resource
    private AppVersionService appVersionService;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiServiceFactory.getAiCodeGeneratorService(0L).generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiServiceFactory.getAiCodeGeneratorService(0L).generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE);
            }
            case VUE_PROJECT -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 工程模式不支持同步生成，请使用流式接口");
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiServiceFactory.getAiCodeGeneratorService(appId).generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiServiceFactory.getAiCodeGeneratorService(appId).generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 工程模式不支持同步生成，请使用流式接口");
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 流式响应
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiServiceFactory.getAiCodeGeneratorService(0L).generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiServiceFactory.getAiCodeGeneratorService(0L).generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE);
            }
            case VUE_PROJECT -> {
                // Vue 项目使用 TokenStream，需要转换为 Flux
                TokenStream tokenStream = aiServiceFactory.getAiCodeGeneratorService(0L, CodeGenTypeEnum.VUE_PROJECT)
                        .generateVueProjectCodeStream(0L, userMessage);
                yield processTokenStream(tokenStream, 0L, userMessage, null);
            }
        };
    }


    /**
     * 统一入口：根据类型生成并保存代码（流式，使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     * @return 流式响应
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        return generateAndSaveCodeStream(userMessage, codeGenTypeEnum, appId, null);
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式，使用 appId 和 userId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     * @param userId          用户 ID（用于创建版本）
     * @return 流式响应
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId, Long userId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiServiceFactory.getAiCodeGeneratorService(appId).generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId, userMessage, userId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiServiceFactory.getAiCodeGeneratorService(appId).generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId, userMessage, userId);
            }
            case VUE_PROJECT -> {
                // Vue 项目使用 TokenStream，需要转换为 Flux
                TokenStream tokenStream = aiServiceFactory.getAiCodeGeneratorService(appId, CodeGenTypeEnum.VUE_PROJECT)
                        .generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream, appId, userMessage, userId);
            }
        };
    }

    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream
                .doOnNext(codeBuilder::append)
                .doOnComplete(() -> {
                    // 流式返回完成后保存代码
                    try {
                        String completeCode = codeBuilder.toString();
                        // 使用执行器解析代码
                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                        // 使用执行器保存代码
                        File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType);
                        log.info("代码保存成功，路径: {}", savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("代码保存失败: {}", e.getMessage(), e);
                    }
                });
    }

    /**
     * 通用流式代码处理方法（使用 appId）
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        return processCodeStream(codeStream, codeGenType, appId, null, null);
    }

    /**
     * 通用流式代码处理方法（使用 appId 和版本创建）
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @param userMessage 用户提示词（用于版本摘要）
     * @param userId      用户 ID（用于创建版本）
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId, String userMessage, Long userId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream
                .doOnNext(codeBuilder::append)
                .doOnComplete(() -> {
                    // 流式返回完成后保存代码
                    try {
                        String completeCode = codeBuilder.toString();
                        // DEBUG: 打印原始内容统计信息
                        System.out.println("[DEBUG] processCodeStream - 原始内容长度: " + completeCode.length());
                        System.out.println("[DEBUG] processCodeStream - 包含 ```html: " + completeCode.contains("```html"));
                        System.out.println("[DEBUG] processCodeStream - 包含 ```css: " + completeCode.contains("```css"));
                        System.out.println("[DEBUG] processCodeStream - 包含 ```js: " + (completeCode.contains("```js") || completeCode.contains("```javascript")));
                        // 使用执行器解析代码
                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                        // 使用执行器保存代码（使用 appId）
                        File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                        log.info("代码保存成功，路径: {}", savedDir.getAbsolutePath());

                        // 创建版本记录
                        if (appId != null && userId != null) {
                            // 从解析结果中提取真正的代码内容保存
                            String finalCodeToSave = extractCodeFromParsedResult(parsedResult, completeCode);
                            createVersionRecord(appId, userId, finalCodeToSave, userMessage);
                        }
                    } catch (Exception e) {
                        log.error("代码保存失败: {}", e.getMessage(), e);
                    }
                });
    }

    /**
     * 创建版本记录
     *
     * @param appId       应用 ID
     * @param userId      用户 ID
     * @param content     代码内容
     * @param userMessage 用户提示词
     */
    private void createVersionRecord(Long appId, Long userId, String content, String userMessage) {
        try {
            AppVersionAddRequest versionRequest = new AppVersionAddRequest();
            versionRequest.setAppId(appId);
            versionRequest.setContent(content);
            versionRequest.setChangeType(ChangeTypeEnum.UPDATE.getValue());
            // 截取用户提示词前 50 个字符作为摘要
            String summary = StrUtil.isNotBlank(userMessage) && userMessage.length() > 50
                    ? "AI 生成：" + userMessage.substring(0, 50) + "..."
                    : "AI 生成：" + (userMessage != null ? userMessage : "");
            versionRequest.setSummary(summary);

            appVersionService.createVersion(versionRequest, userId);
            log.info("版本记录创建成功: appId={}, userId={}", appId, userId);
        } catch (Exception e) {
            log.error("版本记录创建失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从解析结果中提取代码内容
     *
     * @param parsedResult 解析结果对象
     * @param originalCode 原始代码（作为兜底）
     * @return 提取的代码内容
     */
    private String extractCodeFromParsedResult(Object parsedResult, String originalCode) {
        if (parsedResult instanceof HtmlCodeResult htmlResult) {
            return htmlResult.getHtmlCode();
        } else if (parsedResult instanceof MultiFileCodeResult multiFileResult) {
            // 多文件场景：返回 JSON 字符串表示
            return multiFileResult.toString();
        }
        // 兜底：返回原始代码
        return originalCode;
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     * 用于 Vue 项目生成模式，支持工具调用的实时流式输出
     *
     * @param tokenStream TokenStream 对象
     * @param appId       应用 ID
     * @param userMessage 用户消息
     * @param userId      用户 ID
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId, String userMessage, Long userId) {
        StringBuilder fullContent = new StringBuilder();

        return Flux.create(sink -> {
            tokenStream
                    // AI 响应部分 - 每个 token 触发
                    .onPartialResponse(partialResponse -> {
                        fullContent.append(partialResponse);
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    // 工具执行完成 - 返回执行结果
                    .onToolExecuted(toolExecution -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                        // 追加工具执行结果到完整内容
                        fullContent.append(formatToolExecution(toolExecution));
                    })
                    // 响应完成
                    .onCompleteResponse(response -> {
                        // 创建版本记录
                        if (appId != null && userId != null && fullContent.length() > 0) {
                            createVersionRecord(appId, userId, fullContent.toString(), userMessage);
                        }
                        sink.complete();
                    })
                    // 错误处理 - 发送错误消息而不是抛出异常
                    .onError(error -> {
                        log.error("TokenStream 处理错误: {}", error.getMessage(), error);
                        // 构造错误消息并发送，而不是抛出异常
                        String errorMsg = error.getMessage();
                        // 提取关键错误信息
                        if (errorMsg != null && errorMsg.contains("Insufficient Balance")) {
                            errorMsg = "API 余额不足，请充值后重试";
                        } else if (errorMsg != null && errorMsg.contains("JsonParseException")) {
                            errorMsg = "AI 返回了格式错误的数据，请重试";
                        } else if (errorMsg != null && errorMsg.length() > 100) {
                            errorMsg = errorMsg.substring(0, 100);
                        }
                        // 如果已生成内容，说明部分成功
                        if (fullContent.length() > 100) {
                            AiResponseMessage warnMessage = new AiResponseMessage(
                                    "\n\n⚠️ 生成过程中出现错误: " + errorMsg + "（已生成部分内容）\n\n");
                            sink.next(JSONUtil.toJsonStr(warnMessage));
                            // 仍然创建版本记录
                            if (appId != null && userId != null) {
                                createVersionRecord(appId, userId, fullContent.toString(), userMessage);
                            }
                        } else {
                            AiResponseMessage errorMessage = new AiResponseMessage(
                                    "\n\n❌ 生成失败: " + errorMsg + "\n\n");
                            sink.next(JSONUtil.toJsonStr(errorMessage));
                        }
                        sink.complete(); // 完成流，而不是传播错误
                    })
                    .start();
        });
    }

    /**
     * 格式化工具执行结果，用于保存到对话记忆
     *
     * @param toolExecution 工具执行对象
     * @return 格式化后的字符串
     */
    private String formatToolExecution(ToolExecution toolExecution) {
        return String.format("\n[工具调用] %s\n结果: %s\n",
                toolExecution.request().name(),
                toolExecution.result());
    }
}