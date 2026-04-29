package com.zkf.aicodemother.service.impl;

import cn.hutool.json.JSONUtil;
import com.zkf.aicodemother.core.validation.ValidationContext;
import com.zkf.aicodemother.core.validation.ValidationIssue;
import com.zkf.aicodemother.core.validation.ValidationResult;
import com.zkf.aicodemother.core.validation.impl.HtmlValidationService;
import com.zkf.aicodemother.core.validation.impl.MultiFileValidationService;
import com.zkf.aicodemother.core.validation.impl.VueProjectValidationService;
import com.zkf.aicodemother.model.enums.GenerationTaskLogTypeEnum;
import com.zkf.aicodemother.model.enums.GenerationTaskStageEnum;
import com.zkf.aicodemother.service.GenerationTaskService;
import com.zkf.aicodemother.service.GenerationValidationOrchestrator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 生成校验编排器实现
 */
@Slf4j
@Service
public class GenerationValidationOrchestratorImpl implements GenerationValidationOrchestrator {

    @Resource
    private GenerationTaskService generationTaskService;

    @Resource
    private HtmlValidationService htmlValidationService;

    @Resource
    private MultiFileValidationService multiFileValidationService;

    @Resource
    private VueProjectValidationService vueProjectValidationService;

    @Override
    public ValidationResult validateAndUpdateTask(ValidationContext context) {
        log.info("开始校验编排: taskId={}, appId={}, codeGenType={}",
                context.getTaskId(), context.getAppId(), context.getCodeGenType());

        String codeGenType = context.getCodeGenType();
        ValidationResult result;

        if ("HTML".equalsIgnoreCase(codeGenType)) {
            result = validateHtmlOrMultiFile(context);
        } else if ("MULTI_FILE".equalsIgnoreCase(codeGenType) || "multi_file".equalsIgnoreCase(codeGenType)) {
            result = validateHtmlOrMultiFile(context);
        } else if ("VUE_PROJECT".equalsIgnoreCase(codeGenType) || "vue_project".equalsIgnoreCase(codeGenType)) {
            result = validateVueProject(context);
        } else {
            result = ValidationResult.builder()
                    .passed(false)
                    .summary("未知的代码生成类型: " + codeGenType)
                    .build();
        }

        // 根据校验结果更新任务状态
        updateTaskByValidationResult(context.getTaskId(), result);

        log.info("校验编排完成: taskId={}, passed={}, summary={}",
                context.getTaskId(), result.isPassed(), result.getSummary());

        return result;
    }

    @Override
    public ValidationResult validateHtmlOrMultiFile(ValidationContext context) {
        log.info("校验 HTML/MULTI_FILE: taskId={}", context.getTaskId());

        // 更新任务阶段为 VALIDATING
        generationTaskService.updateStage(context.getTaskId(), GenerationTaskStageEnum.VALIDATING);
        generationTaskService.appendLog(context.getTaskId(), GenerationTaskStageEnum.VALIDATING,
                GenerationTaskLogTypeEnum.VALIDATION, "开始代码校验");

        // 执行校验
        ValidationResult result;
        String codeGenType = context.getCodeGenType();

        if ("HTML".equalsIgnoreCase(codeGenType)) {
            result = htmlValidationService.validateHtml(context);
        } else {
            result = multiFileValidationService.validateMultiFile(context);
        }

        // 记录校验结果日志
        generationTaskService.appendLog(context.getTaskId(), GenerationTaskStageEnum.VALIDATING,
                GenerationTaskLogTypeEnum.VALIDATION,
                "校验完成: " + (result.isPassed() ? "通过" : "失败") + ", 问题数: " + result.getIssues().size());

        // 如果有 issues，记录详细信息
        if (!result.getIssues().isEmpty()) {
            String issuesJson = JSONUtil.toJsonStr(result.getIssues());
            generationTaskService.appendLog(context.getTaskId(), GenerationTaskStageEnum.VALIDATING,
                    GenerationTaskLogTypeEnum.VALIDATION, "校验问题列表: " + issuesJson);
        }

        return result;
    }

    @Override
    public ValidationResult validateVueProject(ValidationContext context) {
        log.info("校验 VUE_PROJECT: taskId={}", context.getTaskId());

        // 第一阶段：结构校验
        generationTaskService.updateStage(context.getTaskId(), GenerationTaskStageEnum.VALIDATING);
        generationTaskService.appendLog(context.getTaskId(), GenerationTaskStageEnum.VALIDATING,
                GenerationTaskLogTypeEnum.VALIDATION, "开始 Vue 项目结构校验");

        // 执行完整校验（结构 + 构建）
        ValidationResult result = vueProjectValidationService.validateFull(context);

        // 记录结构校验结果
        generationTaskService.appendLog(context.getTaskId(), GenerationTaskStageEnum.VALIDATING,
                GenerationTaskLogTypeEnum.VALIDATION,
                "结构校验: " + (result.getIssues().stream().noneMatch(i -> "error".equals(i.getSeverity())) ? "通过" : "失败"));

        // 如果有构建结果，记录构建日志
        if (result.getBuildResult() != null) {
            generationTaskService.appendLog(context.getTaskId(), GenerationTaskStageEnum.BUILDING,
                    GenerationTaskLogTypeEnum.BUILD,
                    "npm install: " + (result.getBuildResult().isInstallSuccess() ? "成功" : "失败") +
                            ", 耗时: " + result.getBuildResult().getInstallDurationMs() + "ms");

            generationTaskService.appendLog(context.getTaskId(), GenerationTaskStageEnum.BUILDING,
                    GenerationTaskLogTypeEnum.BUILD,
                    "npm run build: " + (result.getBuildResult().isBuildSuccess() ? "成功" : "失败") +
                            ", 耗时: " + result.getBuildResult().getBuildDurationMs() + "ms");

            // 如果构建失败，记录关键错误
            if (!result.getBuildResult().isOverallSuccess() && !result.getBuildResult().getKeyErrors().isEmpty()) {
                for (String keyError : result.getBuildResult().getKeyErrors()) {
                    generationTaskService.appendLog(context.getTaskId(), GenerationTaskStageEnum.BUILDING,
                            GenerationTaskLogTypeEnum.BUILD, "构建错误: " + keyError);
                }
            }
        }

        // 记录最终结果
        generationTaskService.appendLog(context.getTaskId(), GenerationTaskStageEnum.valueOf(result.getStage()),
                GenerationTaskLogTypeEnum.VALIDATION,
                "校验完成: " + (result.isPassed() ? "通过" : "失败") + ", summary: " + result.getSummary());

        return result;
    }

    /**
     * 根据校验结果更新任务状态
     * 规则：passed 只看 ERROR，issueCount 统计 ERROR，warningCount 统计 WARN
     */
    private void updateTaskByValidationResult(Long taskId, ValidationResult result) {
        int issueCount = result.getErrorCount();    // 只统计 ERROR
        int warningCount = result.getWarningCount(); // 只统计 WARN

        if (result.isPassedByErrors()) {
            generationTaskService.updateValidationSummary(taskId, result.getSummary(), true, issueCount, warningCount);
            generationTaskService.appendLog(taskId, GenerationTaskStageEnum.DONE,
                    GenerationTaskLogTypeEnum.STAGE_CHANGE,
                    String.format("校验通过（ERROR=%d, WARN=%d）", issueCount, warningCount));
        } else {
            generationTaskService.updateValidationSummary(taskId, result.getSummary(), false, issueCount, warningCount);
            generationTaskService.markFailed(taskId, result.getSummary());
        }
    }
}