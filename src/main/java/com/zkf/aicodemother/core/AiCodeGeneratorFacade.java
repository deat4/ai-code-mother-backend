package com.zkf.aicodemother.core;

import cn.hutool.core.util.StrUtil;
import com.zkf.aicodemother.ai.model.HtmlCodeResult;
import com.zkf.aicodemother.ai.model.MultiFileCodeResult;
import com.zkf.aicodemother.config.AppConfig;
import com.zkf.aicodemother.core.parser.CodeParserExecutor;
import com.zkf.aicodemother.core.saver.CodeFileSaverExecutor;
import com.zkf.aicodemother.exception.BusinessException;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.model.dto.appversion.AppVersionAddRequest;
import com.zkf.aicodemother.model.enums.ChangeTypeEnum;
import com.zkf.aicodemother.config.AiCodeGeneratorServiceFactory;
import com.zkf.aicodemother.model.enums.GenerationTaskStageEnum;
import com.zkf.aicodemother.model.enums.GenerationTaskLogTypeEnum;
import com.zkf.aicodemother.service.GenerationTaskService;
import com.zkf.aicodemother.service.GenerationValidationOrchestrator;
import com.zkf.aicodemother.core.validation.ValidationContext;
import com.zkf.aicodemother.core.validation.ValidationResult;

import com.zkf.aicodemother.service.AppService;
import com.zkf.aicodemother.service.AppVersionService;
import com.zkf.aicodemother.service.impl.ScreenshotServiceImpl;
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

    @Resource
    @org.springframework.context.annotation.Lazy
    private AppService appService;

    @Resource
    private ScreenshotServiceImpl screenshotService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private GenerationTaskService generationTaskService;

    @Resource
    private GenerationValidationOrchestrator validationOrchestrator;

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
                HtmlCodeResult result = aiServiceFactory.getAiCodeGeneratorService(0L, convertToModelEnum(codeGenTypeEnum)).generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiServiceFactory.getAiCodeGeneratorService(0L, convertToModelEnum(codeGenTypeEnum)).generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE);
            }
            case VUE_PROJECT -> throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "VUE_PROJECT 类型不支持此方法，请使用 Vue 项目专用的生成流程");
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
                HtmlCodeResult result = aiServiceFactory.getAiCodeGeneratorService(appId, convertToModelEnum(codeGenTypeEnum)).generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiServiceFactory.getAiCodeGeneratorService(appId, convertToModelEnum(codeGenTypeEnum)).generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "VUE_PROJECT 类型不支持此方法，请使用 Vue 项目专用的生成流程");
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
                Flux<String> codeStream = aiServiceFactory.getAiCodeGeneratorService(0L, convertToModelEnum(codeGenTypeEnum)).generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiServiceFactory.getAiCodeGeneratorService(0L, convertToModelEnum(codeGenTypeEnum)).generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE);
            }
            case VUE_PROJECT -> throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "VUE_PROJECT 类型不支持此方法，请使用 Vue 项目专用的生成流程");
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
        return generateAndSaveCodeStream(userMessage, codeGenTypeEnum, appId, userId, null);
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式，使用 appId、userId 和 taskId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     * @param userId          用户 ID（用于创建版本）
     * @param taskId          任务 ID
     * @return 流式响应
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId, Long userId, Long taskId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiServiceFactory.getAiCodeGeneratorService(appId, convertToModelEnum(codeGenTypeEnum)).generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId, userMessage, userId, taskId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiServiceFactory.getAiCodeGeneratorService(appId, convertToModelEnum(codeGenTypeEnum)).generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId, userMessage, userId, taskId);
            }
            case VUE_PROJECT -> throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "VUE_PROJECT 类型不支持此方法，请使用 Vue 项目专用的生成流程");
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
        return processCodeStream(codeStream, codeGenType, appId, null, null, null);
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
        return processCodeStream(codeStream, codeGenType, appId, userMessage, userId, null);
    }

    /**
     * 通用流式代码处理方法（使用 appId、版本创建和任务跟踪）
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @param userMessage 用户提示词（用于版本摘要）
     * @param userId      用户 ID（用于创建版本）
     * @param taskId      任务 ID
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId, String userMessage, Long userId, Long taskId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream
                .doOnNext(codeBuilder::append)
                .concatWith(Flux.defer(() -> {
                    // 在原始流完成后执行处理和校验，并发出事件
                    try {
                        String completeCode = codeBuilder.toString();
                        log.info("流式响应完成，原始内容长度: {}", completeCode.length());

                        // 使用执行器解析代码
                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                        // 使用执行器保存代码（使用 appId）
                        File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                        log.info("代码保存成功，路径: {}", savedDir.getAbsolutePath());

                        // 创建版本记录（使用解析后的内容）
                        if (appId != null && userId != null) {
                            String versionContent = extractVersionContent(parsedResult);
                            createVersionRecord(appId, userId, versionContent, userMessage);
                        }

                        // 更新任务日志
                        if (taskId != null) {
                            generationTaskService.appendLog(taskId, GenerationTaskStageEnum.GENERATING,
                                    GenerationTaskLogTypeEnum.INFO, "代码生成完成，已保存文件");
                        }

                        // 执行校验（HTML/MULTI_FILE）
                        if (taskId != null) {
                            ValidationContext validationContext = ValidationContext.builder()
                                    .taskId(taskId)
                                    .appId(appId)
                                    .userId(userId)
                                    .codeGenType(codeGenType.getValue())
                                    .projectRootPath(savedDir.getAbsolutePath())
                                    .build();

                            ValidationResult validationResult = validationOrchestrator.validateAndUpdateTask(validationContext);

                            // 发出 validation_result 事件（taskId 转为字符串，避免前端 JS 精度丢失）
                            cn.hutool.json.JSONObject validationData = new cn.hutool.json.JSONObject();
                            validationData.set("taskId", String.valueOf(taskId));
                            validationData.set("passed", validationResult.isPassedByErrors());
                            validationData.set("summary", validationResult.getSummary());
                            validationData.set("stage", validationResult.getStage());
                            validationData.set("issueCount", validationResult.getErrorCount());
                            validationData.set("warningCount", validationResult.getWarningCount());
                            validationData.set("issues", validationResult.getIssues());
                            if (validationResult.getBuildResult() != null) {
                                validationData.set("buildResult", validationResult.getBuildResult());
                            }

                            // 校验通过才生成截图，否则直接结束
                            if (validationResult.isPassed()) {
                                generateCoverAsync(appId, codeGenType, taskId);
                            } else {
                                log.warn("校验失败，跳过截图生成: appId={}, taskId={}", appId, taskId);
                            }

                            return Flux.just("EVENT:validation_result:" + validationData.toString());
                        } else {
                            // 无 taskId 时直接生成截图
                            generateCoverAsync(appId, codeGenType, taskId);
                        }
                    } catch (Exception e) {
                        log.error("代码保存失败: {}", e.getMessage(), e);
                        if (taskId != null) {
                            generationTaskService.markFailed(taskId, "代码保存失败: " + e.getMessage());
                        }
                    }
                    return Flux.empty();
                }))
                .doOnError(error -> {
                    log.error("代码生成流失败: {}", error.getMessage());
                    if (taskId != null) {
                        generationTaskService.markFailed(taskId, "代码生成流失败: " + error.getMessage());
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
     * 从解析结果中提取版本内容
     *
     * @param parsedResult 解析结果（HtmlCodeResult 或 MultiFileCodeResult）
     * @return 版本内容字符串
     */
    private String extractVersionContent(Object parsedResult) {
        if (parsedResult instanceof com.zkf.aicodemother.ai.model.HtmlCodeResult htmlResult) {
            return htmlResult.getHtmlCode();
        } else if (parsedResult instanceof com.zkf.aicodemother.ai.model.MultiFileCodeResult multiResult) {
            // 多文件：返回 JSON 格式（包含 html、css、js）
            return cn.hutool.json.JSONUtil.toJsonStr(multiResult);
        }
        return "";
    }

    /**
     * 异步生成截图封面
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型
     */
    private void generateCoverAsync(Long appId, CodeGenTypeEnum codeGenType) {
        generateCoverAsync(appId, codeGenType, null);
    }

    /**
     * 异步生成截图封面（带任务跟踪）
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型
     * @param taskId      任务 ID
     */
    private void generateCoverAsync(Long appId, CodeGenTypeEnum codeGenType, Long taskId) {
        if (appId == null) {
            return;
        }
        // 构建预览 URL
        String previewUrl = String.format("%s/%s_%s/index.html",
                appConfig.getPreview().getHost(), codeGenType.getValue(), appId);
        log.info("开始异步生成截图封面: appId={}, url={}, taskId={}", appId, previewUrl, taskId);

        // 更新任务阶段为 SCREENSHOT
        if (taskId != null) {
            generationTaskService.updateStage(taskId, GenerationTaskStageEnum.SCREENSHOT);
        }

        // 异步执行截图
        screenshotService.generateAndUploadScreenshotAsync(previewUrl)
                .thenAccept(coverUrl -> {
                    if (StrUtil.isNotBlank(coverUrl)) {
                        // 更新应用封面
                        boolean updated = appService.updateCover(appId, coverUrl);
                        if (updated) {
                            log.info("应用封面更新成功: appId={}, coverUrl={}", appId, coverUrl);
                        } else {
                            log.warn("应用封面更新失败: appId={}", appId);
                        }
                        // 记录截图日志并标记任务成功
                        if (taskId != null) {
                            generationTaskService.appendLog(taskId, GenerationTaskStageEnum.SCREENSHOT,
                                    GenerationTaskLogTypeEnum.INFO, "截图生成完成");
                            generationTaskService.markSuccess(taskId);
                        }
                    } else {
                        log.warn("截图生成失败，无法更新封面: appId={}", appId);
                        // 截图失败不标记任务失败，只记日志
                        if (taskId != null) {
                            generationTaskService.appendLog(taskId, GenerationTaskStageEnum.SCREENSHOT,
                                    GenerationTaskLogTypeEnum.ERROR, "截图生成失败");
                            generationTaskService.markSuccess(taskId);
                        }
                    }
                })
                .exceptionally(e -> {
                    log.error("截图任务异常: appId={}", appId, e);
                    if (taskId != null) {
                        generationTaskService.appendLog(taskId, GenerationTaskStageEnum.SCREENSHOT,
                                GenerationTaskLogTypeEnum.ERROR, "截图任务异常: " + e.getMessage());
                        generationTaskService.markSuccess(taskId);
                    }
                    return null;
                });
    }

    /**
     * 将 core.CodeGenTypeEnum 转换为 model.enums.CodeGenTypeEnum
     */
    private com.zkf.aicodemother.model.enums.CodeGenTypeEnum convertToModelEnum(CodeGenTypeEnum coreEnum) {
        if (coreEnum == null) {
            return null;
        }
        return com.zkf.aicodemother.model.enums.CodeGenTypeEnum.getEnumByValue(coreEnum.getValue());
    }
}