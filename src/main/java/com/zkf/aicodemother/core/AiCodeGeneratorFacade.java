package com.zkf.aicodemother.core;

import cn.hutool.core.util.StrUtil;
import com.zkf.aicodemother.ai.model.HtmlCodeResult;
import com.zkf.aicodemother.ai.model.MultiFileCodeResult;
import com.zkf.aicodemother.core.parser.CodeParserExecutor;
import com.zkf.aicodemother.core.saver.CodeFileSaverExecutor;
import com.zkf.aicodemother.exception.BusinessException;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.model.dto.appversion.AppVersionAddRequest;
import com.zkf.aicodemother.model.enums.ChangeTypeEnum;
import com.zkf.aicodemother.config.AiCodeGeneratorServiceFactory;

import com.zkf.aicodemother.service.AppVersionService;
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
                        // 使用执行器解析代码
                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                        // 使用执行器保存代码（使用 appId）
                        File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                        log.info("代码保存成功，路径: {}", savedDir.getAbsolutePath());
                        
                        // 创建版本记录
                        if (appId != null && userId != null) {
                            createVersionRecord(appId, userId, completeCode, userMessage);
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
}