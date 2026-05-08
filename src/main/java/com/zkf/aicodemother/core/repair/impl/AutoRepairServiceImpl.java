package com.zkf.aicodemother.core.repair.impl;

import cn.hutool.core.util.StrUtil;
import com.zkf.aicodemother.core.CodeGenTypeEnum;
import com.zkf.aicodemother.core.repair.AutoRepairService;
import com.zkf.aicodemother.core.repair.RepairContext;
import com.zkf.aicodemother.core.repair.RepairPromptBuilder;
import com.zkf.aicodemother.core.repair.RepairResult;
import com.zkf.aicodemother.core.validation.BuildValidationResult;
import com.zkf.aicodemother.core.validation.ValidationContext;
import com.zkf.aicodemother.core.validation.ValidationResult;
import com.zkf.aicodemother.service.GenerationTaskService;
import com.zkf.aicodemother.service.GenerationValidationOrchestrator;
import com.zkf.aicodemother.config.AiCodeGeneratorServiceFactory;
import com.zkf.aicodemother.ai.AiCodeModifier;
import com.zkf.aicodemother.core.AiCodeGeneratorFacade;
import com.zkf.aicodemother.utils.TokenStreamConverter;
import com.zkf.aicodemother.core.handler.JsonMessageStreamHandler;
import com.zkf.aicodemother.service.ChatHistoryService;
import com.zkf.aicodemother.model.entity.GenerationTask;
import com.zkf.aicodemother.model.entity.User;
import com.zkf.aicodemother.model.enums.GenerationTaskStatusEnum;
import com.zkf.aicodemother.constant.AppConstant;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 自动修复服务实现
 */
@Slf4j
@Service
public class AutoRepairServiceImpl implements AutoRepairService {

    @Resource
    private RepairPromptBuilder repairPromptBuilder;

    @Resource
    private AiCodeGeneratorServiceFactory aiServiceFactory;

    @Resource
    @Lazy
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    @Lazy
    private GenerationValidationOrchestrator validationOrchestrator;

    @Resource
    private GenerationTaskService generationTaskService;

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Override
    public RepairResult repair(RepairContext context) {
        log.info("开始自动修复: taskId={}, appId={}, codeGenType={}, repairRound={}",
                context.getTaskId(), context.getAppId(), context.getCodeGenType(), context.getRepairRound());

        try {
            // 1. 生成修复提示词
            String repairPrompt = repairPromptBuilder.buildRepairPrompt(context);
            log.info("修复提示词已生成: taskId={}, promptLength={}", context.getTaskId(), repairPrompt.length());

            // 2. 根据项目类型调用不同的修复链路
            String codeGenType = context.getCodeGenType();
            RepairResult result;

            if ("HTML".equalsIgnoreCase(codeGenType) || "MULTI_FILE".equalsIgnoreCase(codeGenType)) {
                result = repairHtmlOrMultiFile(context, repairPrompt);
            } else if ("VUE_PROJECT".equalsIgnoreCase(codeGenType) || "vue_project".equalsIgnoreCase(codeGenType)) {
                result = repairVueProject(context, repairPrompt);
            } else {
                result = RepairResult.builder()
                        .attempted(false)
                        .success(false)
                        .skippedReason("不支持的代码生成类型: " + codeGenType)
                        .build();
            }

            log.info("自动修复完成: taskId={}, attempted={}, success={}",
                    context.getTaskId(), result.isAttempted(), result.isSuccess());
            return result;

        } catch (Exception e) {
            log.error("自动修复异常: taskId={}, error={}", context.getTaskId(), e.getMessage(), e);
            return RepairResult.builder()
                    .attempted(true)
                    .success(false)
                    .repairRound(context.getRepairRound())
                    .errorMessage("修复过程异常: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 修复 HTML/MULTI_FILE 类型
     */
    private RepairResult repairHtmlOrMultiFile(RepairContext context, String repairPrompt) {
        Long appId = context.getAppId();
        Long userId = context.getUserId();
        Long taskId = context.getTaskId();

        try {
            // 转换 codeGenType
            CodeGenTypeEnum coreCodeGenType = CodeGenTypeEnum.getEnumByValue(context.getCodeGenType());
            if (coreCodeGenType == null) {
                coreCodeGenType = CodeGenTypeEnum.HTML;
            }

            // 调用 facade 生成并保存修复后的代码（不传 taskId，避免重复日志）
            // 注意：这里使用同步方式等待修复完成
            Flux<String> codeFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(
                    repairPrompt, coreCodeGenType, appId, userId, null);

            // 等待流完成（同步阻塞）
            StringBuilder codeBuilder = new StringBuilder();
            codeFlux.toIterable().forEach(codeBuilder::append);

            log.info("HTML/MULTI_FILE 修复代码已保存: appId={}, contentLength={}",
                    appId, codeBuilder.length());

            // 修复完成后，检查任务状态是否仍然是 RUNNING
            if (taskId != null) {
                GenerationTask task = generationTaskService.getTaskById(taskId);
                if (task == null || !GenerationTaskStatusEnum.RUNNING.getValue().equals(task.getStatus())) {
                    log.warn("任务状态已变化，跳过重新校验: taskId={}, status={}",
                            taskId, task != null ? task.getStatus() : "null");
                    return RepairResult.builder()
                            .attempted(true)
                            .success(false)
                            .repairRound(context.getRepairRound())
                            .skippedReason("任务已被取消或状态已变化")
                            .build();
                }
            }

            // 3. 修复后重新校验
            String projectPath = getProjectPath(context.getCodeGenType(), appId);
            ValidationContext validationContext = ValidationContext.builder()
                    .taskId(taskId)
                    .appId(appId)
                    .userId(userId)
                    .codeGenType(context.getCodeGenType())
                    .projectRootPath(projectPath)
                    .build();

            ValidationResult validationResult = validationOrchestrator.validateAndUpdateTask(validationContext);

            // 4. 构建修复结果
            return RepairResult.builder()
                    .attempted(true)
                    .success(validationResult.isPassedByErrors())
                    .repairRound(context.getRepairRound())
                    .summary(validationResult.isPassedByErrors()
                            ? "HTML/MULTI_FILE 自动修复成功"
                            : "自动修复完成，但校验仍失败: " + validationResult.getSummary())
                    .validationResult(validationResult)
                    .build();

        } catch (Exception e) {
            log.error("HTML/MULTI_FILE 修复异常: appId={}, error={}", appId, e.getMessage(), e);
            return RepairResult.builder()
                    .attempted(true)
                    .success(false)
                    .repairRound(context.getRepairRound())
                    .errorMessage("修复异常: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 修复 VUE_PROJECT 类型
     */
    private RepairResult repairVueProject(RepairContext context, String repairPrompt) {
        Long appId = context.getAppId();
        Long userId = context.getUserId();
        Long taskId = context.getTaskId();

        try {
            // 使用 AiCodeModifier 进行修复（MODIFICATION 场景有完整工具集）
            AiCodeModifier modifier = aiServiceFactory.getVueProjectModifierService(appId);

            // 获取 TokenStream
            TokenStream tokenStream = modifier.updateCodeStream(appId, repairPrompt);

            // 转换为 Flux<String>
            Flux<String> originFlux = TokenStreamConverter.toFlux(tokenStream);

            // 使用 JsonMessageStreamHandler 处理（但不传 taskId，避免重复校验和状态更新）
            // 创建临时 User 对象
            User tempUser = new User();
            tempUser.setId(userId);

            // 收集处理后的内容
            StringBuilder responseBuilder = new StringBuilder();
            Flux<String> processedFlux = jsonMessageStreamHandler.handle(
                    originFlux, chatHistoryService, appId, tempUser, null);

            processedFlux.toIterable().forEach(chunk -> {
                // 只收集普通文本，跳过事件标记
                if (!chunk.startsWith("EVENT:")) {
                    responseBuilder.append(chunk);
                }
            });

            log.info("VUE_PROJECT 修复代码已保存: appId={}, responseLength={}",
                    appId, responseBuilder.length());

            // 修复完成后，检查任务状态是否仍然是 RUNNING
            if (taskId != null) {
                GenerationTask task = generationTaskService.getTaskById(taskId);
                if (task == null || !GenerationTaskStatusEnum.RUNNING.getValue().equals(task.getStatus())) {
                    log.warn("任务状态已变化，跳过重新校验: taskId={}, status={}",
                            taskId, task != null ? task.getStatus() : "null");
                    return RepairResult.builder()
                            .attempted(true)
                            .success(false)
                            .repairRound(context.getRepairRound())
                            .skippedReason("任务已被取消或状态已变化")
                            .build();
                }
            }

            // 修复后重新校验（包含构建）
            String projectPath = getProjectPath(context.getCodeGenType(), appId);
            ValidationContext validationContext = ValidationContext.builder()
                    .taskId(taskId)
                    .appId(appId)
                    .userId(userId)
                    .codeGenType(context.getCodeGenType())
                    .projectRootPath(projectPath)
                    .build();

            ValidationResult validationResult = validationOrchestrator.validateAndUpdateTask(validationContext);

            // 构建修复结果
            boolean success = validationResult.isPassedByErrors();
            String summary = success
                    ? "VUE_PROJECT 自动修复成功，构建通过"
                    : "自动修复完成，但校验仍失败: " + validationResult.getSummary();

            return RepairResult.builder()
                    .attempted(true)
                    .success(success)
                    .repairRound(context.getRepairRound())
                    .summary(summary)
                    .validationResult(validationResult)
                    .buildResult(validationResult.getBuildResult())
                    .build();

        } catch (Exception e) {
            log.error("VUE_PROJECT 修复异常: appId={}, error={}", appId, e.getMessage(), e);
            return RepairResult.builder()
                    .attempted(true)
                    .success(false)
                    .repairRound(context.getRepairRound())
                    .errorMessage("修复异常: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 获取项目路径
     */
    private String getProjectPath(String codeGenType, Long appId) {
        if ("VUE_PROJECT".equalsIgnoreCase(codeGenType) || "vue_project".equalsIgnoreCase(codeGenType)) {
            return AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
        } else if ("MULTI_FILE".equalsIgnoreCase(codeGenType)) {
            return AppConstant.CODE_OUTPUT_ROOT_DIR + "/MULTI_FILE_" + appId;
        } else {
            return AppConstant.CODE_OUTPUT_ROOT_DIR + "/HTML_" + appId;
        }
    }
}