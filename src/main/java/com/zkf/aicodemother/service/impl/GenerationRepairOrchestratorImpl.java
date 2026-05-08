package com.zkf.aicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.zkf.aicodemother.core.repair.AutoRepairService;
import com.zkf.aicodemother.core.repair.RepairContext;
import com.zkf.aicodemother.core.repair.RepairResult;
import com.zkf.aicodemother.core.validation.ValidationContext;
import com.zkf.aicodemother.core.validation.ValidationResult;
import com.zkf.aicodemother.model.entity.GenerationTask;
import com.zkf.aicodemother.model.enums.GenerationTaskLogTypeEnum;
import com.zkf.aicodemother.model.enums.GenerationTaskStageEnum;
import com.zkf.aicodemother.model.enums.GenerationTaskStatusEnum;
import com.zkf.aicodemother.service.GenerationRepairOrchestrator;
import com.zkf.aicodemother.service.GenerationTaskService;
import com.zkf.aicodemother.service.GenerationValidationOrchestrator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 自动修复编排服务实现
 * 负责协调修复流程的完整生命周期
 */
@Slf4j
@Service
public class GenerationRepairOrchestratorImpl implements GenerationRepairOrchestrator {

    @Resource
    private GenerationTaskService taskService;

    @Resource
    @Lazy
    private AutoRepairService autoRepairService;

    @Resource
    @Lazy
    private GenerationValidationOrchestrator validationOrchestrator;

    @Override
    public RepairResult orchestrateRepair(ValidationContext validationContext, ValidationResult validationResult) {
        Long taskId = validationContext.getTaskId();
        Long appId = validationContext.getAppId();

        log.info("开始修复编排: taskId={}, appId={}, errorCount={}",
                taskId, appId, validationResult.getErrorCount());

        // 1. 判断是否应该执行修复
        if (!shouldAutoRepair(taskId, validationResult)) {
            log.info("不满足修复条件，跳过修复: taskId={}", taskId);
            return RepairResult.builder()
                    .attempted(false)
                    .success(false)
                    .skippedReason("不满足自动修复条件")
                    .build();
        }

        // 2. 获取任务信息
        GenerationTask task = taskService.getTaskById(taskId);
        if (task == null) {
            log.error("任务不存在: taskId={}", taskId);
            return RepairResult.builder()
                    .attempted(false)
                    .success(false)
                    .skippedReason("任务不存在")
                    .build();
        }

        // 检查任务状态（只有 RUNNING 状态才能修复）
        if (!GenerationTaskStatusEnum.RUNNING.getValue().equals(task.getStatus())) {
            log.warn("任务状态不是 RUNNING，跳过修复: taskId={}, status={}", taskId, task.getStatus());
            return RepairResult.builder()
                    .attempted(false)
                    .success(false)
                    .skippedReason("任务状态不是 RUNNING: " + task.getStatus())
                    .build();
        }

        // 3. 更新任务阶段到 REPAIRING
        String previousStage = task.getCurrentStage();
        taskService.updateStage(taskId, GenerationTaskStageEnum.REPAIRING);

        // 记录修复日志
        String repairLogContent = String.format("开始自动修复，原因: %s", validationResult.getSummary());
        taskService.appendLog(taskId, GenerationTaskStageEnum.REPAIRING, GenerationTaskLogTypeEnum.REPAIR, repairLogContent);

        // 4. 构建修复上下文
        int currentRepairRound = task.getRepairCount() != null ? task.getRepairCount() : 0;
        int maxRepairRounds = task.getMaxRepairCount() != null ? task.getMaxRepairCount() : 1;

        RepairContext repairContext = RepairContext.builder()
                .taskId(taskId)
                .appId(appId)
                .userId(validationContext.getUserId())
                .codeGenType(validationContext.getCodeGenType())
                .scene(validationContext.getScene())
                .sessionId(validationContext.getSessionId())
                .repairRound(currentRepairRound + 1)
                .maxRepairRounds(maxRepairRounds)
                .validationResult(validationResult)
                .originalMessage(validationContext.getLatestMessage())
                .projectRootPath(validationContext.getProjectRootPath())
                .currentStageBeforeRepair(previousStage)
                .build();

        // 5. 执行修复
        RepairResult repairResult = autoRepairService.repair(repairContext);

        // 6. 更新修复信息
        int newRepairCount = taskService.incrementRepairCount(taskId);
        String repairSummary = repairResult.getSummary();
        if (StrUtil.isBlank(repairSummary)) {
            repairSummary = repairResult.isSuccess()
                    ? "自动修复成功"
                    : "自动修复失败";
        }
        taskService.updateRepairInfo(taskId, newRepairCount, maxRepairRounds, repairSummary);

        // 7. 记录修复结果日志
        String resultLogContent = String.format("修复完成，第 %d 轮，结果: %s，摘要: %s",
                repairResult.getRepairRound(),
                repairResult.isSuccess() ? "成功" : "失败",
                repairSummary);
        taskService.appendLog(taskId, GenerationTaskStageEnum.REPAIRING, GenerationTaskLogTypeEnum.REPAIR, resultLogContent);

        // 8. 根据修复结果决定后续操作
        // 先检查任务是否已被取消
        GenerationTask taskAfterRepair = taskService.getTaskById(taskId);
        if (taskAfterRepair == null || !GenerationTaskStatusEnum.RUNNING.getValue().equals(taskAfterRepair.getStatus())) {
            log.warn("任务状态已变化，跳过后续处理: taskId={}, status={}",
                    taskId, taskAfterRepair != null ? taskAfterRepair.getStatus() : "null");
            return repairResult;
        }

        if (repairResult.isSuccess()) {
            // 修复成功，标记任务成功
            taskService.markSuccess(taskId);
            log.info("修复成功，任务标记为 SUCCESS: taskId={}", taskId);
        } else {
            // 修复失败，标记任务失败
            String failedMessage = String.format("自动修复失败，第 %d 轮，原因: %s",
                    repairResult.getRepairRound(),
                    repairResult.getSummary());
            taskService.markFailed(taskId, failedMessage);
            log.info("修复失败，任务标记为 FAILED: taskId={}, reason={}", taskId, failedMessage);
        }

        log.info("修复编排完成: taskId={}, attempted={}, success={}, repairRound={}",
                taskId, repairResult.isAttempted(), repairResult.isSuccess(), repairResult.getRepairRound());

        return repairResult;
    }

    @Override
    public boolean shouldAutoRepair(Long taskId, ValidationResult validationResult) {
        // 1. 校验结果必须存在
        if (validationResult == null) {
            log.debug("校验结果不存在，不执行修复: taskId={}", taskId);
            return false;
        }

        // 2. 必须有 ERROR（通过 isPassedByErrors 判断）
        if (validationResult.isPassedByErrors()) {
            log.debug("校验已通过（无 ERROR），不执行修复: taskId={}", taskId);
            return false;
        }

        // 3. ERROR 数量必须大于 0
        if (validationResult.getErrorCount() <= 0) {
            log.debug("ERROR 数量为 0，不执行修复: taskId={}", taskId);
            return false;
        }

        // 4. 检查任务状态
        GenerationTask task = taskService.getTaskById(taskId);
        if (task == null) {
            log.debug("任务不存在，不执行修复: taskId={}", taskId);
            return false;
        }

        // 5. 任务必须是 RUNNING 状态
        if (!GenerationTaskStatusEnum.RUNNING.getValue().equals(task.getStatus())) {
            log.debug("任务状态不是 RUNNING，不执行修复: taskId={}, status={}", taskId, task.getStatus());
            return false;
        }

        // 6. 修复轮次不能超过最大轮次
        int repairCount = task.getRepairCount() != null ? task.getRepairCount() : 0;
        int maxRepairCount = task.getMaxRepairCount() != null ? task.getMaxRepairCount() : 1;

        if (repairCount >= maxRepairCount) {
            log.debug("修复轮次已达上限，不执行修复: taskId={}, repairCount={}, maxRepairCount={}",
                    taskId, repairCount, maxRepairCount);
            return false;
        }

        log.info("满足自动修复条件: taskId={}, errorCount={}, repairCount={}, maxRepairCount={}",
                taskId, validationResult.getErrorCount(), repairCount, maxRepairCount);

        return true;
    }
}