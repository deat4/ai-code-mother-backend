package com.zkf.aicodemother.service;

import com.zkf.aicodemother.core.repair.RepairResult;
import com.zkf.aicodemother.core.validation.ValidationContext;
import com.zkf.aicodemother.core.validation.ValidationResult;

/**
 * 自动修复编排服务接口
 * 负责协调修复流程的完整生命周期
 */
public interface GenerationRepairOrchestrator {

    /**
     * 执行自动修复编排
     * 包括判断是否需要修复、调用修复、修复后重新校验
     *
     * @param validationContext 校验上下文
     * @param validationResult  原始校验结果（已失败）
     * @return 修复结果
     */
    RepairResult orchestrateRepair(ValidationContext validationContext, ValidationResult validationResult);

    /**
     * 判断是否应该执行自动修复
     *
     * @param taskId            任务 ID
     * @param validationResult  校验结果
     * @return 是否需要修复
     */
    boolean shouldAutoRepair(Long taskId, ValidationResult validationResult);
}