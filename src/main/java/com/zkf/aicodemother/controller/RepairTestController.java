package com.zkf.aicodemother.controller;

import com.zkf.aicodemother.common.BaseResponse;
import com.zkf.aicodemother.common.ResultUtils;
import com.zkf.aicodemother.core.repair.AutoRepairService;
import com.zkf.aicodemother.core.repair.RepairContext;
import com.zkf.aicodemother.core.repair.RepairResult;
import com.zkf.aicodemother.core.validation.ValidationContext;
import com.zkf.aicodemother.core.validation.ValidationIssue;
import com.zkf.aicodemother.core.validation.ValidationResult;
import com.zkf.aicodemother.model.entity.GenerationTask;
import com.zkf.aicodemother.service.GenerationRepairOrchestrator;
import com.zkf.aicodemother.service.GenerationTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

/**
 * 修复流程测试控制器
 *
 * 安全说明：
 * - 仅在 dev/test profile 启用
 * - 生产环境（location/prod 等）不会加载此 Controller
 * - 用于触发修复流程的测试接口，不应在生产环境暴露
 */
@Slf4j
@RestController
@RequestMapping("/test/repair")
@Profile({"dev", "test"})
public class RepairTestController {

    @Resource
    private GenerationTaskService taskService;

    @Resource
    @Lazy
    private GenerationRepairOrchestrator repairOrchestrator;

    @Resource
    @Lazy
    private AutoRepairService autoRepairService;

    /**
     * 测试触发修复流程
     * 创建一个模拟的验证失败场景，触发修复（直接调用AutoRepairService）
     *
     * @param appId  应用ID
     * @return 修复结果
     */
    @GetMapping("/trigger")
    public BaseResponse<RepairResult> triggerRepair(@RequestParam Long appId) {
        return triggerRepairWithType(appId, "HTML");
    }

    /**
     * 测试触发修复流程（指定类型）
     *
     * @param appId      应用ID
     * @param codeGenType 代码生成类型（HTML, MULTI_FILE, VUE_PROJECT）
     * @return 修复结果
     */
    @GetMapping("/trigger/type")
    public BaseResponse<RepairResult> triggerRepairWithType(
            @RequestParam Long appId,
            @RequestParam String codeGenType) {

        log.info("测试触发修复流程: appId={}, codeGenType={}", appId, codeGenType);

        // 创建一个临时任务用于测试修复流程
        Long testTaskId = taskService.createTask(appId, 386412663475752960L, codeGenType, "MODIFICATION");
        taskService.startTask(testTaskId, "test-repair-session-" + testTaskId);
        taskService.initRepairQuota(testTaskId, 1);

        log.info("创建测试任务: taskId={}", testTaskId);

        // 根据类型构建不同的验证失败结果
        ValidationResult mockValidationResult = buildMockValidationResult(codeGenType);

        // 构建项目路径
        String projectPath = buildProjectPath(codeGenType, appId);

        // 创建修复上下文
        RepairContext repairContext = RepairContext.builder()
                .taskId(testTaskId)
                .appId(appId)
                .userId(386412663475752960L)
                .codeGenType(codeGenType)
                .scene("MODIFICATION")
                .repairRound(1)
                .maxRepairRounds(1)
                .validationResult(mockValidationResult)
                .projectRootPath(projectPath)
                .build();

        // 直接调用修复服务
        RepairResult repairResult = autoRepairService.repair(repairContext);

        return ResultUtils.success(repairResult);
    }

    /**
     * 根据类型构建模拟的验证失败结果
     */
    private ValidationResult buildMockValidationResult(String codeGenType) {
        ValidationResult result = ValidationResult.builder()
                .codeGenType(codeGenType)
                .stage("VALIDATING")
                .passed(false)
                .issues(new ArrayList<>())
                .build();

        if ("HTML".equalsIgnoreCase(codeGenType)) {
            result.setSummary("模拟验证失败：缺少index.html");
            result.addIssue(ValidationIssue.error("structure", "INDEX_MISSING",
                    "入口文件 index.html 不存在（测试注入）"));
        } else if ("MULTI_FILE".equalsIgnoreCase(codeGenType) || "multi_file".equalsIgnoreCase(codeGenType)) {
            result.setSummary("模拟验证失败：缺少script.js");
            result.addIssue(ValidationIssue.error("structure", "FILES_COUNT_INSUFFICIENT",
                    "有效文件数量不足，缺少 script.js（测试注入）"));
        } else if ("VUE_PROJECT".equalsIgnoreCase(codeGenType) || "vue_project".equalsIgnoreCase(codeGenType)) {
            result.setSummary("模拟验证失败：构建失败");
            result.addIssue(ValidationIssue.error("build", "BUILD_FAILED",
                    "npm run build 执行失败（测试注入）"));
        } else {
            result.setSummary("模拟验证失败：未知类型");
            result.addIssue(ValidationIssue.error("structure", "UNKNOWN_TYPE",
                    "未知的代码生成类型"));
        }

        return result;
    }

    /**
     * 根据类型构建项目路径
     */
    private String buildProjectPath(String codeGenType, Long appId) {
        String baseDir = System.getProperty("user.dir") + "/tmp/code_output";
        if ("HTML".equalsIgnoreCase(codeGenType)) {
            return baseDir + "/HTML_" + appId;
        } else if ("MULTI_FILE".equalsIgnoreCase(codeGenType) || "multi_file".equalsIgnoreCase(codeGenType)) {
            return baseDir + "/MULTI_FILE_" + appId;
        } else if ("VUE_PROJECT".equalsIgnoreCase(codeGenType) || "vue_project".equalsIgnoreCase(codeGenType)) {
            return baseDir + "/vue_project_" + appId;
        }
        return baseDir + "/HTML_" + appId;
    }

    /**
     * 测试完整修复编排流程（需要RUNNING状态的任务）
     *
     * @param taskId 任务ID
     * @param appId  应用ID
     * @return 修复结果
     */
    @GetMapping("/orchestrate")
    public BaseResponse<RepairResult> orchestrateRepair(
            @RequestParam Long taskId,
            @RequestParam Long appId) {

        log.info("测试完整修复编排: taskId={}, appId={}", taskId, appId);

        // 检查任务状态
        GenerationTask task = taskService.getTaskById(taskId);
        if (task == null) {
            RepairResult notFoundResult = RepairResult.builder()
                    .attempted(false)
                    .success(false)
                    .skippedReason("任务不存在")
                    .build();
            return ResultUtils.success(notFoundResult);
        }

        // 创建模拟的验证失败结果
        ValidationResult mockValidationResult = ValidationResult.builder()
                .codeGenType("HTML")
                .stage("VALIDATING")
                .passed(false)
                .summary("模拟验证失败：缺少index.html")
                .issues(new ArrayList<>())
                .build();
        mockValidationResult.addIssue(ValidationIssue.error("structure", "INDEX_MISSING",
                "入口文件 index.html 不存在（测试注入）"));

        // 创建验证上下文
        ValidationContext validationContext = ValidationContext.builder()
                .taskId(taskId)
                .appId(appId)
                .userId(386412663475752960L)
                .codeGenType("HTML")
                .projectRootPath(System.getProperty("user.dir") + "/tmp/code_output/HTML_" + appId)
                .build();

        // 触发修复编排
        RepairResult repairResult = repairOrchestrator.orchestrateRepair(validationContext, mockValidationResult);

        return ResultUtils.success(repairResult);
    }
}